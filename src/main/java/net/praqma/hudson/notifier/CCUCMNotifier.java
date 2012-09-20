package net.praqma.hudson.notifier;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerRequest;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.UnableToPromoteBaselineException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.Config;
import net.praqma.hudson.exception.CCUCMException;
import net.praqma.hudson.exception.NotifierException;
import net.praqma.hudson.nametemplates.NameTemplate;
import net.praqma.hudson.remoting.RemoteUtil;
import net.praqma.hudson.scm.CCUCMScm;
import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.util.debug.appenders.FileAppender;
import net.sf.json.JSONObject;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.Future;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

/**
 * CCUCMNotifier perfoms the user-chosen CCUCM post-build actions
 * 
 * @author Troels Selch
 * @author Margit Bennetzen
 * 
 */
public class CCUCMNotifier extends Notifier {

	private PrintStream out;

	private Status status;
	private String id = "";
	private static Logger logger = Logger.getLogger( CCUCMNotifier.class.getName() );
	private String jobName = "";
	private Integer jobNumber = 0;

	//private SimpleDateFormat logformat  = new SimpleDateFormat( "yyyyMMdd-HHmmss" );

	public CCUCMNotifier() {
	}

	/**
	 * This constructor is used in the inner class <code>DescriptorImpl</code>.
	 */
	public CCUCMNotifier( boolean recommended, boolean makeTag, boolean setDescription ) {

	}

	/**
	 * This indicates whether to let CCUCM run after(true) the job is done or
	 * before(false)
	 */
	@Override
	public boolean needsToRunAfterFinalized() {
		return false;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener ) throws InterruptedException, IOException {

		boolean result = true;
		out = listener.getLogger();

		status = new Status();

		/* Prepare job variables */
		jobName = build.getParent().getDisplayName().replace( ' ', '_' );
		jobNumber = build.getNumber();
		this.id = "[" + jobName + "::" + jobNumber + "]";

		SCM scmTemp = build.getProject().getScm();
		if( !( scmTemp instanceof CCUCMScm ) ) {
			/* SCM is not ClearCase ucm, just move it along... Not fail it, duh! */
			return true;
		}

		State pstate = null;
		Baseline baseline = null;
		
		CCUCMBuildAction action = build.getAction( CCUCMBuildAction.class );

		/* Only do this part if a valid CCUCMScm build */
		if( result ) {
			/* Retrieve the CCUCM state */
			logger.fine( "STATES: " + CCUCMScm.ccucm.stringify() );
			try {
				pstate = CCUCMScm.ccucm.getState( jobName, jobNumber );
			} catch( IllegalStateException e ) {
				System.err.println( e.getMessage() );
				CCUCMScm.ccucm.signalFault( jobName, jobNumber );
				out.println( "[" + Config.nameShort + "] " + e.getMessage() );
                logger.log( Level.WARNING, "", e );

				return false;
			}

			logger.fine( "STATE: " + pstate );
			logger.fine( pstate.stringify() );

			/* Validate the state */
			if( pstate.doPostBuild() && pstate.getBaseline() != null ) {
				logger.fine( id + "Post build" );

				/*
				 * This shouldn't actually be necessary!? TODO Maybe the
				 * baseline should be re-Load()ed instead of creating a new
				 * object?
				 */
				String bl = pstate.getBaseline().getFullyQualifiedName();

				/*
				 * If no baselines found bl will be null and the post build
				 * section will not proceed
				 */
				if( bl != null ) {
					try {
						baseline = Baseline.get( bl );
					} catch( ClearCaseException e ) {
						logger.warning( id + "Could not initialize baseline." );
						baseline = null;
					}

					if( baseline == null ) {
						/*
						 * If baseline is null, the user has already been
						 * notified in Console output from CCUCMScm.checkout()
						 */
						result = false;
					}
				} else {
					logger.warning( "Whoops, not a valid baseline" );
					result = false;
				}

			} else {
				logger.warning( "Whoops, not a valid state, there was no baseline found or the post build flag was not set" );
				// Not performing any post build actions.
				result = false;
			}
		} else {
			logger.warning( "WHOA, what happened!? Result = false!!!" );
		}

		/* There's a valid baseline, lets process it */
		if( result ) {
			out.println( "Processing baseline" );
			status.setErrorMessage( pstate.getError() );

			try {
				processBuild( build, launcher, listener, pstate );
				if( pstate.isSetDescription() ) {
					String d = build.getDescription();
					if( d != null ) {
						build.setDescription( ( d.length() > 0 ? d + "<br/>" : "" ) + status.getBuildDescr() );
					} else {
						build.setDescription( status.getBuildDescr() );
					}

				}

			} catch( NotifierException ne ) {
				out.println( ne.getMessage() );
			} catch( IOException e ) {
				out.println( "[" + Config.nameShort + "] Couldn't set build description." );
			}
		} else {
			out.println( "[" + Config.nameShort + "] Nothing to do!" );
			String d = build.getDescription();
			if( d != null ) {
				build.setDescription( ( d.length() > 0 ? d + "<br/>" : "" ) + "Nothing to do" );
			} else {
				build.setDescription( "Nothing to do" );
			}

			build.setResult( Result.NOT_BUILT );
		}

		if( action != null && action.getViewTag() != null ) {
			/* End the view */
			try {
				logger.fine( "Ending view " + action.getViewTag() );
				RemoteUtil.endView( build.getWorkspace(), action.getViewTag() );
			} catch( CCUCMException e ) {
				out.println( e.getMessage() );
				logger.warning( e.getMessage() );
			}
		}

		out.println( "[" + Config.nameShort + "] Post build steps done" );

		return result;
	}

	/**
	 * This is where all the meat is. When the baseline is validated, the actual
	 * post build steps are performed. <br>
	 * First the baseline is delivered(if chosen), then tagged, promoted and
	 * recommended.
	 * 
	 * @param build
	 *            The build object in which the post build action is selected
	 * @param launcher
	 *            The launcher of the build
	 * @param listener
	 *            The listener of the build
	 * @param pstate
	 *            The {@link net.praqma.hudson.scm.CCUCMState} of the build.
	 * @throws NotifierException
	 */
	private void processBuild( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, State pstate ) throws NotifierException {
		Result buildResult = build.getResult();

		VirtualChannel ch = launcher.getChannel();

		if( ch == null ) {
			logger.fine( "The channel was null" );
		}

		FilePath workspace = build.getExecutor().getCurrentWorkspace();

		if( workspace == null ) {
			logger.warning( "Workspace is null" );
			throw new NotifierException( "Workspace is null" );
		}

		out.println( "[" + Config.nameShort + "] Build result: " + buildResult );
		
		CCUCMBuildAction action = build.getAction( CCUCMBuildAction.class );

		/* Initialize variables for post build steps */
		Stream targetstream = null;
		targetstream = pstate.getBaseline().getStream();
		Stream sourcestream = targetstream;
		Baseline sourcebaseline = pstate.getBaseline();
		Baseline targetbaseline = sourcebaseline;

		logger.fine( "NTBC: " + pstate.needsToBeCompleted() );

		/*
		 * Determine whether to treat the build as successful
		 * 
		 * Success: - deliver complete - create baseline - recommend
		 * 
		 * Fail: - deliver cancel
		 */
		boolean treatSuccessful = buildResult.isBetterThan( pstate.getUnstable().treatSuccessful() ? Result.FAILURE : Result.UNSTABLE );

		/*
		 * Finalize CCUCM, deliver + baseline Only do this for child and sibling
		 * polling
		 */
		if( pstate.needsToBeCompleted() && pstate.getPolling().isPollingOther() ) {
			status.setBuildStatus( buildResult );

			try {
				out.print( "[" + Config.nameShort + "] " + ( treatSuccessful ? "Completing" : "Cancelling" ) + " the deliver. " );
				RemoteUtil.completeRemoteDeliver( workspace, listener, pstate, action.getViewTag(), action.getViewPath(), treatSuccessful );
				out.println( "Success." );

				/* If deliver was completed, create the baseline */
				if( treatSuccessful && pstate.createBaseline() ) {

					try {
						pstate.setBuild( build );
						pstate.setListener( listener );
						out.print( "[" + Config.nameShort + "] Creating baseline on Integration stream. " );
						/* Load project for stream */
						logger.fine( "1STREAM: " + pstate.getStream() );
						logger.fine( "1PROJECT: " + pstate.getStream().getProject() );
						RemoteUtil.loadEntity( workspace, pstate.getStream(), true );
						
						pstate.setWorkspace( workspace );
						NameTemplate.validateTemplates( pstate );
						String name = NameTemplate.parseTemplate( pstate.getNameTemplate(), pstate );

						targetbaseline = RemoteUtil.createRemoteBaseline( workspace, listener, name, pstate.getBaseline().getComponent(), action.getViewPath(), pstate.getBaseline().getUser() );

						/**/
						if( action != null ) {
							action.setCreatedBaseline( targetbaseline );
						}
					} catch( Exception e ) {
						ExceptionUtils.print( e, out, false );
						logger.warning( "Failed to create baseline on stream" );
						logger.log( Level.WARNING, "", e );
						/* We cannot recommend a baseline that is not created */
						if( pstate.doRecommend() ) {
							out.println( "[" + Config.nameShort + "] Cannot recommend Baseline when not created" );
						}
						
						/* Set unstable? */
						logger.warning( "Failing build because baseline could not be created" );
						build.setResult( Result.FAILURE );

						pstate.setRecommend( false );
					}
				}

			} catch( Exception e ) {
				status.setBuildStatus( buildResult );
				status.setStable( false );
				out.println( "Failed." );
				logger.log( Level.WARNING, "", e );

				/* We cannot recommend a baseline that is not created */
				if( pstate.doRecommend() ) {
					out.println( "[" + Config.nameShort + "] Cannot recommend a baseline when deliver failed" );
				}
				pstate.setRecommend( false );

				/* If trying to complete and it failed, try to cancel it */
				if( treatSuccessful ) {
					try {
						out.print( "[" + Config.nameShort + "] Trying to cancel the deliver. " );
						RemoteUtil.completeRemoteDeliver( workspace, listener, pstate, action.getViewTag(), action.getViewPath(), false );
						out.println( "Success." );
					} catch( Exception e1 ) {
						out.println( " Failed." );
						logger.warning( "Failed to cancel deliver" );
                        logger.log( Level.WARNING, "", e );
					}
				} else {
					logger.warning( "Failed to cancel deliver" );
                    logger.log( Level.WARNING, "", e );
				}
			}
		}

		if( pstate.getPolling().isPollingOther() ) {
			targetstream = pstate.getStream();
		}

		/* Remote post build step, common to all types */
		try {
			logger.fine( id + "Remote post build step" );
			out.println( "[" + Config.nameShort + "] Performing common post build steps" );

			status = workspace.act( new RemotePostBuild( buildResult, status, listener, pstate.isMakeTag(), pstate.doRecommend(), pstate.getUnstable(), sourcebaseline, targetbaseline, sourcestream, targetstream, build.getParent().getDisplayName(), Integer.toString( build.getNumber() ) ) );
		} catch( Exception e ) {
			status.setStable( false );
            logger.log( Level.WARNING, "", e );
			out.println( "[" + Config.nameShort + "] Error: Post build failed" );
			Throwable cause = net.praqma.util.ExceptionUtils.unpackFrom( IOException.class, e );
			
			ExceptionUtils.print( cause, out, true );
		}

		/* If the promotion level of the baseline was changed on the remote */
		if( status.getPromotedLevel() != null ) {
			try {
				pstate.getBaseline().setPromotionLevel( status.getPromotedLevel() );
				logger.fine( id + "Baselines promotion level sat to " + status.getPromotedLevel().toString() );
			} catch( UnableToPromoteBaselineException e ) {
				logger.warning( "Unable to set promotion level of baseline: " + e.getMessage() );
				e.print( out );
			}
		}

		status.setBuildStatus( buildResult );

		if( !status.isStable() ) {
			build.setResult( Result.UNSTABLE );
		}
	}

	/**
	 * This class is used by Hudson to define the plugin.
	 * 
	 * @author Troels Selch
	 * @author Margit Bennetzen
	 * 
	 */
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super( CCUCMNotifier.class );
			load();
		}

		@Override
		public String getDisplayName() {
			return "ClearCase UCM";
		}

		/**
		 * Hudson uses this method to create a new instance of
		 * <code>CCUCMNotifier</code>. The method gets information from Hudson
		 * config page. This information is about the configuration, which
		 * Hudson saves.
		 */
		@Override
		public Notifier newInstance( StaplerRequest req, JSONObject formData ) throws FormException {

			save();

			return new CCUCMNotifier();
		}

		@Override
		public boolean isApplicable( Class<? extends AbstractProject> arg0 ) {
			return true;
		}
	}
}
