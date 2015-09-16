package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;

import net.praqma.clearcase.ucm.view.SnapshotView;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;

public class EndView implements FileCallable<Boolean> {

	private static final long serialVersionUID = -8984877325832486334L;

	private String viewtag;

	public EndView( String viewtag ) {
		this.viewtag = viewtag;
	}

	@Override
	public Boolean invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {

		try {
			SnapshotView.end( viewtag );
		} catch( Exception e ) {
			throw new IOException( "Unable to end the view " + viewtag + ": " + e.getMessage(), e );
		}

		return true;
	}

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        
    }

}
