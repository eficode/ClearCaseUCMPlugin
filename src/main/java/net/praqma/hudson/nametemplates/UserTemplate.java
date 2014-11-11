package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import net.praqma.hudson.CCUCMBuildAction;

public class UserTemplate extends Template {
	
	@Override
	public String parse( CCUCMBuildAction action, String args, FilePath ws  ) {
		
		try {
			return action.getBaseline().getUser();
		} catch ( Exception e ) {
			return "unknownuser";
		}
	}
}
