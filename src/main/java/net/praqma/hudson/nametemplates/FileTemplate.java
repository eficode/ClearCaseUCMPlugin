package net.praqma.hudson.nametemplates;

import hudson.FilePath;

import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.util.debug.Logger;

public class FileTemplate extends Template {
	
	private Logger logger = Logger.getLogger();

	@Override
	public String parse( State state, String filename ) {
		try {
			FilePath file = new FilePath( state.getWorkspace(), filename );
			logger.debug( "FILE: " + file );
			return file.readToString().trim();
		} catch( Exception e ) {
			
			logger.debug( "E: " + e.getMessage() );
			return "?";
		}
	}

}
