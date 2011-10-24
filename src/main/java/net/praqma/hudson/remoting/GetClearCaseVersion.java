package net.praqma.hudson.remoting;

import java.io.File;
import java.io.IOException;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.utils.BuildNumber;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class GetClearCaseVersion implements FileCallable<String> {

	private static final long serialVersionUID = -8984877325832486334L;

	private Project project;
	
	public GetClearCaseVersion( Project project ) {
		this.project = project;
    }
    
    @Override
    public String invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
        
    	String version = "";
    	
    	try {
			version = BuildNumber.getBuildNumber(project);
		} catch (UCMException e) {
        	throw new IOException( "Unable to load " + project.getShortname() + ":" + e.getMessage() );
		}

    	return version;
    }

}
