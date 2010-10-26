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

	public String[] lsbl_s_comp_stream( String compFqname, String streamFqname )
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String LoadBaseline( String fqname )
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void BaselineMarkBuildInProgress( String fqname, String mark )
	{
		// TODO Auto-generated method stub
		
	}

	public void BaselineMakeAttribute( String fqname, String attr )
	{
		// TODO Auto-generated method stub
		
	}

	public void BaselineRemoveAttribute( String fqname, String attr )
	{
		// TODO Auto-generated method stub
		
	}

	public void SetPromotionLevel( String fqname, String plevel )
	{
		// TODO Auto-generated method stub
		
	}

	public String GetPromotionLevel( String fqname )
	{
		// TODO Auto-generated method stub
		return null;
	}

	public boolean BuildInProgess( String fqname )
	{
		// TODO Auto-generated method stub
		return false;
	}

	public String[] ListBaselines( String component, String stream,
			String plevel )
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String GetRecommendedBaseline( String stream )
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void RecommendBaseline( String stream, String baseline )
			throws CleartoolException
	{
		// TODO Auto-generated method stub
		
	}

	public String LoadVersion( String version )
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String LoadChangeset( String changeset )
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String GetChangeset( String activity )
	{
		// TODO Auto-generated method stub
		return null;
	}
}