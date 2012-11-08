package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.exceptions.UnableToListBaselinesException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.utils.BaselineList;
import net.praqma.clearcase.ucm.utils.Baselines;
import hudson.FilePath.FileCallable;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;
import net.praqma.clearcase.ucm.utils.filters.AfterDate;
import net.praqma.clearcase.ucm.utils.filters.NoDeliver;

public class GetRemoteBaselineFromStream implements FileCallable<BaselineList> {

	private static final long serialVersionUID = -8984877325832486334L;

	private Component component;
	private Stream stream;
	private Project.PromotionLevel plevel;
	private boolean multisitePolling;
    private Date date;

	public GetRemoteBaselineFromStream( Component component, Stream stream, Project.PromotionLevel plevel, boolean multisitePolling, Date date ) {
		this.component = component;
		this.stream = stream;
		this.plevel = plevel;
		this.multisitePolling = multisitePolling;
        this.date = date;
	}
    
    @Override
    public BaselineList invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
    	
    	Logger logger = Logger.getLogger( GetRemoteBaselineFromStream.class.getName() );

    	
    	logger.fine( "Retrieving remote baselines from " + stream.getShortname() );
    	
        /* The baseline list */
        BaselineList baselines = null;

        baselines = new BaselineList( stream, component, plevel, multisitePolling ).
                setSorting( new BaselineList.AscendingDateSort() ).
                addFilter( new NoDeliver() ).
                load();

        /* Only filter by date, if it is valid */
        if( date != null ) {
            baselines.addFilter( new AfterDate( date ) );
        }

        try {
            baselines.apply();
        } catch( Exception e ) {
            throw new IOException( "Unable get list of Baselines", e );
        }

        return baselines;
    }

}
