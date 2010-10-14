package net.praqma;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.praqma.Debug;

class ClearBase
{
	
	protected static final String delim    = "::";
	
	protected static final String rx_fqobj = "(.*)\\@(\\.*)$";
	
	protected static Debug logger = Debug.GetLogger();
	
	protected static enum Plevel
	{
		REJECTED,
		INITIAL,
		BUILT,
		TESTED,
		PLEVEL_RELEASED;
		
		String GetName()
		{
			return this.GetName();
		}
	}
	
	protected static final String BUILD_IN_PROGRESS_ENUM_TRUE = "\"TRUE\"";
	protected static final String ATTR_BUILD_IN_PROGRESS      = "BuildInProgress";
	
	/**
	 * Test if a component is a fully qualified component in the format: baseline\@\\PVOB (not: $fqobj)
	 */
	public String[] TestComponent( String component )
	{
		Pattern pattern = Pattern.compile( rx_fqobj );
		Matcher matches = pattern.matcher( component );
		
		/* A match is found */
		if( matches.find() )
		{
			String res[] = new String[2];
			res[0] = matches.group( 1 );
			res[1] = matches.group( 2 );
			
			return res;
			
		}
		
		return null;
	}
	
	protected Plevel GetPlevelFromString( String level )
	{
		int l = 0;
		try
		{
			l = Integer.parseInt( level );
		}
		catch( NumberFormatException e )
		{
			return Plevel.INITIAL;
		}
		
		return Plevel.values()[l];
	}
}