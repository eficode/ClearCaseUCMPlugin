package net.praqma.hudson.remoting;

import java.io.File;

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
            i.get();
            return;
            
        } catch( Exception e ) {
            throw new PucmException( "Failed to " + ( complete ? "complete" : "cancel" ) + " the deliver: " + e.getMessage() );
        }
    }
    
    public static void createRemoteBaseline( FilePath workspace, BuildListener listener, String baseName, String componentName, File view ) throws PucmException {
        
        try {
            Future<Boolean> i = null;

            i = workspace.actAsync(new CreateRemoteBaseline( baseName, componentName, view, listener ) );
            i.get();
            return;
            
        } catch( Exception e ) {
            throw new PucmException( e.getMessage() );
        }
    }
}
