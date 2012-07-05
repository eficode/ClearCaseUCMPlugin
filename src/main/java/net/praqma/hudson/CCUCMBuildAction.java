package net.praqma.hudson;

import java.io.File;

import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Stream;
import hudson.model.Action;

public class CCUCMBuildAction implements Action {
	
	private Stream stream;
	private Component component;
	private Baseline baseline;

	/* View, possibly remotely */
	private File viewPath;
	private String viewTag;
	
	private Baseline createdBaseline;
	

	
	public CCUCMBuildAction( Stream stream, Component component ) {
		this.stream = stream;
		this.component = component;
	}

	public Baseline getBaseline() {
		return baseline;
	}

	public void setBaseline( Baseline baseline ) {
		this.baseline = baseline;
	}

	public Baseline getCreatedBaseline() {
		return createdBaseline;
	}

	public void setCreatedBaseline( Baseline createdBaseline ) {
		this.createdBaseline = createdBaseline;
	}
	
	public void setViewPath( File path ) {
		this.viewPath = path;
	}
	
	public File getViewPath() {
		return viewPath;
	}

	public String getViewTag() {
		return viewTag;
	}

	public void setViewTag( String viewTag ) {
		this.viewTag = viewTag;
	}

	public Stream getStream() {
		return stream;
	}

	public Component getComponent() {
		return component;
	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public String getIconFileName() {
		return null;
	}

	@Override
	public String getUrlName() {
		return null;
	}

}
