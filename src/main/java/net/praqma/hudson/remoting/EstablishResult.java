package net.praqma.hudson.remoting;

import java.io.Serializable;

import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.hudson.scm.ClearCaseChangeset;

public class EstablishResult implements Serializable {

	
	private String viewtag = "";
	private String log = "";
	
	private ClearCaseChangeset changeset = new ClearCaseChangeset();
	
	private SnapshotView view;
	
	private String message;
	
	public EstablishResult() {
	}
	
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

	public ClearCaseChangeset getChangeset() {
		return changeset;
	}
	
	public void setChangeset( ClearCaseChangeset changeset ) {
		this.changeset = changeset;
	}

	public SnapshotView getView() {
		return view;
	}

	public void setView( SnapshotView view ) {
		this.view = view;
	}
	

}
