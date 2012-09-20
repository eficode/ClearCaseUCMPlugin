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

public class RemoteDeliverComplete implements FileCallable<Boolean> {

	private static final long serialVersionUID = 2506984544940354996L;

	private boolean complete;
	private BuildListener listener;

	private Baseline baseline;
	private Stream stream;
	//private SnapshotView view;
	private String viewtag;
	private File viewPath;

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
				//baseline.deliver( baseline.getStream(), stream, view.getViewRoot(), view.getViewtag(), true, true, true );
			} catch( Exception ex ) {

				try {
					//baseline.cancel( view.getViewRoot() );
					deliver.cancel();
				} catch( Exception ex1 ) {
					throw new IOException( "Completing the deliver failed. Could not cancel.", ex1 );
				}
				throw new IOException( "Completing the deliver failed. Deliver was cancelled.", ex );
			}

		} else {
			out.println( "Cancelling" );
			try {
				//baseline.cancel( view.getViewRoot() );
				deliver.cancel();
			} catch( Exception ex ) {
				throw new IOException( "Could not cancel the deliver.", ex );
			}
		}

		return true;
	}

}
