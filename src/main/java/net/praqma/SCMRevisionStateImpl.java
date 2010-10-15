package net.praqma;

import hudson.scm.SCMRevisionState;

public class SCMRevisionStateImpl extends SCMRevisionState {
	
	//This class is - so far - only to test hudson and CC4HClass.compareRemoteRevisionWith
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
