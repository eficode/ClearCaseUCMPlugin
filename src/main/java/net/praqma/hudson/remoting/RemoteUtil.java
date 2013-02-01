package net.praqma.hudson.remoting;

import java.io.File;
import java.util.Date;
import java.util.List;

import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.hudson.exception.CCUCMException;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import java.util.logging.Logger;

public abstract class RemoteUtil {
    
    private static final Logger logger = Logger.getLogger(RemoteUtil.class.getName());
    
    private RemoteUtil() {}

	public static void completeRemoteDeliver( FilePath workspace, BuildListener listener, Baseline baseline, Stream stream, String viewtag, File viewPath, boolean complete ) throws CCUCMException {
		try {
            workspace.act( new RemoteDeliverComplete( baseline, stream, viewtag, viewPath, complete, listener ) );
		} catch( Exception e ) {
            logger.warning(String.format("Caught exception in RemoteUtil: %s", e));
			throw new CCUCMException( "Failed to " + ( complete ? "complete" : "cancel" ) + " the deliver", e );
		}
	}

	public static Baseline createRemoteBaseline( FilePath workspace, BuildListener listener, String baseName, Component component, File view, String username ) throws CCUCMException {
		try {
            return workspace.act( new CreateRemoteBaseline( baseName, component, view, username, listener ) );
		} catch( Exception e ) {
            logger.warning(String.format("Caught exception in RemoteUtil(createRemoteBaseline): %s", e));
			throw new CCUCMException( e );
		}
	}


	public static UCMEntity loadEntity( FilePath workspace, UCMEntity entity, boolean slavePolling ) throws CCUCMException {
		try {
			if( slavePolling ) {
				return workspace.act( new LoadEntity( entity ) );
			} else {
				LoadEntity t = new LoadEntity( entity );
				return t.invoke( null, null );
			}
		} catch( Exception e ) {
            logger.warning(String.format("Caught exception in RemoteUtil(loadEntity): %s", e));
			throw new CCUCMException( e );
		}
	}

	public static String getClearCaseVersion( FilePath workspace, Project project ) throws CCUCMException {
		try {
			return workspace.act( new GetClearCaseVersion( project ) );
		} catch( Exception e ) {
            logger.warning(String.format("Caught exception in RemoteUtil(getClearCaseVersion): %s", e));
			throw new CCUCMException( e );
		}
	}

	public static void endView( FilePath workspace, String viewtag ) throws CCUCMException {
		try {
			workspace.act( new EndView( viewtag ) );
		} catch( Exception e ) {
            logger.warning(String.format("Caught exception in RemoteUtil(endView): %s", e));
			throw new CCUCMException( e );
		}
	}

    public static List<Stream> getRelatedStreams( FilePath workspace, TaskListener listener, Stream stream, boolean pollingChildStreams, boolean slavePolling, boolean multisitePolling ) throws CCUCMException {
        try {
            if( slavePolling ) {
                return workspace.act( new GetRelatedStreams( listener, stream, pollingChildStreams, multisitePolling ) );
            } else {
                GetRelatedStreams t = new GetRelatedStreams( listener, stream, pollingChildStreams, multisitePolling );
                return t.invoke( null, null );
            }
        } catch( Exception e ) {
            logger.warning(String.format("Caught exception in RemoteUtil: %s", e.getMessage()));
            logger.warning(String.format("Caught exception in RemoteUtil(getRealtedStreams): %s",e));
            throw new CCUCMException( e );
        }
    }


    public static List<Baseline> getRemoteBaselinesFromStream( FilePath workspace, Component component, Stream stream, Project.PromotionLevel plevel, boolean slavePolling, boolean multisitePolling, Date date ) throws CCUCMException {
        
        try {
            if( slavePolling ) {
                return workspace.act( new GetRemoteBaselineFromStream( component, stream, plevel, multisitePolling, date ) );
            } else {
                GetRemoteBaselineFromStream t = new GetRemoteBaselineFromStream( component, stream, plevel, multisitePolling, date );
                return t.invoke( null, null );
            }

        } catch( Exception e ) {
            logger.warning(String.format("Caught exception in RemoteUtil: %s", e.getMessage()));
            logger.warning(String.format("Caught exception in RemoteUtil: %s", e.toString()));
            throw new CCUCMException( e );
        }
    }
}
