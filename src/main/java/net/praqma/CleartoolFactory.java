package net.praqma;

class CleartoolFactory extends AbstractCleartoolFactory
{
	
	private CleartoolFactory()
	{
		
	}
	
	public static AbstractCleartoolFactory CFGet()
	{
		logger.trace_function();
		
		if( cfInstance == null )
		{
			cfInstance = new CleartoolFactory();
		}
		
		return cfInstance;
	}

	public void Update()
	{
		// TODO Auto-generated method stub
	}

	public String diffbl( String nmerge, String fqname )
	{
		// TODO Auto-generated method stub
		return null;
	}
}