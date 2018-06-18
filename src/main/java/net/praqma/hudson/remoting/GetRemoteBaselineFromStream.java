package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.*;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.utils.BaselineList;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import net.praqma.clearcase.ucm.utils.filters.AfterDate;
import net.praqma.clearcase.ucm.utils.filters.NoDeliver;
import net.praqma.clearcase.ucm.utils.filters.NoLabels;
import org.jenkinsci.remoting.RoleChecker;

@SuppressFBWarnings("")
public class GetRemoteBaselineFromStream implements FileCallable<BaselineList> {

	private static final long serialVersionUID = -8984877325832486334L;
    private static final Logger logger = Logger.getLogger(GetRemoteBaselineFromStream.class.getName());
	private final Component component;
	private final Stream stream;
	private final Project.PromotionLevel plevel;
	private final boolean multisitePolling;
    private final Date date;

	public GetRemoteBaselineFromStream( Component component, Stream stream, Project.PromotionLevel plevel, boolean multisitePolling, Date date ) {
		this.component = component;
		this.stream = stream;
		this.plevel = plevel;
		this.multisitePolling = multisitePolling;
        this.date = date;
	}
    
    @Override
    public BaselineList invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {

    	logger.fine( "Retrieving remote baselines from " + stream.getShortname() );
    	
        /* The baseline list */
        BaselineList baselines = null;

        baselines = new BaselineList( stream, component, plevel, multisitePolling ).
                setSorting( new BaselineList.AscendingDateSort() ).
                addFilter( new NoLabels()).
                addFilter( new NoDeliver() );
        
        /* Only filter by date, if it is valid */
        if( date != null ) {
            logger.fine("Adding AfterDate filter");
            baselines.addFilter( new AfterDate( date ) );
        }

        try {
            logger.fine("Applying baselines");            
            baselines.apply();
            logger.fine("Loaded BaselineList");
            logger.fine(String.format("Loaded BaselineList contains %s elements", baselines.size()));            
            
        } catch( Exception e ) {
            logger.warning(String.format("Caught exception in GetRemoteBaselineFromStream: %s", e));
            throw new IOException( "Unable get list of Baselines", e );
        }

        return baselines;
    }

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {

    }
}
