package net.praqma.hudson.scm;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.remoting.Future;
import hudson.remoting.Pipe;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.DeliverException;
import net.praqma.clearcase.exceptions.DeliverException.Type;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.entities.UCMEntity.LabelStatus;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.Config;
import net.praqma.hudson.Util;
import net.praqma.hudson.exception.CCUCMException;
import net.praqma.hudson.exception.DeliverNotCancelledException;
import net.praqma.hudson.exception.ScmException;
import net.praqma.hudson.exception.TemplateException;
import net.praqma.hudson.nametemplates.NameTemplate;
import net.praqma.hudson.notifier.CCUCMNotifier;
import net.praqma.hudson.remoting.*;
import net.praqma.hudson.scm.Polling.PollingType;
import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.util.StopWatch;
import net.praqma.util.execute.AbnormalProcessTerminationException;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

/**
 * is responsible for everything regarding Hudsons connection to ClearCase
 * pre-build. This class defines all the files required by the user. The
 * information can be entered on the config page. 
 */
public class CCUCMScm extends SCM {

    private static Logger logger = Logger.getLogger( CCUCMScm.class.getName() );
	
	/* Currently only for testing */
	private Boolean multisitePolling;

	private Project.PromotionLevel plevel;
	private String levelToPoll;
	private String loadModule;
	private String component;
	private String stream;
	private String bl;
	private StringBuffer pollMsgs = new StringBuffer();
	private Stream integrationstream;
	private boolean doPostBuild = true;
	private String buildProject;
	private String jobName = "";
	private Integer jobNumber;
	private String id = "";

	public static CCUCMState ccucm = new CCUCMState();
	private boolean forceDeliver;

	/* Old notifier fields */
	private boolean recommend;
	private boolean makeTag;
	private boolean setDescription;
	private Unstable treatUnstable;
	private boolean createBaseline;
	private String nameTemplate;

	/* Threshold in milliseconds */
	private Polling polling;
	private String viewtag = "";
	private Baseline lastBaseline;

	private static DateFormat dateFormatter = new SimpleDateFormat( "yyyyMMdd" );

	/**
	 * Default constructor, mainly used for unit tests.
	 */
	public CCUCMScm() {
	}

	@DataBoundConstructor
	public CCUCMScm( String component, String levelToPoll, String loadModule, boolean newest, String polling, String stream, String treatUnstable, 
			         boolean createBaseline, String nameTemplate, boolean forceDeliver, boolean recommend, boolean makeTag, boolean setDescription, String buildProject ) {

		this.component = component;
		this.levelToPoll = levelToPoll;
		this.loadModule = loadModule;
		this.stream = stream;
		this.buildProject = buildProject;

		this.polling = new Polling( polling );
		this.treatUnstable = new Unstable( treatUnstable );

		this.createBaseline = createBaseline;
		this.nameTemplate = nameTemplate;

		this.forceDeliver = forceDeliver;
		this.recommend = recommend;
		this.makeTag = makeTag;
		this.setDescription = setDescription;
		this.plevel = Util.getLevel( levelToPoll );
	}

	@Override
	public boolean checkout( AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile ) throws IOException, InterruptedException {
		/* Prepare job variables */

		jobName = build.getParent().getDisplayName().replace( ' ', '_' );
		jobNumber = build.getNumber();
		this.id = "[" + jobName + "::" + jobNumber + "]";

		boolean result = true;

		PrintStream consoleOutput = listener.getLogger();

        /* Printing short description of build */
		String version = Hudson.getInstance().getPlugin( "clearcase-ucm-plugin" ).getWrapper().getVersion();
		consoleOutput.println( "[" + Config.nameShort + "] ClearCase UCM Plugin version " + version );
		consoleOutput.println( "[" + Config.nameShort + "] Allow for slave polling: " + this.getSlavePolling() );
		consoleOutput.println( "[" + Config.nameShort + "] Poll for posted deliveries: " + this.getMultisitePolling() );
		consoleOutput.println( "[" + Config.nameShort + "] Forcing deliver: " + forceDeliver );

		logger.info( id + "CCUCMSCM checkout v. " + version );
		
		/* Check for ClearCase on remote */
		try {
			workspace.act( new RemoteClearCaseCheck() );
		} catch( AbnormalProcessTerminationException e ) {
			ExceptionUtils.print( e, consoleOutput, true );
			build.setDescription( e.getMessage() );
			throw new AbortException( e.getMessage() );
		}

		doPostBuild = true;

		logger.fine( "STATES: " + ccucm.stringify() );
		/* If we polled, we should get the same object created at that point */
		State state = null;
		try {
			state = ccucm.getState( jobName, jobNumber );
			logger.fine( "The existing state is: " + state.stringify() );
		} catch( IllegalStateException e ) {
			logger.fine( e.getMessage() );
			state = ccucm.create( jobName, jobNumber );
		}

		state.setLoadModule( loadModule );
		storeStateParameters( state );
		
		/* Make build action */
		CCUCMBuildAction action = new CCUCMBuildAction( state.getStream(), state.getComponent() );
		build.addAction( action );
		
		logger.info( "Number of states: " + ccucm.size() );

		logger.fine( id + "The initial state:\n" + state.stringify() );

		/* Check template */
		state.setCreatebaseline( createBaseline );
		/* Trim template, strip out quotes */
		if( nameTemplate.matches( "^\".+\"$" ) ) {
			nameTemplate = nameTemplate.substring( 1, nameTemplate.length() - 1 );
		}
		state.setNameTemplate( nameTemplate );

		/* Check input */
		if( !checkInput( listener ) ) {
			state.setPostBuild( false );
			return false;
		}

		// state.setLoggerSetting( Logger.getLoggerSettings(
		// app.getMinimumLevel() ) );

		/* Determining the Baseline modifier */
		String baselineInput = getBaselineValue( build );

		/* The special Baseline case */
		if( build.getBuildVariables().get( baselineInput ) != null ) {
			logger.fine( "BASELINE: " + baselineInput );
			polling = new Polling( PollingType.none );
			result = doBaseline( build, baselineInput, state, listener );
		} else {
			consoleOutput.println( "[" + Config.nameShort + "] Polling streams: " + polling.toString() );
			result = pollStream( workspace, build.getProject(), state, listener );
			if( !result ) {
				consoleOutput.println( "[" + Config.nameShort + "] No valid baselines found" );
			}
		}

		state.setPolling( polling );
		
		/* If a baseline is found */
		if( state.getBaseline() != null && result ) {
			consoleOutput.println( "[" + Config.nameShort + "] Using " + state.getBaseline() );
			
			/* Add the found baseline to the action */
			action.setBaseline( state.getBaseline() );
			
			baselineName = state.getBaseline().getFullyQualifiedName();
			
			/*
			 * if( setDescription ) { build.setDescription("<small>" +
			 * state.getBaseline().getShortname() + "</small>"); }
			 */

			if( polling.isPollingSelf() || !polling.isPolling() ) {
				logger.fine( "Initializing workspace" );
				result = initializeWorkspace( build, workspace, changelogFile, listener, state );
				if( plevel == null ) {
					/* Save */
					logger.fine( "Storing last baseline" );
					storeLastBaseline( state.getBaseline(), build.getProject() );
				}
			} else {
				/* Only start deliver when NOT polling self */
				logger.fine( "Deliver" );
				result = beginDeliver( build, state, listener, changelogFile );
			}
			
			action.setViewTag( viewtag );
		}

		consoleOutput.println( "[" + Config.nameShort + "] Pre build steps done" );

		/* If plevel is not null, make sure that the CCUCMNotifier is ON */
		if( plevel != null ) {
			boolean used = false;
			for( Publisher p : build.getParent().getPublishersList() ) {
				logger.fine( "NOTIFIER: " + p.toString() );
				if( p instanceof CCUCMNotifier ) {
					used = true;
					break;
				}
			}

			if( !used ) {
				logger.info( "Adding notifier to project" );
				build.getParent().getPublishersList().add( new CCUCMNotifier() );
			}
			
			
			/* If there's a result let's find out whether a baseline is found or not */
			if( result ) {
				if( state.getBaseline() == null ) {
					consoleOutput.println( "[" + Config.nameShort + "] Finished processing; the baseline is null, this could pose as a problem!" );
				} else {
					consoleOutput.println( "[" + Config.nameShort + "] Finished processing " + state.getBaseline() );
				}
			}
			
			/* If plevel is null, make sure CCUCMNotofier is not enabled */
		} else {
			Iterator<Publisher> it = build.getParent().getPublishersList().iterator();
			while( it.hasNext() ) {
				Publisher p = it.next();
				if( p instanceof CCUCMNotifier ) {
					it.remove();
				}
			}

			/* End the view */
			try {
				logger.fine( "Ending view " + viewtag );
                workspace.act( new EndView( viewtag ) );
			} catch( Exception e ) {
				consoleOutput.println( e.getMessage() );
				logger.log( Level.WARNING, "", e );
			}
		}

		return result;
	}

	private boolean storeLastBaseline( Baseline baseline, AbstractProject<?, ?> project ) {
		FileWriter fw = null;
		try {
			fw = new FileWriter( new File( project.getRootDir(), ".lastbaseline" ), false );
			fw.write( baseline.getFullyQualifiedName() );
		} catch( IOException e ) {
			logger.warning( "Could not write last baseline" );
			return false;
		} finally {
			if( fw != null ) {
				try {
					fw.close();
				} catch( IOException e ) {
					logger.warning( "Unable to close file" );
				}
			}
		}

		return true;
	}

	private Baseline getLastBaseline( AbstractProject<?, ?> project, TaskListener listener ) throws ScmException {
		FileReader fr = null;
		PrintStream out = listener.getLogger();
		try {
			fr = new FileReader( new File( project.getRootDir(), ".lastbaseline" ) );
			BufferedReader br = new BufferedReader( fr );
			String bls = br.readLine();
			logger.fine( "Read " + bls );
			if( bls == null || bls.length() == 0 ) {
				throw new ScmException( "No last baseline stored", null );
			}
			Baseline bl = Baseline.get( bls );
			Baseline loaded = (Baseline) RemoteUtil.loadEntity( project.getSomeWorkspace(), bl, getSlavePolling() );
			return loaded;
		} catch( FileNotFoundException e ) {
		} catch( IOException e ) {
			logger.warning( "Could not read last baseline" );
			throw new ScmException( "Could not read last baseline", e );
		} catch( ClearCaseException e ) {
			logger.warning( "Unable to get last baseline!" );
			throw new ScmException( "Unable to get last baseline", e );
			// } catch( CCUCMException e ) {
			// logger.warning( "Unable to load last baseline" );
			// throw new ScmException( "Unable to load last baseline" );
		} catch( CCUCMException e ) {
			out.println( "Unable to load entity: " + e.getMessage() );
			e.printStackTrace();
		} finally {
			if( fr != null ) {
				try {
					fr.close();
				} catch( IOException e ) {
					logger.warning( "Unable to close file" );
				}
			}
		}

		return null;
	}

	private boolean checkInput( TaskListener listener ) {
		PrintStream out = listener.getLogger();

		/* Check baseline template */
		if( createBaseline ) {
			/* Sanity check */
			if( polling.isPollingOther() ) {
				if( nameTemplate != null && nameTemplate.length() > 0 ) {
					try {
						NameTemplate.testTemplate( nameTemplate );
					} catch( TemplateException e ) {
						out.println( "[" + Config.nameShort + "] The template could not be parsed correctly: " + e.getMessage() );
						return false;
					}
				} else {
					out.println( "[" + Config.nameShort + "] A valid template must be provided to create a Baseline" );
					return false;
				}
			} else {
				out.println( "[" + Config.nameShort + "] You cannot create a baseline in this mode" );
			}
		}

		/* Check polling vs plevel */
		if( plevel == null ) {
			if( polling.isPollingSelf() ) {
				return true;
			} else {
				out.println( "[" + Config.nameShort + "] You cannot poll any on other than self" );
				return false;
			}
		}

		return true;
	}

	private boolean initializeWorkspace( AbstractBuild<?, ?> build, FilePath workspace, File changelogFile, BuildListener listener, State state ) {

		PrintStream consoleOutput = listener.getLogger();

		EstablishResult er = null;
		try {
            CheckoutTask ct = new CheckoutTask( listener, jobName, build.getNumber(), state.getStream(), loadModule, state.getBaseline(), buildProject, ( plevel == null ) );
            er = workspace.act( ct );
			String changelog = er.getMessage();

			this.viewtag = er.getViewtag();
			state.setChangeset( er.getChangeset() );

			/* Write change log */
			try {
				FileOutputStream fos = new FileOutputStream( changelogFile );
				fos.write( changelog.getBytes() );
				fos.close();
			} catch( IOException e ) {
				logger.fine( id + "Could not write change log file" );
				consoleOutput.println( "[" + Config.nameShort + "] Could not write change log file" );
			}

		} catch( Exception e ) {
			consoleOutput.println( "[" + Config.nameShort + "] Unable to initialize workspace" );
			Exception cause = (Exception) e.getCause();
			
			if( cause != null ) {
				try {
					throw cause;
				} catch( Exception e1 ) {
					ExceptionUtils.print( cause, consoleOutput, true );
					ExceptionUtils.log( cause, true );
				}
			} else {
				ExceptionUtils.print( cause, consoleOutput, true );
				ExceptionUtils.log( cause, true );
			}
			
			doPostBuild = false;
			state.setPostBuild( false );
			return false;
		}

		return true;
	}

	public boolean doBaseline( AbstractBuild<?, ?> build, String baselineInput, State state, BuildListener listener ) {

		PrintStream consoleOutput = listener.getLogger();
		boolean result = true;

		String baselinename = (String) build.getBuildVariables().get( baselineInput );
		try {
			state.setBaseline( Baseline.get( baselinename ) );
			state.setStream( state.getBaseline().getStream() );
			consoleOutput.println( "[" + Config.nameShort + "] Starting parameterized build with a CCUCM_baseline." );
			/* The component could be used in the post build section */
			state.setComponent( state.getBaseline().getComponent() );
			state.setStream( state.getBaseline().getStream() );
			logger.fine( id + "Saving the component for later use" );
		} catch( ClearCaseException e ) {
			consoleOutput.println( "[" + Config.nameShort + "] Could not find baseline from parameter '" + baselinename + "'." );
			state.setPostBuild( false );
			result = false;
			state.setBaseline( null );
		}

		return result;
	}

	public String getBaselineValue( AbstractBuild<?, ?> build ) {
		Collection<?> c = build.getBuildVariables().keySet();
		Iterator<?> i = c.iterator();

		while( i.hasNext() ) {
			String next = i.next().toString();
			if( next.equalsIgnoreCase( "baseline" ) ) {
				return next;
			}
		}

		return null;
	}

	public class AscendingDateSort implements Comparator<Baseline> {

		@Override
		public int compare( Baseline bl1, Baseline bl2 ) {
			if( bl2.getDate() == null ) {
				return 1;
			}
			if( bl1.getDate() == null ) {
				return -1;
			}
			return (int) ( ( bl1.getDate().getTime() / 1000 ) - ( bl2.getDate().getTime() / 1000 ) );
		}
	}

	private boolean pollStream( FilePath workspace, AbstractProject<?, ?> project, State state, BuildListener listener ) {
        logger.fine( "Polling stream " + state.getStream().getNormalizedName() );
		boolean result = true;
		PrintStream out = listener.getLogger();

		try {

			printParameters( out );
			state.setStream( Stream.get( stream ) );
			/* The Stream must be loaded */
			try {
				state.setStream( (Stream) RemoteUtil.loadEntity( workspace, state.getStream(), getSlavePolling() ) );
			} catch( CCUCMException e1 ) {
				ExceptionUtils.print( e1, out, true );
				throw new AbortException( "Unable to load " + state.getStream() );
			}

			if( !state.isAddedByPoller() ) {
				logger.fine( "This job was not added by a poller" );

				List<Baseline> baselines = null;
				/* Old skool self polling */
				if( polling.isPollingSelf() ) {
					baselines = getValidBaselinesFromStream( workspace, state, plevel, state.getStream(), state.getComponent() );
				} else {
					/* Find the Baselines and store them */
					baselines = getBaselinesFromStreams( workspace, listener, out, state, state.getStream(), state.getComponent(), polling.isPollingChilds() );
				}

				filterBaselines( project, state.getStream(), baselines );

				/* if we did not find any baselines we should return false */
				if( baselines.size() < 1 ) {
					return false;
				}

				/* Sort by date */
				Collections.sort( baselines, new AscendingDateSort() );

				state.setBaselines( baselines );
				state.setBaseline( selectBaseline( state.getBaselines(), plevel ) );
			}

			if( state.getBaselines() == null || state.getBaselines().size() < 1 ) {
				return false;
			}

			/* Print the baselines to jenkins out */
			printBaselines( state.getBaselines(), out );
			out.println( "" );

		} catch( Exception e ) {
			out.println( "[" + Config.nameShort + "] " + e.getMessage() );
			logger.log( Level.WARNING, "", e );
			return false;
		}

		return result;
	}

	public boolean beginDeliver( AbstractBuild<?, ?> build, State state, BuildListener listener, File changelogFile ) {
		FilePath workspace = build.getWorkspace();
		PrintStream consoleOutput = listener.getLogger();
		boolean result = true;

		EstablishResult er = new EstablishResult();

		try {
			logger.config( "Starting remote deliver" );

            RemoteDeliver rmDeliver = new RemoteDeliver( state.getStream().getFullyQualifiedName(), listener, loadModule, state.getBaseline().getFullyQualifiedName(), build.getParent().getDisplayName(), state.getForceDeliver() );
            er = workspace.act( rmDeliver );

			CCUCMBuildAction action = build.getAction( CCUCMBuildAction.class );
			action.setViewPath( er.getView().getViewRoot() );
			action.setViewTag( er.getViewtag() );
			
			//state.setSnapView( er.getView() );
			this.viewtag = er.getViewtag();

			/* Write change log */
			try {
				FileOutputStream fos = new FileOutputStream( changelogFile );
				fos.write( er.getMessage().getBytes() );
				fos.close();
			} catch( IOException e ) {
				logger.fine( id + "Could not write change log file" );
				consoleOutput.println( "[" + Config.nameShort + "] Could not write change log file" );
			}

			consoleOutput.println( "[" + Config.nameShort + "] Deliver successful" );

			state.setChangeset( er.getChangeset() );
			
		/* Deliver failed */
		} catch( Exception e ) {
			consoleOutput.println( "[" + Config.nameShort + "] Deliver failed" );
			result = false;
			
			/* Check for exception types */
			Exception cause = (Exception) net.praqma.util.ExceptionUtils.unpackFrom( IOException.class, e );
			
			consoleOutput.println( "[" + Config.nameShort + "] Cause: " + cause.getClass() );
			
			/* Re-throw */
			try {
				ExceptionUtils.log( cause, true );
				throw cause;
			} catch( DeliverException de ) {
				
				consoleOutput.println( "[" + Config.nameShort + "] " + de.getType() );
				
				/* We need to store this information anyway */
				CCUCMBuildAction action = build.getAction( CCUCMBuildAction.class );
				action.setViewPath( de.getDeliver().getViewContext() );
				action.setViewTag( de.getDeliver().getViewtag() );
				
				/* The deliver is started, cancel it */
				if( de.isStarted() ) {
					try {
						consoleOutput.print( "[" + Config.nameShort + "] Cancelling deliver. " );
						RemoteUtil.completeRemoteDeliver( workspace, listener, state, de.getDeliver().getViewtag(), de.getDeliver().getViewContext(), false );
						consoleOutput.println( "Success" );

						/* Make sure, that the post step is not run */
						state.setNeedsToBeCompleted( false );

					} catch( Exception ex ) {
						consoleOutput.println( "[" + Config.nameShort + "] Failed to cancel deliver" );
						consoleOutput.println( "[" + Config.nameShort + "] Original error:" );
						ExceptionUtils.print( de, consoleOutput, true );
						consoleOutput.println( "[" + Config.nameShort + "] Cancellation error:" );
						ExceptionUtils.print( ex, consoleOutput, true );
						logger.log( Level.WARNING, "", de );
                        logger.log( Level.WARNING, "", ex );
					}
				} else {
					logger.fine( id + "No need for completing deliver" );
					state.setNeedsToBeCompleted( false );
				}
				
				/* Write something useful to the output */
				if( de.getType().equals( Type.MERGE_ERROR ) ) {
					try {
						consoleOutput.println( "[" + Config.nameShort + "] Changes need to be manually merged, The stream " + state.getBaseline().getStream().getShortname() + " must be rebased to the most recent baseline on " + state.getStream().getShortname() + " - During the rebase the merge conflict should be solved manually. Hereafter create a new baseline on " + state.getBaseline().getStream().getShortname() + "." );
						state.setError( "merge error" );
					} catch( Exception e3 ) {
					}
				}
				
			/* Force deliver not cancelled */
			} catch( DeliverNotCancelledException e1 ) {
				consoleOutput.println( "[" + Config.nameShort + "] Failed to force cancel existing deliver" );
				state.setNeedsToBeCompleted( false );
			} catch( Exception e1 ) {
                logger.log( Level.WARNING, "", e );
				ExceptionUtils.print( e, consoleOutput, true );
				result = false;
			}
		}


		try {
			state.setStream( Stream.get( stream ) );
		} catch( ClearCaseException e ) {
			consoleOutput.println( "[" + Config.nameShort + "] " + e.getMessage() );
            logger.log( Level.WARNING, "", e );
			result = false;
		}

		return result;
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		return new ChangeLogParserImpl();
	}
	
	private String baselineName = "";
	
	public String getBaselineName() {
		return baselineName;
	}

	@Override
	public void buildEnvVars( AbstractBuild<?, ?> build, Map<String, String> env ) {
		super.buildEnvVars( build, env );
		
		String CC_BASELINE = "";
		String CC_VIEWPATH = "";
		String CC_VIEWTAG  = "";
		
		try {
			
			CCUCMBuildAction action = build.getAction( CCUCMBuildAction.class );
			CC_BASELINE = action.getBaseline().getFullyQualifiedName();
		} catch( Exception e1 ) {
			System.out.println( "Failed to get baseline: " + e1.getMessage() );
			try {
				State state = ccucm.getState( jobName, jobNumber );
				
				/* Baseline */
				if( state.getBaseline() != null ) {
					CC_BASELINE = state.getBaseline().getFullyQualifiedName();
				} else {
					CC_BASELINE = "";
				}
			} catch( Exception e2 ) {
				System.out.println( "Variables not available: " + e2.getMessage() );
				
				/* Try env vars */
				try {
					System.out.println( "Trying with env vars" );
					String VAR_JOBNAME = env.get( "JOB_NAME" );
					String n = env.get( "BUILD_NUMBER" );
					int VAR_BUILDNUMBER = Integer.parseInt( n );
					
					System.out.println( "VARS: " + VAR_JOBNAME + ", " + VAR_BUILDNUMBER );
					
					State state = ccucm.getState( VAR_JOBNAME, VAR_BUILDNUMBER );
					
					/* Baseline */
					if( state.getBaseline() != null ) {
						CC_BASELINE = state.getBaseline().getFullyQualifiedName();
					} else {
						CC_BASELINE = "";
					}
				} catch( Exception e3 ) {
					System.out.println( "Not possible to retrieve variables: " + e3.getMessage() );
				}
			}
		}
		
		/* View tag */
		CC_VIEWTAG = viewtag;
		
		/* View path */
		String workspace = env.get( "WORKSPACE" );
		if( workspace != null ) {
			CC_VIEWPATH = workspace + File.separator + "view";
		} else {
			CC_VIEWPATH = "";
		}

		env.put( "CC_BASELINE", CC_BASELINE );
		env.put( "CC_VIEWTAG", CC_VIEWTAG );
		env.put( "CC_VIEWPATH", CC_VIEWPATH );
	}

	/**
	 * This method polls the version control system to see if there are any
	 * changes in the source code.
	 * 
	 */
	@Override
	public PollingResult compareRemoteRevisionWith( AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState rstate ) throws IOException, InterruptedException {

		/**/
		try {
			workspace.act( new RemoteClearCaseCheck() );
		} catch( AbnormalProcessTerminationException e )  {
			throw new AbortException( e.getMessage() );
		}
		
		jobName = project.getDisplayName().replace( ' ', '_' );
		jobNumber = project.getNextBuildNumber();
		this.id = "[" + jobName + "::" + jobNumber + "]";

		PollingResult p = PollingResult.NO_CHANGES;



		/* Interrupt polling if: */
		if( this.getMultisitePolling() ) {
			/* multisite polling and a build is in progress */
			if( project.isBuilding() ) {
				logger.fine( "A build already building - cancelling poll" );
	            return PollingResult.NO_CHANGES;
			}
		} else {
			/* not multisite polling and a the project is already in queue */
			if( project.isInQueue() ) {
				logger.fine( "A build already in queue - cancelling poll" );
	            return PollingResult.NO_CHANGES;
			}
		}

		boolean createdByThisPoll = false;
		State state = null;
		try {
			state = ccucm.getState( jobName, jobNumber );
			logger.fine( "The existing state is: " + state.stringify() );
			logger.info( "Let's NOT poll" );
			System.out.println( "Undo polling for " + jobName + " " + jobNumber );
			return p;
		} catch( IllegalStateException e ) {
			logger.fine( e.getMessage() );
			state = ccucm.create( jobName, jobNumber );
			createdByThisPoll = true;
		}
		
		logger.fine( "Need for polling" );

		storeStateParameters( state );
		
		PrintStream out = listener.getLogger();
		printParameters( out );
		
		/* The Stream must be loaded */
		try {
			state.setStream( (Stream) RemoteUtil.loadEntity( workspace, state.getStream(), getSlavePolling() ) );
		} catch( CCUCMException e1 ) {
			ExceptionUtils.print( e1, out, true );
			throw new AbortException( "Unable to load " + state.getStream() );
		}

		out.println( "[" + Config.nameShort + "] polling streams: " + polling );

		state.setCreatebaseline( createBaseline );
		/* Trim template, strip out quotes */
		if( nameTemplate.matches( "^\".+\"$" ) ) {
			nameTemplate = nameTemplate.substring( 1, nameTemplate.length() - 1 );
		}
		state.setNameTemplate( nameTemplate );

        /*
		StopWatch sw = StopWatch.get( jobName + "-" + jobNumber );
		sw.reset();
		sw.start();
		*/

		logger.fine( "Let's go!" );

		/* Check input */
		if( checkInput( listener ) ) {
			try {
				List<Baseline> baselines = null;

				/* Old skool self polling */
				if( polling.isPollingSelf() ) {
					baselines = getValidBaselinesFromStream( workspace, state, plevel, state.getStream(), state.getComponent() );
				} else {
					/* Find the Baselines and store them */
					baselines = getBaselinesFromStreams( workspace, listener, out, state, state.getStream(), state.getComponent(), polling.isPollingChilds() );
				}

				logger.fine( "I found " + baselines.size() + " baseline" + ( baselines.size() == 1 ? "" : "s" ) );

				/* Discard baselines */
				filterBaselines( project,state.getStream(), baselines );

				logger.fine( "When filtered, I have " + baselines.size() + " baseline" + ( baselines.size() == 1 ? "" : "s" ) );

				if( baselines.size() > 0 ) {
					p = PollingResult.BUILD_NOW;
					/* Sort by date */
					Collections.sort( baselines, new AscendingDateSort() );

					state.setBaselines( baselines );
					state.setBaseline( selectBaseline( state.getBaselines(), plevel ) );

					/* If ANY */
					if( plevel == null ) {
						try {
							lastBaseline = getLastBaseline( project, listener );
						} catch( ScmException e ) {
							out.println( e.getMessage() );
						}

						boolean newer = false;

						/*
						 * if the newest found baseline is newer than the last
						 * baseline, build it If there's no last baseline, build
						 * it
						 */
						if( lastBaseline != null ) {

							try {
								out.println( "The last baseline: " + lastBaseline.stringify() );
							} catch( Exception e ) {
								out.println( "Could not stringify last: " + e.getMessage() );
								e.printStackTrace( out );
							}

							try {
								out.println( "The found baseline: " + state.getBaseline().stringify() );
							} catch( Exception e ) {
								out.println( "Could not stringify state baseline" );
							}

							// if( lastBaseline.getDate().after(
							// state.getBaseline().getDate() ) ) {
							if( state.getBaseline().getDate().after( lastBaseline.getDate() ) ) {
								// if
								// (lastBaseline.getFullyQualifiedName().equals(state.getBaseline().getFullyQualifiedName()))
								// {
								newer = true;
							}
						} else {
							newer = true;
						}

						if( !newer ) {
							p = PollingResult.NO_CHANGES;
						}
					}

				} else {
					p = PollingResult.NO_CHANGES;
				}

				logger.fine( id + "The POLL state:\n" + state.stringify() ); // 5413_dev_from_BUILT

				/* Remove state if not being built */
				if( p == PollingResult.NO_CHANGES ) {
					state.remove();
				}
			} catch( ScmException e ) {
				out.println( "Error while retrieving baselines: " + e.getMessage() );
				logger.warning( "Error while retrieving baselines: " + e.getMessage() );
				p = PollingResult.NO_CHANGES;
			}
		}

		/* Remove state if not being built */
		if( p.equals( PollingResult.NO_CHANGES ) ) {
			logger.fine( id + "No new baselines to build" );
			if( createdByThisPoll ) {
				logger.fine( id + "Removing: " + state.stringify() );
				state.remove();
			}
		} else {
			logger.fine( id + "New baseline to build" );
			state.setAddedByPoller( true );
		}

        /*
		sw.stop();
		logger.fine( "Polling took " + sw.getSeconds() + " seconds" );
		out.println( "Polling took " + sw.getSeconds() + " seconds" );
		sw.stop();
		*/

		return p;
	}

	/**
	 * Get the {@link Baseline}s from a {@link Stream}s related Streams.
	 */
	private List<Baseline> getBaselinesFromStreams( FilePath workspace, TaskListener listener, PrintStream consoleOutput, State state, Stream stream, Component component, boolean pollingChildStreams ) {

		List<Stream> streams = null;
		List<Baseline> baselines = new ArrayList<Baseline>();

		try {
			streams = RemoteUtil.getRelatedStreams( workspace, listener, stream, pollingChildStreams, this.getSlavePolling(), this.getMultisitePolling() );
		} catch( CCUCMException e1 ) {
			e1.printStackTrace( consoleOutput );
			logger.warning( "Could not retrieve streams: " + e1.getMessage() );
			consoleOutput.println( "[" + Config.nameShort + "] No streams found" );
			return baselines;
		}

		consoleOutput.println( "[" + Config.nameShort + "] Scanning " + streams.size() + " stream" + ( streams.size() == 1 ? "" : "s" ) + " for baselines." );

		int c = 1;
		for( Stream s : streams ) {
			try {
				consoleOutput.printf( "[" + Config.nameShort + "] [%02d] %s ", c, s.getShortname() );
				c++;
				List<Baseline> found = RemoteUtil.getRemoteBaselinesFromStream( workspace, component, s, plevel, this.getSlavePolling(), this.getMultisitePolling() );
				for( Baseline b : found ) {
					baselines.add( b );
				}
				consoleOutput.println( found.size() + " baseline" + ( found.size() == 1 ? "" : "s" ) + " found" );
			} catch( CCUCMException e ) {
				consoleOutput.println( "No baselines: " + e.getMessage() );
			}
		}

		consoleOutput.println( "" );

		return baselines;
	}

	/**
	 * Given the {@link Stream}, {@link Component} and {@link net.praqma.clearcase.ucm.entities.Project.PromotionLevel} a list of
	 * valid {@link Baseline}s is returned.
	 */
	private List<Baseline> getValidBaselinesFromStream( FilePath workspace, State state, Project.PromotionLevel plevel, Stream stream, Component component ) throws ScmException {
		logger.fine( id + "Retrieving valid baselines." );

		/* The baseline list */
		List<Baseline> baselines = new ArrayList<Baseline>();

		try {
			baselines = RemoteUtil.getRemoteBaselinesFromStream( workspace, component, stream, plevel, this.getSlavePolling(), this.getMultisitePolling() );
		} catch( CCUCMException e1 ) {
			// throw new ScmException("Unable to get baselines from " +
			// stream.getShortname() + ": " + e1.getMessage());
			/* no op */
			logger.fine( "No baselines: " + e1.getMessage() );
			throw new ScmException("Unable to get baselines from " + stream.getShortname(), e1 );
		}

		return baselines;
	}

	/**
	 * Filter out baselines that is involved in a deliver or does not have a
	 * label
	 * 
	 * @param baselines
	 */
	private void filterBaselines( AbstractProject<?, ?> project, Stream stream, List<Baseline> baselines ) {

		/* Remove deliver baselines */
		Iterator<Baseline> it = baselines.iterator();
		while( it.hasNext() ) {
			Baseline baseline = it.next();

			/* Get the state for the current baseline */
			State cstate = ccucm.getStateByBaseline( jobName, baseline.getFullyQualifiedName() );

			if( baseline.getShortname().startsWith( "deliverbl." ) || baseline.getLabelStatus().equals( LabelStatus.UNLABLED ) ) {
				it.remove();
			} else if( !baseline.getMastership().equals( stream.getMastership() ) ) {
				it.remove();
			} else if( cstate != null ) {
				/* The baseline is in progress, determine if the job is still running */
				Integer bnum = cstate.getJobNumber();
				Build bld = (Build) project.getBuildByNumber( bnum );

				/* prevent null pointer exceptions */
				if( bld != null ) {
					if( baseline.getPromotionLevel( true ).equals( cstate.getBaseline().getPromotionLevel( true ) ) ) {
						logger.fine( id + baseline.getShortname() + " has the same promotion level" );
						it.remove();
					} else if( bld.isLogUpdated() ) {
						logger.fine( id + "Job " + bld.getNumber() + " is building " + cstate.getBaseline().getFullyQualifiedName() );
						it.remove();
					} else {
						/* The job is not running */
						logger.fine( id + "Job " + bld.getNumber() + " is not building" );
					} 
				}
			}
		}
	}

	/**
	 * Store the globally defined parameters in the state object
	 * 
	 * @param state
	 */
	private void storeStateParameters( State state ) {

		try {
			state.setStream( Stream.get( stream ) );
		} catch( ClearCaseException e ) {
            logger.log( Level.WARNING, "", e );
		}

		try {
			state.setComponent( Component.get( component ) );
		} catch( ClearCaseException e ) {
            logger.log( Level.WARNING, "", e );
		}

		state.setUnstable( treatUnstable );

		/* Notifier stuff */
		state.setSetDescription( setDescription );
		state.setMakeTag( makeTag );
		state.setRecommend( recommend );
		state.setForceDeliver( forceDeliver );

		state.setPlevel( plevel );
	}

	@Override
	public SCMRevisionState calcRevisionsFromBuild( AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener ) throws IOException, InterruptedException {
		SCMRevisionState scmRS = null;

		if( bl != null ) {
			scmRS = new SCMRevisionStateImpl();
		}
		return scmRS;
	}

	private Baseline selectBaseline( List<Baseline> baselines, Project.PromotionLevel plevel ) {
		if( baselines.size() > 0 ) {
			if( plevel != null ) {
				return baselines.get( 0 );
			} else {
				return baselines.get( baselines.size() - 1 );
			}
		} else {
			return null;
		}
	}

	private void printParameters( PrintStream ps ) {
		ps.println( "[" + Config.nameShort + "] Getting baselines for :" );
		ps.println( "[" + Config.nameShort + "] * Stream:          " + stream );
		ps.println( "[" + Config.nameShort + "] * Component:       " + component );
		ps.println( "[" + Config.nameShort + "] * Promotion level: " + levelToPoll );
		ps.println( "" );
	}

	public void printBaselines( List<Baseline> baselines, PrintStream ps ) {
		if( baselines != null ) {
			ps.println( "[" + Config.nameShort + "] Retrieved " + baselines.size() + " baseline" + ( baselines.size() == 1 ? "" : "s" ) + ":" );
			if( !( baselines.size() > 8 ) ) {
				for( Baseline b : baselines ) {
					ps.println( "[" + Config.nameShort + "] + " + b.getShortname() + "(" + b.getDate() + ")" );
				}
			} else {
				int i = baselines.size();
				ps.println( "[" + Config.nameShort + "] + " + baselines.get( 0 ).getShortname() + "(" + baselines.get( 0 ).getDate() + ")" );
				ps.println( "[" + Config.nameShort + "] + " + baselines.get( 1 ).getShortname() + "(" + baselines.get( 1 ).getDate() + ")" );
				ps.println( "[" + Config.nameShort + "] + " + baselines.get( 2 ).getShortname() + "(" + baselines.get( 2 ).getDate() + ")" );
				ps.println( "[" + Config.nameShort + "]   ..." );
				ps.println( "[" + Config.nameShort + "] + " + baselines.get( i - 3 ).getShortname() + "(" + baselines.get( i - 3 ).getDate() + ")" );
				ps.println( "[" + Config.nameShort + "] + " + baselines.get( i - 2 ).getShortname() + "(" + baselines.get( i - 2 ).getDate() + ")" );
				ps.println( "[" + Config.nameShort + "] + " + baselines.get( i - 1 ).getShortname() + "(" + baselines.get( i - 1 ).getDate() + ")" );
			}
		}
	}

	/*
	 * The following getters and booleans (six in all) are used to display saved
	 * userdata in Hudsons gui
	 */
	public String getLevelToPoll() {
		return levelToPoll;
	}

	public String getComponent() {
		return component;
	}

	public String getStream() {
		return stream;
	}

	public String getLoadModule() {
		return loadModule;
	}

	/*
	 * getStreamObject() and getBaseline() are used by CCUCMNotifier to get the
	 * Baseline and Stream in use, but does not work with concurrent builds!!!
	 */
	public Stream getStreamObject() {
		return integrationstream;
	}

	@Exported
	public String getBaseline() {
		return bl;
	}

	public boolean getSlavePolling() {
		CCUCMScmDescriptor desc = (CCUCMScmDescriptor) this.getDescriptor();
		return desc.getSlavePolling();

	}

	public boolean getMultisitePolling() {
		if( this.multisitePolling != null ) {
			return this.multisitePolling;
		} else {
			CCUCMScmDescriptor desc = (CCUCMScmDescriptor) this.getDescriptor();
			return desc.getMultisitePolling();
		}
	}

	@Exported
	public String getPolling() {
		return polling.toString();
	}

	@Exported
	public String getTreatUnstable() {
		return treatUnstable.toString();
	}

	@Exported
	public boolean doPostbuild() {
		return doPostBuild;
	}

	public String getBuildProject() {
		return buildProject;
	}

	public boolean getForceDeliver() {
		return forceDeliver;
	}

	public boolean isCreateBaseline() {
		return this.createBaseline;
	}

	public String getNameTemplate() {
		return this.nameTemplate;
	}

	public boolean isMakeTag() {
		return this.makeTag;
	}

	public boolean isSetDescription() {
		return this.setDescription;
	}

	public boolean isRecommend() {
		return this.recommend;
	}
	
	public void setMultisitePolling( boolean mp ) {
		this.multisitePolling = mp;
	}

	/**
	 * This class is used to describe the plugin to Hudson
	 * 
	 * @author Troels Selch
	 * @author Margit Bennetzen
	 * 
	 */
	@Extension
	public static class CCUCMScmDescriptor extends SCMDescriptor<CCUCMScm> implements hudson.model.ModelObject {

		private boolean slavePolling;
		private boolean multisitePolling;
		private List<String> loadModules;

		public CCUCMScmDescriptor() {
			super( CCUCMScm.class, null );
			loadModules = getLoadModules();
			load();
		}

		/**
		 * This method is called, when the user saves the global Hudson
		 * configuration.
		 */
		@Override
		public boolean configure( org.kohsuke.stapler.StaplerRequest req, JSONObject json ) throws FormException {
			try {
				String s = json.getString( "slavePolling" );
				if( s != null ) {
					slavePolling = Boolean.parseBoolean( s );
				}
				s = json.getString( "multisitePolling" );
				if( s != null ) {
					multisitePolling = Boolean.parseBoolean( s );
				}
			} catch( Exception e ) {
				e.getMessage();
			}

			save();

			return true;
		}

		public boolean getSlavePolling() {
			return slavePolling;
		}

		public boolean getMultisitePolling() {
			return multisitePolling;
		}
		/**
		 * This is called by Hudson to discover the plugin name
		 */
		@Override
		public String getDisplayName() {
			return "ClearCase UCM";
		}

		/**
		 * This method is called by the scm/CCUCM/global.jelly to validate the
		 * input without reloading the global configuration page
		 * 
		 * @param value
		 * @return
		 */
		public FormValidation doExecutableCheck( @QueryParameter String value ) {
			return FormValidation.validateExecutable( value );
		}

		public FormValidation doCheckTemplate( @QueryParameter String value ) throws FormValidation {
			try {
				NameTemplate.testTemplate( NameTemplate.trim( value ) );
				return FormValidation.ok( "The template seems ok" );
			} catch( TemplateException e ) {
				throw FormValidation.error( "Does not appear to be a valid template: " + e.getMessage() );
			}
		}

		public void doLevelCheck( @QueryParameter String polling, @QueryParameter String level ) throws FormValidation {
			System.out.println( "LEVEL CHECK: " + polling + " + " + level );
			if( level.equalsIgnoreCase( "any" ) && !polling.equals( "self" ) ) {
				throw FormValidation.error( "You can only combine self and any" );
			}
		}

		@Override
		public CCUCMScm newInstance( StaplerRequest req, JSONObject formData ) throws FormException {
			try {
				String polling = formData.getString( "polling" );
				String level = formData.getString( "levelToPoll" );

				if( level.equalsIgnoreCase( "any" ) ) {
					if( !polling.equalsIgnoreCase( "self" ) ) {
						throw new FormException( "You can only use any with self polling", "polling" );
					}
				}
			} catch( JSONException e ) {
				throw new FormException( "You missed some fields: " + e.getMessage(), "CCUCM.polling" );
			}
			CCUCMScm instance = req.bindJSON( CCUCMScm.class, formData );
			/* TODO This is actually where the Notifier check should be!!! */
			return instance;
		}

		/**
		 * Used by Hudson to display a list of valid promotion levels to build
		 * from. The list of promotion levels is hard coded in
		 * net.praqma.hudson.Config.java
		 * 
		 * @return
		 */
		public List<String> getLevels() {
			return Config.getLevels();
		}

		/**
		 * Used by Hudson to display a list of loadModules (whether to poll all
		 * or only modifiable elements
		 * 
		 * @return
		 */
		public List<String> getLoadModules() {
			loadModules = new ArrayList<String>();
			loadModules.add( "All" );
			loadModules.add( "Modifiable" );
			return loadModules;
		}
	}
}
