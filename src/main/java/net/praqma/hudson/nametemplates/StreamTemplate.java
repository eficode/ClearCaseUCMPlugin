package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import net.praqma.hudson.CCUCMBuildAction;

public class StreamTemplate extends Template {
	
	@Override
	public String parse( CCUCMBuildAction action, String args, FilePath ws  ) {
		
		try {
			return action.getStream().getShortname();
		} catch ( Exception e ) {
			return "unknownstream";
		}
	}
}
