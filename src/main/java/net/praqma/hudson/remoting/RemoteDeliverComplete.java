package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.hudson.scm.ClearCaseChangeset.Element;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.appenders.StreamAppender;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;

public class RemoteDeliverComplete implements FileCallable<Boolean> {

	private static final long serialVersionUID = 2506984544940354996L;

	private State state;
	private boolean complete;
	private BuildListener listener;
	private Pipe pipe;

	public RemoteDeliverComplete( State state, boolean complete, BuildListener listener, Pipe pipe ) {
		this.state = state;
		this.complete = complete;
		this.listener = listener;
		this.pipe = pipe;
	}

	@Override
	public Boolean invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
		
		PrintStream out = listener.getLogger();
		
		out.println( "Setting up logger" );
		
    	StreamAppender app = null;
    	if( pipe != null ) {
	    	PrintStream toMaster = new PrintStream( pipe.getOut() );
	    	app = new StreamAppender( toMaster );
	    	Logger.addAppender( app );
    	}

    	if( complete ) {
    		out.println( "Completing" );

			try {
				out.println( "Before" );
				state.getBaseline().deliver( state.getBaseline().getStream(), state.getStream(), state.getSnapView().getViewRoot(), state.getSnapView().getViewtag(), true, true, true );
				out.println( "After" );
			} catch (UCMException ex) {

				try {
					state.getBaseline().cancel( state.getSnapView().getViewRoot() );
				} catch (UCMException ex1) {
	        		Logger.removeAppender( app );
					throw new IOException( "Completing the deliver failed. Could not cancel." );
				}
	        	Logger.removeAppender( app );
				throw new IOException( "Completing the deliver failed. Deliver was cancelled." );
			}
			
			/* All is well, change ownerships */
			for( Element e : state.getChangeset().getList() ) {
				try {
					UCMEntity.changeOwnership( e.getVersion(), e.getUser(), state.getSnapView().getViewRoot() );
				} catch( UCMException ex ) {
					Logger.removeAppender( app );
					out.println( "Unable to change ownership: " + ex.getMessage() );
				}
			}
			
		} else {
			out.println( "Cancelling" );
			try {
				state.getBaseline().cancel( state.getSnapView().getViewRoot() );
			} catch (UCMException ex) {
				Logger.removeAppender( app );
				throw new IOException( "Could not cancel the deliver." );
			}
		}

		Logger.removeAppender( app );
		return true;
	}

}
