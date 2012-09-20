package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.exceptions.UnableToListBaselinesException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.utils.Baselines;
import hudson.FilePath.FileCallable;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;

public class GetRemoteBaselineFromStream implements FileCallable<List<Baseline>> {

	private static final long serialVersionUID = -8984877325832486334L;

	private Component component;
	private Stream stream;
	private Project.PromotionLevel plevel;
	private boolean multisitePolling;	

	public GetRemoteBaselineFromStream( Component component, Stream stream, Project.PromotionLevel plevel, boolean multisitePolling ) {
		this.component = component;
		this.stream = stream;
		this.plevel = plevel;
		this.multisitePolling = multisitePolling;
	}
    
    @Override
    public List<Baseline> invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
    	
    	Logger logger = Logger.getLogger( GetRemoteBaselineFromStream.class.getName() );

    	
    	logger.fine( "Retrieving remote baselines from " + stream.getShortname() );
    	
        /* The baseline list */
        List<Baseline> baselines = null;
        
        try {
            baselines = Baselines.get( stream, component, plevel, multisitePolling );
        } catch (UnableToInitializeEntityException e) {
            logger.log( Level.SEVERE, "", e );
            throw new IOException("Could not retrieve baselines from repository. " + e.getMessage(), e);
        }
    	catch (UnableToListBaselinesException e) {
        	logger.log( Level.SEVERE, "", e );
            throw new IOException("Could not retrieve baselines from repository. " + e.getMessage(), e);
        }
        
        /* Load baselines remotely */
        for( Baseline baseline : baselines ) {
        	try {
        		logger.fine( "Loading the baseline " + baseline );
				baseline.load();
			} catch (Exception e) {
				logger.warning( "Could not load the baseline " + baseline.getShortname() + ": " + e.getMessage() );
				/* Maybe it should be removed from the list... In fact, this shouldn't happen */
			}
        }

        return baselines;
    }

}
