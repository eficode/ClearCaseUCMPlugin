package net.praqma.hudson.nametemplates;

import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.hudson.exception.TemplateException;
import net.praqma.hudson.remoting.RemoteUtil;
import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.util.debug.Logger;

public class ClearCaseVersionNumberTemplate extends Template {

	private static Logger logger = Logger.getLogger();
	
	@Override
	public String parse( State state, String args ) throws TemplateException {

		try {
			logger.debug( "STREAM: " + state.getStream() );
			logger.debug( "PROJECT: " + state.getStream().getProject() );
			Project project = state.getStream().getProject();
			return rutil.getClearCaseVersion( state.getWorkspace(), project );
		} catch( Exception e ) {
			logger.warning( "Getting cc version error: " + e.getMessage() );
			logger.warning( e );
			return "unknownccversion";
		}
	}

}
