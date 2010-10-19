package net.praqma;

import java.io.BufferedReader;
import java.io.InputStreamReader;

class CleartoolException extends RuntimeException
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

/**
 * The Cleartool proxy class
 * All calls to cleartool, should be done through these static functions.
 * run( String )  : returns the return value as String.
 * run_a( String ): returns the return value as an array of Strings, separated by new lines. 
 * @author wolfgang
 *
 */
class Cleartool
{
	protected static Debug logger = Debug.GetLogger();
	
	protected static final String linesep = System.getProperty( "line.separator" );
	
	/**
	 * Executes a cleartool command.
	 * @param cmd
	 * @return The return value of the cleartool command as a String
	 */
	public static String run( String cmd ) throws CleartoolException
	{
		logger.trace_function();
		
		try
		{
			logger.debug( "$ cleartool " + cmd );
			
			/* Call cleartool and wait for it to finish. */
			Process p = Runtime.getRuntime().exec( "cleartool " + cmd );
			p.waitFor();
			BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
			
			br.close();
			
			/* Abnormal process termination
			 * Should an exception be thrown or should the system die?
			 * CHW: Currently, the system dies 
			 * */
			if ( p.exitValue() != 0 )
			{
				logger.log( "Abnormal process termination", "warning" );
				System.err.println( "Abnormal process termination" );
				throw new CleartoolException();
			}
			
			/* Return the buffer as a String */
			return br.toString();
		}
		catch ( Exception e )
		{
			logger.log( "Could not execute the command \"" + cmd + "\" correctly", "warning" );
			return "";
		}
	}
	
	/**
	 * Executes a cleartool command.
	 * @param cmd
	 * @return The return value of the cleartool command as an array Strings, separated by new lines.
	 */
	public static String[] run_a( String cmd )
	{
		logger.trace_function();
		
		/* Just call the run method an split the result */
		String result = run( cmd );		
		return result.split( linesep );
	}
	
	
	
	
	public static String run_qx( String cmd )
	{
		return "";
	}
}