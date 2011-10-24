package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.entities.Project.Plevel;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.utils.BaselineList;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class GetRemoteBaselineFromStream implements FileCallable<List<Baseline>> {

	private static final long serialVersionUID = -8984877325832486334L;

	private Component component;
	private Stream stream;
	private Plevel plevel;

	public GetRemoteBaselineFromStream( Component component, Stream stream, Plevel plevel ) {
		this.component = component;
		this.stream = stream;
		this.plevel = plevel;
    }

    @Override
    public List<Baseline> invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
    	
    	Logger logger = Logger.getLogger( GetRemoteBaselineFromStream.class.getName() );

    	UCM.setContext( UCM.ContextType.CLEARTOOL );

    	logger.info( "Retrieving remote baselines from " + stream.getShortname() );

        /* The baseline list */
        BaselineList baselines = null;

        try {
            baselines = component.getBaselines( stream, plevel );
        } catch (UCMException e) {
            throw new IOException("Could not retrieve baselines from repository. " + e.getMessage());
        }

        /* Load baselines remotely */
        for( Baseline baseline : baselines ) {
        	try {
				baseline.load();
			} catch (UCMException e) {
				logger.warning( "Could not load the baseline " + baseline.getShortname() + ": " + e.getMessage() );
				/* Maybe it should be removed from the list... In fact, this shouldn't happen */
			}
        }

        return baselines;
    }

}
