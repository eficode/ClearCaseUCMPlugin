package net.praqma;

class Activity extends ClearBase
{
	private String fqactivity = null;
	private String shortname  = null;
	private String changeset  = null;
	private String stream     = null;
	private String pvob       = null;
	
	public Activity( String fqactivity, boolean trusted )
	{
		logger.trace_function();
		
		String[] res = TestComponent( fqactivity );
		if( res == null )
		{
			logger.log( "ERROR: Activity constructor: The first parameter ("+fqactivity+") must be a fully qualified activity in the format: activity\\@\\PVOB" + linesep, "error" );
			System.err.println( "ERROR: Activity constructor: The first parameter ("+fqactivity+") must be a fully qualified activity in the format: activity\\@\\PVOB" + linesep );
			System.exit( 1 );
		}
		
		this.fqactivity = fqactivity;
		this.fqname     = fqactivity;
		this.shortname  = res[0];
		this.pvob       = res[1];
		
		if( !trusted )
		{
			// cleartool desc activity:$fqactivity 
			String cmd = "desc activity:" + fqactivity;
			Cleartool.run( cmd );
			
		}
	}
	
	public void Create()
	{
		logger.trace_function();
	}
	
	public String toString()
	{
		logger.trace_function();
		
		return "";
	}
	
	public void GetChangeSet()
	{
		logger.trace_function();
		
	}
	
	public void GetShortname()
	{
		logger.trace_function();
	}
	
	public void GetChangeSetAsElements()
	{
		logger.trace_function();
	}
	
	
}