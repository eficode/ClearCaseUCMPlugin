package net.praqma.hudson.nametemplates;

import net.praqma.hudson.CCUCMBuildAction;

public class NumberTemplate extends Template {

	@Override
	public String parse( CCUCMBuildAction action, String args ) {
		try {
			return action.getBuild().getNumber()+"";
		} catch( NullPointerException e ) {
			return "?";
		}
	}

}
