package net.praqma.hudson.nametemplates;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.praqma.hudson.scm.CCUCMState.State;

public class TimeTemplate extends Template {
	
	private SimpleDateFormat timeFormat  = new SimpleDateFormat( "HHmmss" );

	@Override
	public String parse( State state, String args ) {
		
		return timeFormat.format( new Date() );
	}
}
