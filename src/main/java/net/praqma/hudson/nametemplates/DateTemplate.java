package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.praqma.hudson.CCUCMBuildAction;

public class DateTemplate extends Template {
	
	private SimpleDateFormat dateFormat  = new SimpleDateFormat( "yyyyMMdd" );

	@Override
	public String parse( CCUCMBuildAction action, String args, FilePath ws ) {
		
		return dateFormat.format( new Date() );
	}

}
