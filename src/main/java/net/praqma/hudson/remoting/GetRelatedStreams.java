package net.praqma.hudson.remoting;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import net.praqma.clearcase.Cool;
import net.praqma.clearcase.Cool.ContextType;
import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;

public class GetRelatedStreams implements FileCallable<List<Stream>> {

	private static final long serialVersionUID = -8984877325832486334L;

	private Stream stream;
	private boolean pollingChildStreams;
	private TaskListener listener;
	private Pipe pipe;
	
	public GetRelatedStreams( TaskListener listener, Stream stream, boolean pollingChildStreams, Pipe pipe ) {
		this.stream = stream;
		this.pollingChildStreams = pollingChildStreams;
		this.listener = listener;
		
		this.pipe = pipe;
    }
    
    @Override
    public List<Stream> invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
    	
    	PrintStream out = listener.getLogger();
    	
    	OutputStream logger = pipe.getOut();
    	
    	PrintStream lout = new PrintStream( logger );
    	lout.println("Wolle was here");
    	lout.close();
    	
    	Cool.setContext( ContextType.CLEARTOOL );
    	
    	List<Stream> streams = null;
    	
   	
        try {
        	if( pollingChildStreams ) {
        		streams = stream.getChildStreams();
        	} else {
        		streams = stream.getSiblingStreams();
        	}
        } catch( UCMException e1 ) {
        	e1.printStackTrace( out );
        	throw new IOException( "Could not find any related streams: " + e1.getMessage() );
        }
        
        return streams;
    }
}
