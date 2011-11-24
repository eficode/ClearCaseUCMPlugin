package net.praqma.hudson.nametemplates;

import net.praqma.hudson.exception.TemplateException;
import net.praqma.hudson.remoting.RemoteUtil;
import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.Logger.LogLevel;

public abstract class Template {
	protected static RemoteUtil rutil = new RemoteUtil( Logger.getLoggerSettings( LogLevel.INFO ) );
	public abstract String parse( State state, String args ) throws TemplateException;
}