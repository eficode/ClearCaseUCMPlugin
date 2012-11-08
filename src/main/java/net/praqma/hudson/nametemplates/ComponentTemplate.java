package net.praqma.hudson.nametemplates;

import net.praqma.hudson.CCUCMBuildAction;

public class ComponentTemplate extends Template {
	
	@Override
	public String parse( CCUCMBuildAction action, String args ) {
		
		try {
			return action.getBaseline().getComponent().getShortname();
		} catch ( Exception e ) {
			return "unknowncomponent";
		}
	}
}
