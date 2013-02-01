package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.hudson.CCUCMBuildAction;

public class FileTemplate extends Template {
	
	private static final Logger logger = Logger.getLogger( FileTemplate.class.getName() );

	@Override
	public String parse( CCUCMBuildAction action, String filename ) {
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
    
    public class FileFoundable implements FilePath.FileCallable<String> {
        public final String filename;
        
        public FileFoundable(final String filename) {
            logger.fine("[FileTemplate] FileFoundable created");
            this.filename = filename;
        }

        @Override
        public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            FilePath path = null;
            logger.fine(String.format("In invoke. Operating on slave with workspace path: %s",f.getAbsolutePath()));
            try {
                path  = new FilePath(new FilePath(f), filename);
                String readFile = path.readToString().trim();
                logger.fine(String.format("[FileTemplate] This file was read on the slave: %s", readFile));
                return readFile; 
           } catch (IOException ex) {
                logger.fine(String.format("[FileTemplate] Using this file on remote: %s",path.absolutize().getRemote()));
                logger.fine(String.format("[FileTemplate] Invoke caught exception with message %s", ex.getMessage()));
                throw ex;
            }
        }
        
    }
}
