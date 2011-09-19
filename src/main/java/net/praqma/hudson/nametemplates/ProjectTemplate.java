package net.praqma.hudson.nametemplates;

import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.hudson.remoting.RemoteUtil;
import net.praqma.hudson.scm.CCUCMState.State;

public class ProjectTemplate extends Template {
	
	@Override
	public String parse( State state, String args ) {
		
		try {
			Baseline bl = null;
			if( !state.getBaseline().isLoaded() ) {
				bl = (Baseline) RemoteUtil.loadEntity( state.getWorkspace(), state.getBaseline() );
			} else {
				bl = state.getBaseline();
			}
			
			Stream st = null;
			if( !bl.getStream().isLoaded() ) {
				st = (Stream) RemoteUtil.loadEntity( state.getWorkspace(), state.getBaseline().getStream() );
			} else {
				st = bl.getStream();
			}
			
			return st.getProject().getShortname();
		} catch ( Exception e ) {
			return "unknownproject";
		}
	}
}
