package net.praqma.hudson.notifier;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.AbortException;
import org.kohsuke.stapler.StaplerRequest;

import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.Config;
import net.praqma.hudson.exception.NotifierException;
import net.praqma.hudson.nametemplates.NameTemplate;
import net.praqma.hudson.remoting.RemoteUtil;
import net.praqma.hudson.scm.CCUCMScm;
import net.sf.json.JSONObject;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.hudson.exception.TemplateException;
import net.praqma.hudson.remoting.RebaseCancelTask;
import net.praqma.hudson.remoting.RebaseCompleteTask;
import org.apache.commons.lang.StringUtils;

public class CCUCMNotifier extends Notifier {

	private PrintStream out;

	private Status status;
	private static final Logger logger = Logger.getLogger( CCUCMNotifier.class.getName() );
    public static String logShortPrefix = String.format("[%s]", Config.nameShort);

	public CCUCMNotifier() { }
	/**
	 * This indicates whether to let CCUCM run after(true) the job is done or
	 * before(false)
     * @return Always false
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
        
		SCM scmTemp = build.getProject().getScm();
		if( !( scmTemp instanceof CCUCMScm ) ) {
			/* SCM is not ClearCase ucm, just move it along... Not fail it, duh! */
			return true;
		}

		Baseline baseline = null;
		
		CCUCMBuildAction action = build.getAction( CCUCMBuildAction.class );

		if( action != null ) {            
			baseline = action.getBaseline();
		} else {
            throw new AbortException( "No ClearCase Action object found" );
		}

		/* There's a valid baseline, lets process it */
		if( baseline != null ) {
			out.println( String.format ( "%s Processing baseline", "["+Config.nameShort + "]"));
			status.setErrorMessage( action.getError() );
			try {
				processBuild( build, launcher, listener, action );
                logger.fine( action.stringify() );
				if( action.doSetDescription() ) {
					String d = build.getDescription();
                    logger.fine( String.format( "build.getDesciption() is: %s", d ) );
					if( d != null ) {
						build.setDescription( ( d.length() > 0 ? d + "<br/>" : "" ) + status.getBuildDescr() );
					} else {
                        logger.fine( String.format( "Setting build description to: %s", status.getBuildDescr() ) );
						build.setDescription( status.getBuildDescr() );
					}
				}

			} catch( NotifierException ne ) {
				out.println( "NotifierException: " + ne.getMessage() );
			} catch( IOException e ) {
                out.println( String.format( "%s Couldn't set build description", logShortPrefix ) );
			}
		} else {
			if( action.getResolveBaselineException() != null ) {
                build.setResult( Result.FAILURE );
            } else {
                out.println( String.format( "%s Nothing to do!", logShortPrefix ) );
                String d = build.getDescription();
                if( d != null ) {
                    build.setDescription( ( d.length() > 0 ? d + "<br/>" : "" ) + "Nothing to do" );
                } else {
                    build.setDescription( "Nothing to do" );
                }

                build.setResult( Result.NOT_BUILT );
            }
		}

		if( action.getViewTag() != null ) {
            RemoteUtil.endView( build.getWorkspace(), action.getViewTag() );
        }

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
	 * @throws NotifierException
	 */
	private void processBuild( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, CCUCMBuildAction pstate ) throws NotifierException {
		Result buildResult = build.getResult();
        
        String workspace = null;
        FilePath currentWorkspace = build.getExecutor().getCurrentWorkspace();
        try {
            workspace = build.getExecutor().getCurrentWorkspace().absolutize().getRemote();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, String.format("%s Failed to get remote workspace", Config.nameShort), ex);
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, String.format("%s Failed to get remote workspace", Config.nameShort), ex);
        }
        
		if( StringUtils.isBlank(workspace) ) {
			logger.warning( "Workspace is null" );
			throw new NotifierException( "Workspace is null" );
		}

        out.println( String.format( "%s Build result: %s", logShortPrefix, buildResult ) );
		
		CCUCMBuildAction action = build.getAction( CCUCMBuildAction.class );

        logger.fine(String.format("NTBC: %s",pstate.doNeedsToBeCompleted()));
        
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
		if( pstate.doNeedsToBeCompleted() && pstate.getPolling().isPollingOther() ) {
			status.setBuildStatus( buildResult );

			try {
				out.print( logShortPrefix + " " + ( treatSuccessful ? "Completing" : "Cancelling" ) + " the deliver. " );
				RemoteUtil.completeRemoteDeliver( build.getExecutor().getCurrentWorkspace(), listener, pstate.getBaseline(), pstate.getStream(), action.getViewTag(), action.getViewPath(), treatSuccessful );
				out.println( "Success." );

				/* If deliver was completed, create the baseline */
				if( treatSuccessful && pstate.doCreateBaseline() ) {

					try {
                        Baseline succesBaseline = createBaselineOnSuccess(workspace, pstate, build, currentWorkspace, pstate.getBaseline().getComponent());
                        action.setCreatedBaseline( succesBaseline );
						
					} catch( Exception e ) {
						ExceptionUtils.print( e, out, false );
						logger.warning( "Failed to create baseline on stream" );
						logger.log( Level.WARNING, "", e );
						/* We cannot recommend a baseline that is not created */
						if( pstate.doRecommend() ) {
                            out.println( String.format( "%s Cannot recommend Baseline when not created", logShortPrefix ) );							
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
                    out.println( String.format( "%s Cannot recommend a baseline when deliver failed", logShortPrefix ) );
				}
                
				pstate.setRecommend( false );

				/* If trying to complete and it failed, try to cancel it */
				if( treatSuccessful ) {
					try {
                        out.println( String.format("%s Trying to cancel the deliver.", logShortPrefix) );
						//out.print( "[" + Config.nameShort + "] Trying to cancel the deliver. " );
                        
						RemoteUtil.completeRemoteDeliver( currentWorkspace, listener, pstate.getBaseline(), pstate.getStream(), action.getViewTag(), action.getViewPath(), false );
						out.println( "Success." );
					} catch( Exception e1 ) {
						out.println( " Failed." );
						logger.warning( "Failed to cancel deliver" );
                        logger.log( Level.WARNING, "Exception caught - RemoteUtil.completeRemoteDeliver() - TreatSuccesful == true", e1 );
					}
				} else {
					logger.warning( "Failed to cancel deliver" );
                    logger.log( Level.WARNING, "TreatSuccesful == false", e );
				}
			}
		}
        
        //Complete the rebase
        try {
            if( treatSuccessful && pstate.getPolling().isPollingRebase() ) {
                out.println(String.format("%s The build was a succes, completing rebase", logShortPrefix));
                build.getWorkspace().act(new RebaseCompleteTask(pstate.getStream()));
                
                if(pstate.doCreateBaseline()) {                   
                    out.println( String.format( "%s Creating baseline on Integration stream.", logShortPrefix ) );
                    out.println( String.format( "%s Absolute path of remoteWorkspace: %s", logShortPrefix, workspace ) );
                    pstate.setWorkspace( workspace );
                    NameTemplate.validateTemplates( pstate, build.getWorkspace() );
                    String name = NameTemplate.parseTemplate( pstate.getNameTemplate(), pstate, build.getWorkspace() );
                    Baseline createdRebaseBaseline = RemoteUtil.createRemoteBaseline( currentWorkspace, name, pstate.getStream(), pstate.getComponent(), pstate.getViewPath() );
                    action.setCreatedBaseline( createdRebaseBaseline );                
                }
                
            } else if ( !treatSuccessful && pstate.getPolling().isPollingRebase()) {
                out.println(String.format( "%s The build failed, cancelling rebase", logShortPrefix));
                build.getWorkspace().act(new RebaseCancelTask(pstate.getStream()));
            }
        } catch (TemplateException templex) {
            out.println( String.format("%s %s", logShortPrefix, templex.getMessage() ) );
            logger.warning( "Failing build because baseline could not be created in poll rebase" );
            pstate.setRecommend(false);
            build.setResult( Result.FAILURE );            
        } catch (Exception ex) {
            Throwable cause = net.praqma.util.ExceptionUtils.unpackFrom( IOException.class, ex );
            out.println( String.format( "%s Rebase operation failed", logShortPrefix ) );
			ExceptionUtils.print( cause, out, true );
            pstate.setRecommend(false);
            logger.warning( "Failing build because baseline could not be created in poll rebase" );
            build.setResult( Result.FAILURE );            
        }

		/* Remote post build step, common to all types */
		try {
            logger.fine( String.format( "Remote post build step" ) );
            out.println( String.format( "%s Performing common post build steps",logShortPrefix ) );
            
            status = currentWorkspace.act(pstate.getMode().postBuildFinalizer(build, listener, status));
			//status = currentWorkspace.act( new RemotePostBuild( buildResult, status, listener, pstate.doMakeTag(), pstate.doRecommend(), pstate.getUnstable(), skipPromote, sourcebaseline, targetbaseline, sourcestream, targetstream, build.getParent().getDisplayName(), Integer.toString( build.getNumber() ), pstate.getRebaseTargets() ) );
		} catch( Exception e ) {
			status.setStable( false );
            logger.log( Level.WARNING, "", e );
            out.println( String.format( "%s Error: Post build failed", logShortPrefix ) );
			Throwable cause = net.praqma.util.ExceptionUtils.unpackFrom( IOException.class, e );
			ExceptionUtils.print( cause, out, true );
		}

		/* If the promotion level of the baseline was changed on the remote */
		if( status.getPromotedLevel() != null ) {
            logger.fine("Baseline promotion level was remotely changed, " + status.getPromotedLevel() );
			pstate.getBaseline().setLocalPromotionLevel( status.getPromotedLevel() );
		}

        logger.fine("Setting build status on Status object");
		status.setBuildStatus( buildResult );

		if( !status.isStable() ) {
            logger.fine("BuildStatus object marked build unstable");
			build.setResult( Result.UNSTABLE );
		}
	}

    private Baseline createBaselineOnSuccess(String workspace, CCUCMBuildAction pstate, AbstractBuild<?, ?> build, FilePath currentWorkspace, Component component) throws IOException, TemplateException, InterruptedException {
        Baseline targetbaseline;
        out.println( String.format( "%s Creating baseline on Integration stream.", logShortPrefix ) );
        out.println( String.format( "%s Absolute path of remoteWorkspace: %s", logShortPrefix, workspace ) );
        pstate.setWorkspace( workspace );
        NameTemplate.validateTemplates( pstate, build.getWorkspace() );
        String name = NameTemplate.parseTemplate( pstate.getNameTemplate(), pstate, build.getWorkspace() );
        targetbaseline = RemoteUtil.createRemoteBaseline( currentWorkspace, name, component, pstate.getViewPath() );
        return targetbaseline;
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
			return Config.nameLong;
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
