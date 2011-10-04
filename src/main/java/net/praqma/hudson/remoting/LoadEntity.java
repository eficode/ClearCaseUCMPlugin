package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.appenders.StreamAppender;

import hudson.FilePath.FileCallable;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;

public class LoadEntity implements FileCallable<UCMEntity> {

	private static final long serialVersionUID = -8984877325832486334L;

	private UCMEntity entity;
	private Pipe pipe;
	
	private Set<String> subscriptions;
	
	public LoadEntity( UCMEntity entity, Pipe pipe, Set<String> subscriptions ) {
		this.entity = entity;
		this.pipe = pipe;
		
		this.subscriptions = subscriptions;
    }
    
    @Override
    public UCMEntity invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
        
    	StreamAppender app = null;
    	if( pipe != null ) {
	    	PrintStream toMaster = new PrintStream( pipe.getOut() );
	    	app = new StreamAppender( toMaster );
	    	Logger.addAppender( app );
	    	app.setSubscriptions( subscriptions );
    	}
        
    	try {
			entity.load();
		} catch (UCMException e) {
        	Logger.removeAppender( app );
        	throw new IOException( "Unable to load " + entity.getShortname() + ":" + e.getMessage() );
		}

    	Logger.removeAppender( app );

    	return entity;
    }

}