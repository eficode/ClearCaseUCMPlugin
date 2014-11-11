package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.remoting.RemoteUtil;

public class ProjectTemplate extends Template {    
    private static final Logger logger = Logger.getLogger(ProjectTemplate.class.getName());
	
	@Override
	public String parse( CCUCMBuildAction action, String args, FilePath ws  ) {		
		try {
			Baseline bl = null;
			if( !action.getBaseline().isLoaded() ) {
				bl = (Baseline) RemoteUtil.loadEntity(ws, action.getBaseline(), true );
			} else {
				bl = action.getBaseline();
			}
			
			Stream st = null;
			if( !bl.getStream().isLoaded() ) {
				st = (Stream) RemoteUtil.loadEntity(ws, action.getBaseline().getStream(), true );
			} else {
				st = bl.getStream();
			}
			
			return st.getProject().getShortname();
		} catch ( Exception e ) {
            logger.log(Level.SEVERE, "Failed to correctly get project name for template", e);
			return "unknownproject";
		}
	}
}
