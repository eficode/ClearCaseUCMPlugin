package net.praqma.hudson.notifier;

import java.io.Serializable;

public class UCMDeliver implements Serializable
{
	/* Determines whether to deliver or not */
	public boolean ucmDeliver = false;
	
	
	
	/* If defined, this alternate stream is target. if not default target is used */
	public String alternateTarget;
	/* If defined, a baseline is created on the target stream */
	public String baselineName;
	
	/* If true, a four level version number is appended to the baseline name */
	//public boolean apply4level = false;
	
	/* Determines how the version number is retrieved. Can be "project" or "settings" */
	public String versionFrom;
	
	/* Can be "component" or "current" */
	public String buildnumberSequenceSelector;
	
	/* If settings is selected, these version numbers are used */
	public String buildnumberMajor;
	public String buildnumberMinor;
	public String buildnumberPatch;
	/* This is only used if component is chosen */
	public String buildnumberSequence;
	
}
