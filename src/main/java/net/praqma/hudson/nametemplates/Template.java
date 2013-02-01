package net.praqma.hudson.nametemplates;

import java.io.Serializable;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.exception.TemplateException;

public abstract class Template implements Serializable {
	public abstract String parse( CCUCMBuildAction action, String args ) throws TemplateException;
}