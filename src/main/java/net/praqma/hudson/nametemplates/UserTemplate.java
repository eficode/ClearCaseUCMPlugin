package net.praqma.hudson.nametemplates;

import net.praqma.hudson.scm.CCUCMState.State;

public class UserTemplate extends Template {
	
	@Override
	public String parse( State state, String args ) {
		
		try {
			return state.getBaseline().getUser();
		} catch ( Exception e ) {
			return "unknownstream";
		}
	}
}
