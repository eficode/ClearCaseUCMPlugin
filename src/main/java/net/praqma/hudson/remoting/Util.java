package net.praqma.hudson.remoting;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;

import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Project.Plevel;
import net.praqma.hudson.exception.CCUCMException;
import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.util.debug.Logger;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.remoting.Future;
import hudson.remoting.Pipe;

public abstract class Util {
	
	private static Logger logger = Logger.getLogger();

	public static void completeRemoteDeliver( FilePath workspace, BuildListener listener, State state, boolean complete ) throws CCUCMException {

		try {
			if( workspace.isRemote() ) {
				final Pipe pipe = Pipe.createRemoteToLocal();
				Future<Boolean> i = null;
				i = workspace.actAsync( new RemoteDeliverComplete( state.getBaseline(), state.getStream(), state.getSnapView(), state.getChangeset(), complete, listener, pipe ) );
				logger.redirect( pipe.getIn() );
				i.get();
			} else {
				Future<Boolean> i = null;
				i = workspace.actAsync( new RemoteDeliverComplete( state.getBaseline(), state.getStream(), state.getSnapView(), state.getChangeset(), complete, listener, null ) );
				i.get();
			}
			return;

		} catch (Exception e) {
			throw new CCUCMException( "Failed to " + ( complete ? "complete" : "cancel" ) + " the deliver: " + e.getMessage() );
		}
	}

	public static Baseline createRemoteBaseline( FilePath workspace, BuildListener listener, String baseName, Component component, File view, String username ) throws CCUCMException {

		try {
			if( workspace.isRemote() ) {
				final Pipe pipe = Pipe.createRemoteToLocal();
				Future<Baseline> i = null;
				i = workspace.actAsync( new CreateRemoteBaseline( baseName, component, view, username, listener, pipe ) );
				logger.redirect( pipe.getIn() );
				return i.get();
			} else {
				Future<Baseline> i = null;
				i = workspace.actAsync( new CreateRemoteBaseline( baseName, component, view, username, listener, null ) );
				return i.get();
			}

		} catch (Exception e) {
			throw new CCUCMException( e.getMessage() );
		}
	}

	public static List<Baseline> getRemoteBaselinesFromStream( FilePath workspace, Component component, Stream stream, Plevel plevel ) throws CCUCMException {

		try {
			if( workspace.isRemote() ) {
				final Pipe pipe = Pipe.createRemoteToLocal();
				Future<List<Baseline>> i = null;
				i = workspace.actAsync( new GetRemoteBaselineFromStream( component, stream, plevel, pipe ) );
				logger.redirect( pipe.getIn() );
				return i.get();
			} else {
				Future<List<Baseline>> i = null;
				i = workspace.actAsync( new GetRemoteBaselineFromStream( component, stream, plevel, null ) );
				return i.get();
			}

		} catch (Exception e) {
			throw new CCUCMException( e.getMessage() );
		}
	}


	public static List<Stream> getRelatedStreams( FilePath workspace, TaskListener listener, Stream stream, boolean pollingChildStreams ) throws CCUCMException {

		PrintStream out = listener.getLogger();
		
		try {
			
			if( workspace.isRemote() ) {
				final Pipe pipe = Pipe.createRemoteToLocal();
				Future<List<Stream>> i = null;
				i = workspace.actAsync( new GetRelatedStreams( listener, stream, pollingChildStreams, pipe ) );
				logger.redirect( pipe.getIn() );
				return i.get();
			} else {
				Future<List<Stream>> i = null;
				i = workspace.actAsync( new GetRelatedStreams( listener, stream, pollingChildStreams, null ) );
				return i.get();

			}

		} catch (Exception e) {
			e.printStackTrace( out );
			throw new CCUCMException( e.getMessage() );
		}
	}
}
