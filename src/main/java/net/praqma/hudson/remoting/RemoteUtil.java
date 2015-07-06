package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
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
import net.praqma.hudson.scm.Polling;
import net.praqma.util.structure.Tuple;

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

	public static Baseline createRemoteBaseline( FilePath workspace, String baseName, Component component, File view ) throws IOException, InterruptedException {
        return workspace.act( new CreateRemoteBaseline( baseName, component, view ) );
	}
    
    public static Baseline createRemoteBaseline( FilePath workspace, String baseName, Stream stream, Component component, File view ) throws IOException, InterruptedException {
        return workspace.act( new CreateRemoteBaseline( baseName, stream, component, view ) );
	}

	public static UCMEntity loadEntity( FilePath workspace, UCMEntity entity, boolean slavePolling ) throws IOException, InterruptedException {
		if( slavePolling ) {
            return workspace.act( new LoadEntity( entity ) );
        } else {
            LoadEntity t = new LoadEntity( entity );
            return t.invoke( null, null );
        }
	}

	public static String getClearCaseVersion( FilePath workspace, Project project ) throws IOException, InterruptedException {
		return workspace.act( new GetClearCaseVersion( project ) );
	}

	public static void endView( FilePath workspace, String viewtag ) throws IOException, InterruptedException {
		workspace.act( new EndView( viewtag ) );

	}

    public static List<Stream> getRelatedStreams( FilePath workspace, TaskListener listener, Stream stream, Polling polling, boolean slavePolling, boolean multisitePolling, String hLinkFeedFrom ) throws IOException, InterruptedException {
        if( slavePolling ) {
            return workspace.act( new GetRelatedStreams( listener, stream, polling, multisitePolling, hLinkFeedFrom) );
        } else {
            GetRelatedStreams t = new GetRelatedStreams( listener, stream, polling, multisitePolling, hLinkFeedFrom );
            return t.invoke( null, null );
        }
    }


    public static List<Baseline> getRemoteBaselinesFromStream( FilePath workspace, Component component, Stream stream, Project.PromotionLevel plevel, boolean slavePolling, boolean multisitePolling, Date date ) throws IOException, InterruptedException {
        if( slavePolling ) {
            return workspace.act( new GetRemoteBaselineFromStream( component, stream, plevel, multisitePolling, date ) );
        } else {
            GetRemoteBaselineFromStream t = new GetRemoteBaselineFromStream( component, stream, plevel, multisitePolling, date );
            return t.invoke( null, null );
        }
    }
    
    public static Tuple<List<Baseline>,List<Baseline>> getRemoteRebaseCandidatesFromStream(FilePath workspace, Stream stream, List<String> excludeComponents, Project.PromotionLevel plevel) throws IOException, InterruptedException {
        return workspace.act(new GetRebaseBaselines(stream, excludeComponents, plevel));
    }
}
