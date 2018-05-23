package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;

import net.praqma.clearcase.ucm.entities.UCMEntity;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.remoting.RoleChecker;

public class LoadEntity implements FileCallable<UCMEntity> {

	private static final long serialVersionUID = -8984877325832486334L;

	private final UCMEntity entity;

	public LoadEntity( UCMEntity entity ) {
		this.entity = entity;
	}

	@Override
	public UCMEntity invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {

		try {
			entity.load();
		} catch( Exception e ) {
			throw new IOException( "Unable to load " + entity.getShortname(), e );
		}

		return entity;
	}

	@Override
	public void checkRoles(RoleChecker roleChecker) throws SecurityException {

	}
}
