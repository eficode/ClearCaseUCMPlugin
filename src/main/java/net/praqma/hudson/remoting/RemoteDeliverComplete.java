package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

import net.praqma.clearcase.Deliver;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;

public class RemoteDeliverComplete implements FileCallable<Boolean> {

	private static final long serialVersionUID = 2506984544940354996L;

	private final boolean complete;
	private final BuildListener listener;

	private final Baseline baseline;
	private final Stream stream;
	
	private final String viewtag;
	private final File viewPath;

	public RemoteDeliverComplete( Baseline baseline, Stream stream, String viewtag, File viewPath, boolean complete, BuildListener listener ) {
		this.complete = complete;
		this.listener = listener;
		this.baseline = baseline;
		this.stream = stream;
		this.viewtag = viewtag;
		this.viewPath = viewPath;
	}

	@Override
	public Boolean invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {

		PrintStream out = listener.getLogger();

		Logger logger = Logger.getLogger( RemoteDeliverComplete.class.getName() );

		logger.fine( "Remote deliver complete" );
		
		Deliver deliver = new Deliver( baseline, baseline.getStream(), stream, viewPath, viewtag );
		if( complete ) {
			try {
				deliver.complete();
			} catch( Exception ex ) {
				try {
					deliver.cancel();
				} catch( Exception ex1 ) {
					throw new IOException( "Completing the deliver failed. Could not cancel.", ex1 );
				}
				throw new IOException( "Completing the deliver failed. Deliver was cancelled.", ex );
			}

		} else {
			out.println( "Cancelling" );
			try {				
				deliver.cancel();
			} catch( Exception ex ) {
				throw new IOException( "Could not cancel the deliver.", ex );
			}
		}

		return true;
	}

	@Override
	public void checkRoles(RoleChecker roleChecker) throws SecurityException {

	}
}
