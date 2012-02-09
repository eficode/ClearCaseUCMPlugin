package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.Logger.LogLevel;
import net.praqma.util.debug.LoggerSetting;
import net.praqma.util.debug.appenders.StreamAppender;

import hudson.FilePath.FileCallable;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;

public class LoadEntity implements FileCallable<UCMEntity> {

	private static final long serialVersionUID = -8984877325832486334L;

	private UCMEntity entity;
	private Pipe pipe;
	
	private LoggerSetting loggerSetting;
	private PrintStream pstream;
	
	public LoadEntity( UCMEntity entity, Pipe pipe, PrintStream pstream, LoggerSetting loggerSetting ) {
		this.entity = entity;
		this.pipe = pipe;
		
		this.pstream = pstream;
		
		this.loggerSetting = loggerSetting;
    }
    
    @Override
    public UCMEntity invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
        
    	StreamAppender app = null;
    	if( pipe != null ) {
	    	PrintStream toMaster = new PrintStream( pipe.getOut() );
	    	app = new StreamAppender( toMaster );
	    	app.lockToCurrentThread();
	    	Logger.addAppender( app );
	    	app.setSettings( loggerSetting );
    	} else if( pstream != null ) {
	    	app = new StreamAppender( pstream );
	    	app.lockToCurrentThread();
	    	Logger.addAppender( app );
	    	app.setSettings( loggerSetting );    		
    	}
        
    	try {
    		UCM.setContext( UCM.ContextType.CLEARTOOL );
			entity.load();
		} catch (UCMException e) {
        	Logger.removeAppender( app );
        	throw new IOException( "Unable to load " + entity.getShortname() + ":" + e.getMessage() );
		}

    	Logger.removeAppender( app );

    	return entity;
    }

}
