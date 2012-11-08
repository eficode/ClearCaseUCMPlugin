package net.praqma.hudson.nametemplates;

import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.exception.TemplateException;
import net.praqma.hudson.scm.CCUCMState.State;

public abstract class Template {
	public abstract String parse( CCUCMBuildAction action, String args ) throws TemplateException;
}