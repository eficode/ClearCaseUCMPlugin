package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import java.io.File;
import java.util.logging.Logger;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.exception.TemplateException;
import net.praqma.hudson.remoting.RemoteUtil;

public class ClearCaseVersionNumberTemplate extends Template {

	private static Logger logger = Logger.getLogger( ClearCaseVersionNumberTemplate.class.getName() );
	
	@Override
	public String parse( CCUCMBuildAction action, String args ) throws TemplateException {

		try {
			logger.fine( "STREAM: " + action.getStream() );
			logger.fine( "PROJECT: " + action.getStream().getProject() );
			Project project = action.getStream().getProject();
			return RemoteUtil.getClearCaseVersion( new FilePath(new File(action.getWorkspace())), project );
		} catch( Exception e ) {
			logger.warning( "Getting cc version error: " + e.getMessage() );
			return "unknownccversion";
		}
	}

}
