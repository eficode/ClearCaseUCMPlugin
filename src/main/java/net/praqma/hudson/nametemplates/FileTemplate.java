package net.praqma.hudson.nametemplates;

import edu.umd.cs.findbugs.annotations.*;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.hudson.CCUCMBuildAction;

@SuppressFBWarnings("")
public class FileTemplate extends Template {
	
	private static final Logger logger = Logger.getLogger( FileTemplate.class.getName() );

	@Override
	public String parse( CCUCMBuildAction action, String filename, FilePath ws ) {
		try {
            logger.fine(String.format("[FileTemplate] Parsing FileTemplate"));
            String res = action.getBuild().getExecutor().getCurrentWorkspace().act(new FileFoundable(filename));
            logger.fine(String.format("[FileTemplate] Parse result: %s",res));
            return res;
		} catch( Exception e ) {
            logger.logp(Level.SEVERE, this.getClass().getName(), "parse", "[FileTemplate] Caught exception", e);
			logger.fine( "E: " + e.getMessage() );            
			return "?";
		}
	}
}
