package net.praqma.hudson.scm;

import hudson.scm.SCMRevisionState;
import net.praqma.util.debug.PraqmaLogger;
import net.praqma.util.debug.PraqmaLogger.Logger;

/**
 * 
 * @author Troels Selch Sørensen
 * @author Margit Bennetzen
 * 
 */
public class SCMRevisionStateImpl extends SCMRevisionState
{

	protected static Logger logger = PraqmaLogger.getLogger();

	public SCMRevisionStateImpl()
	{
		super();
		logger.trace_function();
	}
}
