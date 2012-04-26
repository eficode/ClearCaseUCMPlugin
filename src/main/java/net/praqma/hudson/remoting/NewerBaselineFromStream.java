package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.appenders.StreamAppender;
import hudson.FilePath.FileCallable;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;

public class NewerBaselineFromStream implements FileCallable<Boolean> {

	private static final long serialVersionUID = -8984877325832486334L;

	private Baseline baseline;
	private Stream stream;
	private Pipe pipe;
	
	private Set<String> subscriptions;
	
	public NewerBaselineFromStream( Stream stream, Baseline baseline, Pipe pipe, Set<String> subscriptions ) {
		this.baseline = baseline;
		this.stream = stream;
		this.pipe = pipe;
		
		this.subscriptions = subscriptions;
    }
    
    @Override
    public Boolean invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
    	
    	Logger logger = Logger.getLogger();
    	boolean newer = true;

    	StreamAppender app = null;
    	if( pipe != null ) {
	    	PrintStream toMaster = new PrintStream( pipe.getOut() );	    	
	    	app = new StreamAppender( toMaster );
	    	Logger.addAppender( app );
	    	app.setSubscriptions( subscriptions );
    	}
    	
    	logger.info( "Retrieving remote baselines from " + stream.getShortname() );
    	
        /* The baseline list */
        List<Baseline> baselines;
		try {
			baselines = stream.getLatestBaselines();
			for( Baseline bl : baselines ) {
				if( bl.getFullyQualifiedName().equals( baseline.getFullyQualifiedName() ) ) {
					newer = false;
					break;
				}
			}
		} catch( ClearCaseException e ) {
			logger.warning( "Could not match baselines" );
		}
        
        Logger.removeAppender( app );

        return newer;
    }

}
