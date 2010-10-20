package net.praqma;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import net.praqma.Debug;

/**
 * UNTESTED cmd class
 * @author wolfgang
 *
 */
class Cmd
{
	protected static Debug logger = Debug.GetLogger();
	
	public static String run( String cmd )
	{
		logger.trace_function();
		
		try
		{
			logger.debug( "$ " + cmd );
			Process p = Runtime.getRuntime().exec( cmd );
			p.waitFor();
			BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
			
			br.close();
			
			return br.toString();
		}
		catch ( Exception e )
		{
			logger.log( "Could not execute the command \"" + cmd + "\" correctly", "warning" );
			return "";
		}
	}
}