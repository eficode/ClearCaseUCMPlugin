package net.praqma.hudson;

import net.praqma.hudson.scm.CCUCMScm;
import net.praqma.hudson.scm.CCUCMState.State;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import hudson.scm.SCM;

@Extension
public class CCUCMRunListener extends RunListener<Run> {

	public CCUCMRunListener() {
		super( Run.class );
	}
	
	@Override
	public void onFinalized( Run r ) {
		AbstractBuild<?, ?> build = null;
		SCM scm = null;
		
		if( r instanceof AbstractBuild ) {
			try {
				/* I don't even know if this works... */
				build = (AbstractBuild<?, ?>)r;
				scm = build.getProject().getScm();
			} catch( Exception e ) {
				System.out.println( "Ok, that didn't work..." );
				return;
			}
			
			if( scm instanceof CCUCMScm ) {
				String jobName = build.getParent().getDisplayName().replace( ' ', '_' );
				int jobNumber = build.getNumber();
				State state = CCUCMScm.ccucm.getState( jobName, jobNumber );
				
				state.remove();
			}
		}
	}

}
