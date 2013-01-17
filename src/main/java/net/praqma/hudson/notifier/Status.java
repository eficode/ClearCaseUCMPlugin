package net.praqma.hudson.notifier;

import hudson.model.Result;
import java.io.Serializable;
import net.praqma.clearcase.ucm.entities.Project;

public class Status implements Serializable {
	private static final long serialVersionUID = 1113020858633109523L;
	private boolean recommended = true;
	private boolean tagPersisted = false;
	private boolean tagAvailable = false;
	private Result buildStatus;
	private String errorMessage = "";
	private String buildDescr = "";

	private Project.PromotionLevel promotedLevel = null;

	private boolean stable = true;

	public Status() {

	}

	public void setStable( boolean stable ) {
		this.stable = stable;
	}

	public boolean isStable() {
		return this.stable;
	}

	public void setRecommended( boolean recommended ) {
		this.recommended = recommended;
	}

	public boolean isRecommended() {
		return recommended;
	}

	public void setTagPersisted( boolean tagPersisted ) {
		this.tagPersisted = tagPersisted;
	}

	public boolean isTagPersisted() {
		return tagPersisted;
	}

	public void setBuildStatus( Result buildStatus ) {
		this.buildStatus = buildStatus;
	}

	public Result getBuildStatus() {
		return buildStatus;
	}

	public void setTagAvailable( boolean tagAvailable ) {
		this.tagAvailable = tagAvailable;
	}

	public boolean isTagAvailable() {
		return tagAvailable;
	}

	public void setBuildDescr( String buildDescr ) {
		this.buildDescr = buildDescr;
	}

	public String getBuildDescr() {
		return buildDescr;
	}

	public void setPromotedLevel( Project.PromotionLevel promotedLevel ) {
		this.promotedLevel = promotedLevel;
	}

	public Project.PromotionLevel getPromotedLevel() {
		return promotedLevel;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage( String errorMessage ) {
		this.errorMessage = errorMessage;
	}

}
