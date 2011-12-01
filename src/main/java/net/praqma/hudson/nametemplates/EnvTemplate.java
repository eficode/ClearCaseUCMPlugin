package net.praqma.hudson.nametemplates;

import hudson.EnvVars;

import net.praqma.hudson.exception.TemplateException;
import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.util.debug.Logger;

public class EnvTemplate extends Template {

	private Logger logger = Logger.getLogger();
	
	@Override
	public String parse( State state, String args ) throws TemplateException {
		EnvVars vars = null;
		try {
			vars = state.getBuild().getEnvironment( state.getListener() );
		} catch( Exception e ) {
			logger.warning( "I could not get env vars: " + e.getMessage() );
			return "?";
		}
		if( vars.containsKey( args ) ) {
			logger.debug( args + "=" + vars.get( args ) );
			return vars.get( args );
		} else if( state.getBuild().getBuildVariables().containsKey( args ) ) {
			logger.debug( args + "=" + state.getBuild().getBuildVariables().get( args ) );
			return state.getBuild().getBuildVariables().get( args );
		} else {
			return "?";
		}
	}

}
