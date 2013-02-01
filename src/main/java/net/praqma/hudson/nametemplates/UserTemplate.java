package net.praqma.hudson.nametemplates;

import net.praqma.hudson.CCUCMBuildAction;

public class UserTemplate extends Template {
	
	@Override
	public String parse( CCUCMBuildAction action, String args ) {
		
		try {
			return action.getBaseline().getUser();
		} catch ( Exception e ) {
			return "unknownstream";
		}
	}
}
