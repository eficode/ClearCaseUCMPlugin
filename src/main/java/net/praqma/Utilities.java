package net.praqma;

import java.io.File;
import java.util.ArrayList;

class Utilities
{
	protected static Debug logger = Debug.GetLogger();
	
	public static ArrayList<String> GetNonEmptyArrayElements( String[] array )
	{
		logger.trace_function();
		
		ArrayList<String> result = new ArrayList<String>();
		for( int i = 0 ; i < array.length ; i++ )
		{
			if( array[i].matches( "^\\S+$" ) )
			{
				result.add( array[i] );
			}
		}
		
		return result;
	}
	
	/**
	 * CHW: Scary stuff!
	 * @param <T>
	 * @param array
	 * @return
	 */
	public static <T> ArrayList<T> GetNonEmptyArrayElements( ArrayList<T> array )
	{
		logger.trace_function();
		
		logger.trace_function();
		logger.log( "Running experimental code with template parameters", "experimental" );
		
		ArrayList<T> result = new ArrayList<T>();
		for( int i = 0 ; i < array.size() ; i++ )
		{
			if( array.get( i ).toString().matches( "^\\S+$" ) )
			{
				result.add( array.get( i ) );
			}
		}
		
		return result;
	}
	
	/*
	 * rx | nn | val
	 * 1  | 1  | 1    (x)
	 * 1  | 0  | 0   !(x)
	 * 0  | 1  | 0    (x)
	 * 0  | 0  | 1   !(x)
	 * 
	 */
	

	/**
	 * CHW: Experimental
	 * @param <T>
	 */
	public static <T> ArrayList<T> GetArrayElements( ArrayList<T> array, String regex, boolean mustbe )
	{
		logger.trace_function();
		logger.log( "Running experimental code, templated again!!", "experimental" );
		
		ArrayList<T> result = new ArrayList<T>();
		for( int i = 0 ; i < array.size() ; i++ )
		{
			if( array.get( i ).toString().matches( regex ) == mustbe )
			{
				result.add( array.get( i ) );
			}
		}
		
		return result;
	}
	
	/**
	 * CHW: Even more experimental!!!
	 * @param files
	 * @return
	 */
	public static File[] StringsToFiles( String[] files )
	{
		logger.trace_function();
		logger.log( "Running experimental code", "experimental" );
		
		File[] result = new File[files.length];
		for( int i = 0 ; i < files.length ; i++ )
		{
			result[i] = new File( files[i] );
			/* Tests the file */
			if( !result[i].exists() )
			{
				logger.log( "The file \"" + result[i].toString() + "\" does not exist!", "warning" );
			}
		}
		
		return result;
	}
	
	
}