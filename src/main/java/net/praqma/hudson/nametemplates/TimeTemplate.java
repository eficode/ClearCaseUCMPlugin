package net.praqma.hudson.nametemplates;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.praqma.hudson.CCUCMBuildAction;

public class TimeTemplate extends Template {
	
	private SimpleDateFormat timeFormat  = new SimpleDateFormat( "HHmmss" );

	@Override
	public String parse( CCUCMBuildAction action, String args ) {
		
		return timeFormat.format( new Date() );
	}
}
