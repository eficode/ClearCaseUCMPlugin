package net.praqma.clearcase.cleartool;

import net.praqma.clearcase.objects.*;
import net.praqma.debug.Debug;

import java.util.HashMap;

class ClearcaseObjectException extends RuntimeException
{
	ClearcaseObjectException()
	{
		super(); 
	}
	
	ClearcaseObjectException( String s )
	{
		super( s ); 
	}
}

public class ClearcaseObjectFactory
{
	static HashMap<String, Baseline> baselines   = new HashMap<String, Baseline>();
	static HashMap<String, Component> components = new HashMap<String, Component>();
	static HashMap<String, Stream> streams       = new HashMap<String, Stream>();
	static HashMap<String, Snapview> snapview    = new HashMap<String, Snapview>();
	
	static HashMap<String, ClearBase> objects       = new HashMap<String, ClearBase>();
	static HashMap<String, ClearBase> objectClasses = new HashMap<String, ClearBase>();
	
	protected static Debug logger = Debug.GetLogger();
	
	static
	{
		logger.debug( "Why is this not run? IT IS!!!" );
	}
	
	/* Singleton implementation */
	private static final ClearcaseObjectFactory INSTANCE = new ClearcaseObjectFactory();
	private ClearcaseObjectFactory()
	{
		
	}
	
	public void RegisterType( String type, ClearBase objectClass )
	{
		logger.debug( "Registering type " + type );
		objectClasses.put( type, objectClass );
	}
	
	public static ClearcaseObjectFactory GetInstance()
	{
		logger.debug( "Returning the instance!" );
		return INSTANCE;
	}
	
	/**
	 * This function returns a ClearBase object. 
	 * @param fqname Fully qualified name
	 * @param trusted Is the baseline trusted by clear base
	 * @return a baseline
	 */
//	public ClearBase GetObject( String fqname, boolean trusted, String type ) throws ClearcaseObjectException
//	{
//		logger.trace_function();
//		
//		if( objects.containsKey( fqname ) )
//		{
//			logger.log( "The ClearBase object, \"" + fqname + "\", already exists." );
//			return objects.get( fqname );
//		}
//		
//		if( !objectClasses.containsKey( fqname ) )
//		{
//			throw new ClearcaseObjectException( "The object type " + type + " is not registered!" );
//		}
//		
//		logger.debug( "type="+type );
//		ClearBase obj = ((ClearBase)objectClasses.get( type )).GetObject( fqname, trusted );
//		logger.log( "Creating the object " + fqname + " of type " + obj.getClass().getName() );
//		objects.put( fqname, obj );
//		
//		return obj;
//	}
	
	
	
	/**
	 * This function creates a baseline. If it already exists, it will be returned. 
	 * @param fqname Fully qualified name
	 * @param trusted Is the baseline trusted by clear base
	 * @return a baseline
	 */
//	public static Baseline GetBaseline( String fqname, boolean trusted )
//	{
//		logger.trace_function();
//		
//		if( baselines.containsKey( fqname ) )
//		{
//			logger.log( "The baseline " + fqname + " already exists." );
//			return baselines.get( fqname );
//		}
//		
//		logger.log( "Creating the baseline " + fqname );
//		Baseline bl = new Baseline( fqname, trusted );
//		baselines.put( fqname, bl );
//		
//		return bl;
//	}
}