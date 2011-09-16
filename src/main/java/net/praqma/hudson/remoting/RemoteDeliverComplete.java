package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.hudson.scm.ClearCaseChangeset.Element;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

public class RemoteDeliverComplete implements FileCallable<Boolean> {

	private static final long serialVersionUID = 2506984544940354996L;

	private State state;
	private boolean complete;
	private BuildListener listener;

	public RemoteDeliverComplete( State state, boolean complete, BuildListener listener ) {
		this.state = state;
		this.complete = complete;
		this.listener = listener;
	}

	@Override
	public Boolean invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
		
		PrintStream out = listener.getLogger();

		if( complete ) {

			try {
				state.getBaseline().deliver( state.getBaseline().getStream(), state.getStream(), state.getSnapView().getViewRoot(), state.getSnapView().getViewtag(), true, true, true );
			} catch (UCMException ex) {

				try {
					state.getBaseline().cancel( state.getSnapView().getViewRoot() );
				} catch (UCMException ex1) {
					throw new IOException( "Completing the deliver failed. Could not cancel." );
				}
				throw new IOException( "Completing the deliver failed. Deliver was cancelled." );
			}
			
			/* All is well, change ownerships */
			for( Element e : state.getChangeset().getList() ) {
				try {
					UCMEntity.changeOwnership( e.getVersion(), e.getUser(), state.getSnapView().getViewRoot() );
				} catch( UCMException ex ) {
					out.println( "Unable to change ownership: " + ex.getMessage() );
				}
			}
			
		} else {
			try {
				state.getBaseline().cancel( state.getSnapView().getViewRoot() );
			} catch (UCMException ex) {
				throw new IOException( "Could not cancel the deliver." );
			}
		}

		return true;
	}

}
