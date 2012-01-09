package net.praqma.hudson.nametemplates;

import hudson.EnvVars;

import net.praqma.hudson.exception.TemplateException;
import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.util.debug.Logger;

public class EnvTemplate extends Template {

	private Logger logger = Logger.getLogger();
	
	@Override
	public String parse( State state, String args ) throws TemplateException {
		logger.debug( "ENV PARSING" );
		EnvVars vars = null;
		try {
			vars = state.getBuild().getEnvironment( state.getListener() );
		} catch( Exception e ) {
			logger.warning( "I could not get env vars: " + e.getMessage() );
			return "?";
		}
		
		logger.debug( "ENV VARS: " + vars );
		logger.debug( "ENV VARS: " + System.getenv() );
		
		if( vars.containsKey( args ) ) {
			logger.debug( "EnvVars: " + args + "=" + vars.get( args ) );
			return vars.get( args );
		} else if( state.getBuild().getBuildVariables().containsKey( args ) ) {
			logger.debug( "BuildVars: " + args + "=" + state.getBuild().getBuildVariables().get( args ) );
			return state.getBuild().getBuildVariables().get( args );
		} else if( System.getenv().containsKey( args ) ) {
			logger.debug( "Vars: " + args + "=" + System.getenv( args ) );
			return vars.get( args );
		} else {
			return "?";
		}
	}

}
