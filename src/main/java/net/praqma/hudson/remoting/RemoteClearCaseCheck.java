package net.praqma.hudson.remoting;

import net.praqma.clearcase.cleartool.Cleartool;
import net.praqma.util.execute.AbnormalProcessTerminationException;

import hudson.remoting.Callable;

public class RemoteClearCaseCheck implements Callable<Boolean, AbnormalProcessTerminationException> {

	@Override
	public Boolean call() throws AbnormalProcessTerminationException {
		
		Cleartool.run( "lsvob" );
		
		return null;
	}

}
