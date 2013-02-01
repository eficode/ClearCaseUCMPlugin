package net.praqma.hudson.nametemplates;

import net.praqma.hudson.CCUCMBuildAction;

public class StreamTemplate extends Template {
	
	@Override
	public String parse( CCUCMBuildAction action, String args ) {
		
		try {
			return action.getStream().getShortname();
		} catch ( Exception e ) {
			return "unknownstream";
		}
	}
}
