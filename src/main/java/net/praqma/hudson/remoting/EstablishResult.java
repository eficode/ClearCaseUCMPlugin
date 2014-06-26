package net.praqma.hudson.remoting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.view.SnapshotView;

public class EstablishResult implements Serializable {
	
	private String viewtag = "";
	private String log = "";	
	private List<Activity> activities = new ArrayList<Activity>();
	private SnapshotView view;
	private String message;
	
	public EstablishResult() { }
	
	public EstablishResult( String viewtag ) {
		this.viewtag = viewtag;
	}
	
	public String getViewtag() {
		return this.viewtag;
	}
	
	public void setViewtag( String viewtag ) {
		this.viewtag = viewtag;
	}

	public String getLog() {
		return log;
	}

	public void setLog( String log ) {
		this.log = log;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage( String message ) {
		this.message = message;
	}

    public List<Activity> getActivities() {
        return activities;
    }

    public void setActivities( List<Activity> activities ) {
        this.activities = activities;
    }

    public SnapshotView getView() {
		return view;
	}

	public void setView( SnapshotView view ) {
		this.view = view;
	}
	

}
