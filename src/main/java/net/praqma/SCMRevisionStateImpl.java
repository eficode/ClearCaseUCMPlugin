package net.praqma;

import hudson.scm.SCMRevisionState;

public class SCMRevisionStateImpl extends SCMRevisionState {
	
	//This class is - so far - only to test hudson and CC4HClass.compareRemoteRevisionWith
	
	private String baseline;
	
	public SCMRevisionStateImpl(){
		super();
		String baseline = "Vores_baseline"; 
	}

	public String getBaseline() {
		return baseline;
	}

}
