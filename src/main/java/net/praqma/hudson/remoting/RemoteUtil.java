package net.praqma.hudson.remoting;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.entities.Project.Plevel;
import net.praqma.hudson.exception.CCUCMException;
import net.praqma.hudson.scm.CCUCMState.State;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.TaskListener;

public abstract class RemoteUtil {

	public static void completeRemoteDeliver( FilePath workspace, BuildListener listener, State state, boolean complete ) throws CCUCMException {

		try {
			workspace.act( new RemoteDeliverComplete( state.getBaseline(), state.getStream(), state.getSnapView(), state.getChangeset(), complete, listener ) );
		} catch (Exception e) {
			throw new CCUCMException( "Failed to " + ( complete ? "complete" : "cancel" ) + " the deliver: " + e.getMessage() );
		}
	}

	public static Baseline createRemoteBaseline( FilePath workspace, BuildListener listener, String baseName, Component component, File view, String username ) throws CCUCMException {

		try {
			return workspace.act( new CreateRemoteBaseline( baseName, component, view, username, listener ) );
		} catch (Exception e) {
			throw new CCUCMException( e.getMessage() );
		}
	}

	public static List<Baseline> getRemoteBaselinesFromStream( FilePath workspace, Component component, Stream stream, Plevel plevel ) throws CCUCMException {

		try {
			return workspace.act( new GetRemoteBaselineFromStream( component, stream, plevel ) );
		} catch (Exception e) {
			throw new CCUCMException( e.getMessage() );
		}
	}


	public static List<Stream> getRelatedStreams( FilePath workspace, TaskListener listener, Stream stream, boolean pollingChildStreams ) throws CCUCMException {

		PrintStream out = listener.getLogger();
		
		try {
			return workspace.act( new GetRelatedStreams( listener, stream, pollingChildStreams ) );
		} catch (Exception e) {
			e.printStackTrace( out );
			throw new CCUCMException( e.getMessage() );
		}
	}
	
	public static UCMEntity loadEntity( FilePath workspace, UCMEntity entity ) throws CCUCMException {
		
		try {
			return workspace.act(  new LoadEntity( entity ) );
		} catch (Exception e) {
			throw new CCUCMException( e.getMessage() );
		}
	}
	
	public static String getClearCaseVersion( FilePath workspace, Project project ) throws CCUCMException {
		
		try {
			return workspace.act( new GetClearCaseVersion( project ) );
		} catch (Exception e) {
			throw new CCUCMException( e.getMessage() );
		}
	}
}
