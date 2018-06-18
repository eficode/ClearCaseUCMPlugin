package net.praqma.hudson.remoting;

import edu.umd.cs.findbugs.annotations.*;
import net.praqma.clearcase.cleartool.Cleartool;
import net.praqma.util.execute.AbnormalProcessTerminationException;

import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

@SuppressFBWarnings("")
public class RemoteClearCaseCheck implements Callable<Boolean, AbnormalProcessTerminationException> {

	@Override
	public Boolean call() throws AbnormalProcessTerminationException {

		Cleartool.run( "lsvob" );
		
		return null;
	}

	@Override
	public void checkRoles(RoleChecker roleChecker) throws SecurityException {

	}
}
