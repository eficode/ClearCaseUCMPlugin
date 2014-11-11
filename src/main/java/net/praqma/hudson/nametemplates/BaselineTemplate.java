package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import net.praqma.hudson.CCUCMBuildAction;

public class BaselineTemplate extends Template {
	
	@Override
	public String parse( CCUCMBuildAction action, String args, FilePath ws ) {		
		try {
			return action.getBaseline().getShortname();
		} catch ( Exception e ) {
			return "unknownbaseline";
		}
	}
}
