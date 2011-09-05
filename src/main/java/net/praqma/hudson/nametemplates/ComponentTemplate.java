package net.praqma.hudson.nametemplates;

import net.praqma.hudson.scm.CCUCMState.State;

public class ComponentTemplate extends Template {
	
	@Override
	public String parse( State state, String args ) {
		
		try {
			return state.getBaseline().getComponent().getShortname();
		} catch ( Exception e ) {
			return "unknowncomponent";
		}
	}
}
