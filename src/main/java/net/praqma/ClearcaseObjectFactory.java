package net.praqma;

import java.util.HashMap;

class ClearcaseObjectFactory
{
	static HashMap<String, Baseline> baselines = new HashMap<String, Baseline>();
	
	protected static Debug logger = Debug.GetLogger();
	
	
	
	/**
	 * This function returns a baseline. If it already exists, it will be returned or else a new will be created and returned. 
	 * @param fqname Fully qualified name
	 * @param trusted Is the baseline trusted by clear base
	 * @return a baseline
	 */
	public static Baseline GetBaseline( String fqname, boolean trusted )
	{
		logger.trace_function();
		
		if( baselines.containsKey( fqname ) )
		{
			logger.log( "The baseline " + fqname + " already exists." );
			return baselines.get( fqname );
		}
		
		logger.log( "Creating the baseline " + fqname );
		Baseline bl = new Baseline( fqname, trusted );
		baselines.put( fqname, bl );
		
		return bl;
	}
}