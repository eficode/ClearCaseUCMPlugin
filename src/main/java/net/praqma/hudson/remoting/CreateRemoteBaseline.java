package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.entities.Component;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

public class CreateRemoteBaseline implements FileCallable<Baseline> {

	private static final long serialVersionUID = -8984877325832486334L;
	private final String baseName;
	private final Component component;
	private final File view;
	private final BuildListener listener;
	private final String username;

	public CreateRemoteBaseline( String baseName, Component component, File view, String username, BuildListener listener ) {
		this.baseName = baseName;
		this.component = component;
		this.view = view;
		this.listener = listener;
		this.username = username;
	}

	@Override
	public Baseline invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
        
		Baseline bl = null;
        
		try {
			bl = Baseline.create( baseName, component, view, LabelBehaviour.INCREMENTAL, false );
		} catch( Exception e ) {
			throw new IOException( "Unable to create Baseline:" + e.getMessage(), e );
		}

		return bl;
	}

}
