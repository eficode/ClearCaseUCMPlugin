package net.praqma.hudson.nametemplates;

import hudson.EnvVars;
import hudson.FilePath;

import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.exception.TemplateException;

import java.util.logging.Logger;

public class EnvTemplate extends Template {

	private static final Logger logger = Logger.getLogger( EnvTemplate.class.getName() );
	
	@Override
	public String parse( CCUCMBuildAction action, String args, FilePath ws ) throws TemplateException {
		logger.finest( "ENV PARSING" );
		EnvVars vars = null;
		try {
			vars = action.getBuild().getEnvironment( action.getListener() );
		} catch( Exception e ) {
			logger.warning( "I could not get env vars: " + e.getMessage() );
			return "?";
		}
		if( vars.containsKey( args ) ) {			
			return vars.get( args );
		} else if( action.getBuild().getBuildVariables().containsKey( args ) ) {			
			return action.getBuild().getBuildVariables().get( args );
		} else if( System.getenv().containsKey( args ) ) {
			return vars.get( args );
		} else {
			return "?";
		}
	}

}
