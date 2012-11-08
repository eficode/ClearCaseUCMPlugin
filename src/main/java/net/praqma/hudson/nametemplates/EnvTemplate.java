package net.praqma.hudson.nametemplates;

import hudson.EnvVars;

import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.exception.TemplateException;

import java.util.logging.Logger;

public class EnvTemplate extends Template {

	private Logger logger = Logger.getLogger( EnvTemplate.class.getName() );
	
	@Override
	public String parse( CCUCMBuildAction action, String args ) throws TemplateException {
		logger.finest( "ENV PARSING" );
		EnvVars vars = null;
		try {
			vars = action.getBuild().getEnvironment( action.getListener() );
		} catch( Exception e ) {
			logger.warning( "I could not get env vars: " + e.getMessage() );
			return "?";
		}
		
		logger.finest( "ENV VARS: " + vars );
		logger.finest( "ENV VARS: " + System.getenv() );
		
		if( vars.containsKey( args ) ) {
			logger.finest( "EnvVars: " + args + "=" + vars.get( args ) );
			return vars.get( args );
		} else if( action.getBuild().getBuildVariables().containsKey( args ) ) {
			logger.finest( "BuildVars: " + args + "=" + action.getBuild().getBuildVariables().get( args ) );
			return action.getBuild().getBuildVariables().get( args );
		} else if( System.getenv().containsKey( args ) ) {
			logger.finest( "Vars: " + args + "=" + System.getenv( args ) );
			return vars.get( args );
		} else {
			return "?";
		}
	}

}
