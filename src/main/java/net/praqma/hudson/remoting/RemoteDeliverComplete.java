package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.hudson.scm.PucmState.State;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

public class RemoteDeliverComplete implements FileCallable<Boolean> {

    private static final long serialVersionUID = 2506984544940354996L;
    
    private State state;
    private boolean complete;
    
    public RemoteDeliverComplete( State state, boolean complete ) {
        this.state = state;
        this.complete = complete;
    }
    
    @Override
    public Boolean invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
        
        if ( complete ) {
            
            try {

                state.getBaseline().deliver(state.getBaseline().getStream(), state.getStream(), state.getSnapView().GetViewRoot(), state.getSnapView().GetViewtag(), true, true, true);
                Baseline childBase = state.getBaseline();
                Baseline.create(childBase.getShortname(), childBase.getComponent(), state.getSnapView().GetViewRoot(), true, true);
            } catch (UCMException ex) {
                
                try {
                    state.getBaseline().cancel(state.getSnapView().GetViewRoot());
                } catch (UCMException ex1) {
                    
                    throw new IOException( "Completing the deliver failed. Could not cancel." );
                }
                
                throw new IOException( "Completing the deliver failed. Deliver was cancelled." );
            }
            
            
        } else {
            try {
                state.getBaseline().cancel(state.getSnapView().GetViewRoot());
            } catch (UCMException ex) {
                throw new IOException( "Could not cancel the deliver." );
            }
        }
    
        return true;
    }

}
