package net.praqma;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.praqma.Debug;

class ClearBase
{
	
	protected static final String delim    = "::";
	
	protected static final String rx_fqobj = "(.*)\\@(\\.*)$";
	
	protected static Debug logger = Debug.GetLogger();
	
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
}