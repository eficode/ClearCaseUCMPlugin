package net.praqma.hudson.scm;

import hudson.scm.SCMRevisionState;
import net.praqma.debug.Debug;

/**
 * 
 * @author Troels Selch Sørensen
 * @author Margit Bennetzen
 *
 */
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
