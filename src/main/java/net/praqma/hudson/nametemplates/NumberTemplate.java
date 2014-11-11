package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import net.praqma.hudson.CCUCMBuildAction;

public class NumberTemplate extends Template {

	@Override
	public String parse( CCUCMBuildAction action, String args, FilePath ws ) {
		try {
			return action.getBuild().getNumber()+"";
		} catch( NullPointerException e ) {
			return "?";
		}
	}

}
