package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;

import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.utils.BuildNumber;
import net.praqma.util.debug.Logger;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class GetClearCaseVersion implements FileCallable<String> {

	private static final Logger logger = Logger.getLogger();

	private final Project project;

	public GetClearCaseVersion( Project project ) {
		this.project = project;
	}

	@Override
	public String invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {

		String version = "";

		try {
			version = BuildNumber.getBuildNumber( project );
		} catch( Exception e ) {
			logger.warning( "get clearcase version: " + e.getMessage() );
			throw new IOException( "Unable to load " + project.getShortname() + ":" + e.getMessage(), e );
		}

		return version;
	}

}
