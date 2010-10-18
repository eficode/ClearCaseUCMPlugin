package net.praqma;

class Component extends ClearBase
{
	private String fqobj     = null;
	private String shortname = null;
	private String pvob      = null;
	private String rootdir   = null;
	
	
	public Component( String fqobj, boolean trusted )
	{
		logger.trace_function();
		
		/* Prefix the object with component: */
		if( !fqobj.startsWith( "component:" ) )
		{
			fqobj = "component:" + fqobj;
		}
		
		this.fqobj   = fqobj;
		this.fqname  = fqobj;
		String[] res = TestComponent( fqobj );
		
		this.shortname = res[0];
		this.pvob      = res[1];
		
		if( !trusted )
		{
			// 'desc '.$fqobj
			Cleartool.run( "desc " + fqobj );
		}
	}
	
	public void GetBlsQueuedFor()
	{
		logger.trace_function();
	}
	
	public void GetBlsWithPlevel()
	{
		logger.trace_function();
	}
	
	
	/* Not verified! */
	public String GetName()
	{
		logger.trace_function();
		
		return shortname;
	}
	
	public String GetShortName()
	{
		logger.trace_function();
		
		return shortname;
	}
	
	public String GetPvob()
	{
		logger.trace_function();
		
		return pvob;
	}
	
	public String GetRootDir()
	{
		logger.trace_function();
		
		if( rootdir == null )
		{
			// cleartool("desc -fmt %[root_dir]p ".$self->get_fqname());
			String cmd = "desc -fmt %[root_dir]p " + this.GetFQName();
			this.rootdir = Cleartool.run( cmd ).trim();
		}
		
		return rootdir;
	}
	
	public void GetAttr()
	{
		logger.trace_function();
	}

}