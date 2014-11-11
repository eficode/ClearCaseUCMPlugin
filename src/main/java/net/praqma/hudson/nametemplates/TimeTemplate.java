package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.praqma.hudson.CCUCMBuildAction;

public class TimeTemplate extends Template {
	
	private final SimpleDateFormat timeFormat  = new SimpleDateFormat( "HHmmss" );

	@Override
	public String parse( CCUCMBuildAction action, String args, FilePath ws  ) {
		
		return timeFormat.format( new Date() );
	}
}
