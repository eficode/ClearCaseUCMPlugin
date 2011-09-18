package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.appenders.StreamAppender;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;

public class CreateRemoteBaseline implements FileCallable<String> {

	private static final long serialVersionUID = -8984877325832486334L;

	private String baseName;
	private Component component;
	private File view;
	private BuildListener listener;
	private String username;
	private Pipe pipe;
	
	public CreateRemoteBaseline( String baseName, Component component, File view, String username, BuildListener listener, Pipe pipe ) {
		this.baseName = baseName;
		this.component = component;
		this.view = view;
		this.listener = listener;
		this.username = username;
		this.pipe = pipe;
    }
    
    @Override
    public String invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
        PrintStream out = listener.getLogger();
        
    	StreamAppender app = null;
    	if( pipe != null ) {
	    	PrintStream toMaster = new PrintStream( pipe.getOut() );
	    	app = new StreamAppender( toMaster );
	    	Logger.addAppender( app );
    	}
        
    	Baseline bl = null;
    	try {
			bl = Baseline.create( baseName, component, view, true, true );
		} catch (UCMException e) {
        	if( pipe != null ) {
        		Logger.removeAppender( app );
        	}
			throw new IOException( "Unable to create Baseline:" + e.getMessage() );
		}
    	
    	try {
			bl.changeOwnership( username, null );
		} catch (UCMException e) {
        	if( pipe != null ) {
        		Logger.removeAppender( app );
        	}
			throw new IOException( "Unable to change ownership of " + baseName + ":" + e.getMessage() );
		}
    
    	if( pipe != null ) {
    		Logger.removeAppender( app );
    	}
        return bl.getFullyQualifiedName();
    }

}
