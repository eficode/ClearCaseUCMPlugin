package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.hudson.scm.ClearCaseChangeset;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

public class RemoteDeliverComplete implements FileCallable<Boolean> {

	private static final long serialVersionUID = 2506984544940354996L;

	private boolean complete;
	private BuildListener listener;

	private Baseline baseline;
	private Stream stream;
	private SnapshotView view;
	private ClearCaseChangeset changeset;

	public RemoteDeliverComplete( Baseline baseline, Stream stream, SnapshotView view, ClearCaseChangeset changeset, boolean complete, BuildListener listener ) {
		this.complete = complete;
		this.listener = listener;

		this.baseline = baseline;
		this.stream = stream;
		this.view = view;
		this.changeset = changeset;
	}

	@Override
	public Boolean invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {

		PrintStream out = listener.getLogger();

    	if( complete ) {

			try {
				baseline.deliver( baseline.getStream(), stream, view.getViewRoot(), view.getViewtag(), true, true, true );
			} catch (UCMException ex) {

				try {
					baseline.cancel( view.getViewRoot() );
				} catch (UCMException ex1) {
					throw new IOException( "Completing the deliver failed. Could not cancel." );
				}
				throw new IOException( "Completing the deliver failed. Deliver was cancelled." );
			}

		} else {
			out.println( "Cancelling" );
			try {
				baseline.cancel( view.getViewRoot() );
			} catch (UCMException ex) {
				throw new IOException( "Could not cancel the deliver." );
			}
		}
    	
		return true;
	}

}
