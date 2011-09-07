package net.praqma.hudson.nametemplates;

import net.praqma.hudson.scm.CCUCMState.State;

public class ProjectTemplate extends Template {
	
	@Override
	public String parse( State state, String args ) {
		
		try {
			return state.getBaseline().getStream().getProject().getShortname();
		} catch ( Exception e ) {
			return "unknownproject";
		}
	}
}
