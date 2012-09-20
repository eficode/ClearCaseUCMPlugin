package net.praqma.hudson.nametemplates;

import hudson.FilePath;

import net.praqma.hudson.scm.CCUCMState.State;

import java.util.logging.Logger;

public class FileTemplate extends Template {
	
	private Logger logger = Logger.getLogger( FileTemplate.class.getName() );

	@Override
	public String parse( State state, String filename ) {
		try {
			FilePath file = new FilePath( state.getWorkspace(), filename );
			logger.fine( "FILE: " + file );
			return file.readToString().trim();
		} catch( Exception e ) {
			
			logger.fine( "E: " + e.getMessage() );
			return "?";
		}
	}

}
