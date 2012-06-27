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
import java.util.Set;

import net.praqma.clearcase.Cool;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.LoggerSetting;
import net.praqma.util.debug.appenders.StreamAppender;
import net.praqma.util.execute.CommandLine;

public class GetRelatedStreams implements FileCallable<List<Stream>> {

	private static final long serialVersionUID = -8984877325832486334L;

	private Stream stream;
	private boolean pollingChildStreams;
	private TaskListener listener;
	private Pipe pipe;

	private LoggerSetting loggerSetting;
	private PrintStream pstream;
	private boolean multisitePolling;

	public GetRelatedStreams( TaskListener listener, Stream stream, boolean pollingChildStreams, Pipe pipe, PrintStream pstream, LoggerSetting loggerSetting, boolean multisitePolling ) {
		this.stream = stream;
		this.pollingChildStreams = pollingChildStreams;
		this.listener = listener;
		this.pstream = pstream;
		this.pipe = pipe;
		this.loggerSetting = loggerSetting;
		this.multisitePolling = multisitePolling;
	}

	@Override
	public List<Stream> invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {

		PrintStream out = listener.getLogger();
		Logger logger = Logger.getLogger();

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

		List<Stream> streams = null;

		try {
			if( pollingChildStreams ) {
				streams = stream.getChildStreams( multisitePolling );
			} else {
				streams = stream.getSiblingStreams();
			}
		} catch( Exception e1 ) {
			e1.printStackTrace( out );
			Logger.removeAppender( app );

			throw new IOException( "Could not find any related streams: " + e1.getMessage() );
		}

		Logger.removeAppender( app );

		return streams;
	}
}
