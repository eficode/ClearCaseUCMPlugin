package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import java.io.File;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.remoting.RemoteUtil;

public class ProjectTemplate extends Template {
	
	@Override
	public String parse( CCUCMBuildAction action, String args ) {
		
		try {
			Baseline bl = null;
			if( !action.getBaseline().isLoaded() ) {
				bl = (Baseline) RemoteUtil.loadEntity( new FilePath(new File(action.getWorkspace())), action.getBaseline(), true );
			} else {
				bl = action.getBaseline();
			}
			
			Stream st = null;
			if( !bl.getStream().isLoaded() ) {
				st = (Stream) RemoteUtil.loadEntity(new FilePath(new File(action.getWorkspace())), action.getBaseline().getStream(), true );
			} else {
				st = bl.getStream();
			}
			
			return st.getProject().getShortname();
		} catch ( Exception e ) {
			return "unknownproject";
		}
	}
}
