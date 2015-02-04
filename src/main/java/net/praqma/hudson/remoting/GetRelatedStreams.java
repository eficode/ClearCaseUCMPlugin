package net.praqma.hudson.remoting;

import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.hudson.scm.Polling;
import net.praqma.hudson.scm.Polling.PollingType;

public class GetRelatedStreams implements FileCallable<List<Stream>> {

	private static final long serialVersionUID = -8984877325832486334L;
	private final Stream stream;
    
    @Deprecated
	private final boolean pollingChildStreams;
    
	private final TaskListener listener;
	private final boolean multisitePolling;
    private final Polling polling;
    private final String hyperLinkName;

    @Deprecated
	public GetRelatedStreams( TaskListener listener, Stream stream, boolean pollingChildStreams, boolean multisitePolling ) {
		this.stream = stream;
		this.pollingChildStreams = pollingChildStreams;
		this.listener = listener;
		this.multisitePolling = multisitePolling;
        this.polling = null;
        this.hyperLinkName = "AlternateDeliverTarget";
	}
    
    public GetRelatedStreams( TaskListener listener, Stream stream, Polling polling, boolean multisitePolling, String hyperLinkName ) {
		this.stream = stream;
		this.pollingChildStreams = polling.isPollingChilds();
		this.listener = listener;
		this.multisitePolling = multisitePolling;
        this.polling = polling;
        this.hyperLinkName = hyperLinkName;
	}

	@Override
	public List<Stream> invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
		PrintStream out = listener.getLogger();
		
		List<Stream> streams = null;

		try {
			if( pollingChildStreams ) {
				streams = stream.getChildStreams( multisitePolling );
            } else if(polling.getType().equals(PollingType.siblingshlink)) {
                streams = stream.getDeliveringStreamsUsingHlink(hyperLinkName);
			} else {
				streams = stream.getSiblingStreams();
			}
		} catch( Exception e1 ) {
			e1.printStackTrace( out );
            
			throw new IOException( "Could not find any related streams: " + e1.getMessage() );
		}

		return streams;
	}
}
