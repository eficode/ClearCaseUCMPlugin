package net.praqma.hudson.nametemplates;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.praqma.hudson.scm.CCUCMState.State;

public class DateTemplate extends Template {
	
	private SimpleDateFormat dateFormat  = new SimpleDateFormat( "yyyyMMdd" );

	@Override
	public String parse( State state, String args ) {
		
		return dateFormat.format( new Date() );
	}

}
