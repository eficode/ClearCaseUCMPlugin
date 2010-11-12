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
	
	private String jobname;

	
	public SCMRevisionStateImpl(String jobname){
		super();
		logger.trace_function();
		this.jobname = jobname;
	}

	public String getJobname() {
		logger.trace_function();
		return jobname;
	}

}
