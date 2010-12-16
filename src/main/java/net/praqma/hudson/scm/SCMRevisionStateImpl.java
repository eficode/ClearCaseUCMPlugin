package net.praqma.hudson.scm;

import hudson.scm.SCMRevisionState;
import net.praqma.util.Debug;

/**
 * 
 * @author Troels Selch Sørensen
 * @author Margit Bennetzen
 * 
 */
public class SCMRevisionStateImpl extends SCMRevisionState
{

	protected static Debug logger = Debug.GetLogger();

	public SCMRevisionStateImpl()
	{
		super();
		logger.trace_function();
	}
}
