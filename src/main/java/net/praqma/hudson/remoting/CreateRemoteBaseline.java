package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;

import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.entities.Component;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import net.praqma.clearcase.ucm.entities.Stream;
import org.jenkinsci.remoting.RoleChecker;

public class CreateRemoteBaseline implements FileCallable<Baseline> {

	private static final long serialVersionUID = -8984877325832486334L;
	private final String baseName;
	private final Component component;
	private final File view;
    private final Stream stream;

	public CreateRemoteBaseline( String baseName, Component component, File view ) {
		this.baseName = baseName;
		this.component = component;
		this.view = view;
        this.stream = null;
	}
    
    public CreateRemoteBaseline( String baseName, Stream stream, Component component, File view ) {
		this.baseName = baseName;
		this.component = component;
		this.view = view;
        this.stream = stream;
	}

	@Override
	public Baseline invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
        
		Baseline bl = null;        
		try {
            if(stream == null) {
                bl = Baseline.create( baseName, component, view, LabelBehaviour.INCREMENTAL, false );
            } else {
                bl = Baseline.create(stream, component, baseName, view, LabelBehaviour.INCREMENTAL, false);
            }
		} catch( Exception e ) {
			throw new IOException( "Unable to create Baseline:" + e.getMessage(), e );
		}

		return bl;
	}

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        
    }

}
