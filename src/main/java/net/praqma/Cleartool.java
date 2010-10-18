package net.praqma;

import java.io.BufferedReader;
import java.io.InputStreamReader;

class CleartoolException extends Exception
{
	CleartoolException()
	{
		super(); 
	}
	
	CleartoolException( String s )
	{
		super( s ); 
	}

}

class Cleartool
{
	protected static Debug logger = Debug.GetLogger();
	
	protected static final String linesep = System.getProperty( "line.separator" );
	
	public static String run( String cmd ) // throws CleartoolException
	{
		logger.trace_function();
		
		try
		{
			logger.debug( "$ cleartool " + cmd );
			
			Process p = Runtime.getRuntime().exec( "cleartool " + cmd );
			p.waitFor();
			BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
			
			br.close();
			
			/* Abnormal process termination
			 * Should an exception be thrown or should the system die?
			 * CHW: Currently, the system dies 
			 * */
			if ( p.exitValue() > 0 )
			{
				logger.log( "Abnormal process termination", "warning" );
				System.err.println( "Abnormal process termination" );
				System.exit( 1 );
				//throw new CleartoolException();
			}
			
			return br.toString();
		}
		catch ( Exception e )
		{
			logger.log( "Could not execute the command \"" + cmd + "\" correctly", "warning" );
			return "";
		}
	}
	
	public static String[] run( String cmd, boolean ls )
	{
		/* Call the system */
		String result = "";
		
		return result.split( linesep );
	}
	
	
	
	
	public static String run_qx( String cmd )
	{
		return "";
	}
}