package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import net.praqma.hudson.CCUCMBuildAction;

public class VersionNumberTemplate extends Template {
	
	@Override
	public String parse( CCUCMBuildAction action, String args, FilePath ws  ) {
		
		if( args != null && args.length() > 0 ) {
			String result = "";
			String[] ns = args.split( "," );
			for( String n : ns ) {
				n = n.trim();
				try {
					result += "_" + Integer.parseInt( n );
				} catch( NumberFormatException e ) {
					result += "_?";
				}
			}
			
			return result;
		} else {
			return "unkownversionnumber";
		}
	}
}
