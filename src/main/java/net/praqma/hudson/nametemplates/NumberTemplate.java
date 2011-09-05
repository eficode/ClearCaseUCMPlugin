package net.praqma.hudson.nametemplates;

import net.praqma.hudson.scm.CCUCMState.State;

public class NumberTemplate extends Template {

	@Override
	public String parse( State state, String args ) {
		try {
			return state.getJobNumber().toString();
		} catch( NullPointerException e ) {
			return "?";
		}
	}

}
