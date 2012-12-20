package net.praqma.hudson.remoting;

import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Logger;

import net.praqma.clearcase.ucm.entities.Stream;

public class GetRelatedStreams implements FileCallable<List<Stream>> {

	private static final long serialVersionUID = -8984877325832486334L;

	private Stream stream;
	private boolean pollingChildStreams;
	private TaskListener listener;
	private boolean multisitePolling;

	public GetRelatedStreams( TaskListener listener, Stream stream, boolean pollingChildStreams, boolean multisitePolling ) {
		this.stream = stream;
		this.pollingChildStreams = pollingChildStreams;
		this.listener = listener;
		this.multisitePolling = multisitePolling;
	}

	@Override
	public List<Stream> invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {

		PrintStream out = listener.getLogger();
		Logger logger = Logger.getLogger( GetRelatedStreams.class.getName() );

		List<Stream> streams = null;

		try {
			if( pollingChildStreams ) {
				streams = stream.getChildStreams( multisitePolling );
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
