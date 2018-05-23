package net.praqma.hudson.nametemplates;

import hudson.FilePath;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.praqma.hudson.CCUCMBuildAction;

public class DateTemplate extends Template {
	@Override
	public String parse( CCUCMBuildAction action, String args, FilePath ws ) {
		SimpleDateFormat dateFormat  = new SimpleDateFormat( "yyyyMMdd" );
		return dateFormat.format( new Date() );
	}

}
