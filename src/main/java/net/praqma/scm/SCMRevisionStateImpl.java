package net.praqma.scm;

import hudson.scm.SCMRevisionState;
import net.praqma.debug.Debug;

public class SCMRevisionStateImpl extends SCMRevisionState {
	
	protected static Debug logger = Debug.GetLogger();
	
	private String baseline;
	
	public SCMRevisionStateImpl(){
		super();
		logger.trace_function();
		String baseline = "Vores_baseline"; 
	}

	public String getBaseline() {
		logger.trace_function();
		return baseline;
	}

}
