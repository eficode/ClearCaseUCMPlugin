package net.praqma.hudson.notifier;

import java.io.Serializable;

import hudson.model.Result;

public class Status implements Serializable
{
	private boolean pLevel = false;
	private boolean recommended = false;
	private boolean tagPersisted = false;
	private boolean tagAvailable = false;
	private Result buildStatus;
	private String errorMsg = "";
	private String buildDescr = "";
	
	private boolean stable = true;
	
	private String log = "";
	
	public Status()
	{
		
	}

	public void setPLevel( boolean pLevel )
	{
		this.pLevel = pLevel;
	}

	public boolean isPLevel()
	{
		return pLevel;
	}
	
	public void addToLog( String l )
	{
		this.log += l;
	}
	
	public String getLog()
	{
		return log;
	}
	
	public void setStable( boolean stable )
	{
		this.stable = stable;
	}
	
	public boolean isStable()
	{
		return this.stable;
	}

	public void setRecommended( boolean recommended )
	{
		this.recommended = recommended;
	}

	public boolean isRecommended()
	{
		return recommended;
	}

	public void setTagPersisted( boolean tagPersisted )
	{
		this.tagPersisted = tagPersisted;
	}

	public boolean isTagPersisted()
	{
		return tagPersisted;
	}

	public void setBuildStatus( Result buildStatus )
	{
		this.buildStatus = buildStatus;
	}

	public Result getBuildStatus()
	{
		return buildStatus;
	}

	public void setTagAvailable( boolean tagAvailable )
	{
		this.tagAvailable = tagAvailable;
	}

	public boolean isTagAvailable()
	{
		return tagAvailable;
	}

	public void setBuildDescr(String buildDescr) {
		this.buildDescr = buildDescr;
	}

	public String getBuildDescr() {
		return buildDescr;
	}

	
}
