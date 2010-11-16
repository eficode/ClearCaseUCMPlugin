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
	private String buildno;

	public SCMRevisionStateImpl(String jobname, String buildno) {
		super();
		logger.trace_function();
		this.jobname = jobname;
		this.buildno = buildno;
	}

	public String getJobname() {
		logger.trace_function();
		return jobname;
	}

	public String getJobno() {
		logger.trace_function();
		return buildno;
	}
}
