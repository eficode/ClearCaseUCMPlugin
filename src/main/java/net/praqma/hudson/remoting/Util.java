package net.praqma.hudson.remoting;

import java.io.PrintStream;

import net.praqma.hudson.exception.PucmException;
import net.praqma.hudson.scm.PucmState.State;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.remoting.Future;

public abstract class Util {

    public static void completeRemoteDeliver( FilePath workspace, BuildListener listener, State state, boolean complete ) throws PucmException {
                
        try {
            Future<Boolean> i = null;

            i = workspace.actAsync(new RemoteDeliverComplete( state, complete ) );

            return;
            
        } catch( Exception e ) {
            throw new PucmException( "Failed to " + ( complete ? "complete" : "cancel" ) + " the deliver: " + e.getMessage() );
        }
    }
}
