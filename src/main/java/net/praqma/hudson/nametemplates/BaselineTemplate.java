package net.praqma.hudson.nametemplates;

import net.praqma.hudson.scm.CCUCMState.State;

public class BaselineTemplate extends Template {
	
	@Override
	public String parse( State state, String args ) {
		
		try {
			return state.getBaseline().getShortname();
		} catch ( Exception e ) {
			return "unknownbaseline";
		}
	}
}
