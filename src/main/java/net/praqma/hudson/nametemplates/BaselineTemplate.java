package net.praqma.hudson.nametemplates;

import net.praqma.hudson.CCUCMBuildAction;

public class BaselineTemplate extends Template {
	
	@Override
	public String parse( CCUCMBuildAction action, String args ) {
		
		try {
			return action.getBaseline().getShortname();
		} catch ( Exception e ) {
			return "unknownbaseline";
		}
	}
}
