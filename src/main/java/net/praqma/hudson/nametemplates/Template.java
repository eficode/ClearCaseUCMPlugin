package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import java.io.Serializable;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.exception.TemplateException;

public abstract class Template implements Serializable {
	public abstract String parse( CCUCMBuildAction action, String args, FilePath ws ) throws TemplateException;
}