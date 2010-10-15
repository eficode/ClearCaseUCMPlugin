package net.praqma;

class Component extends ClearBase
{
	private String fqobj     = null;
	private String shortname = null;
	private String pvob      = null;
	private String rootdir   = null;
	
	
	public Component( String fqobj, boolean trusted )
	{
		/* Prefix the object with component: */
		if( !fqobj.startsWith( "component:" ) )
		{
			fqobj = "component:" + fqobj;
		}
		
		this.fqobj = fqobj;
		String[] res = TestComponent( fqobj );
		
		this.shortname = res[0];
		this.pvob      = res[1];
	}
	
	
	/* Not verified! */
	public String GetName()
	{
		logger.trace_function();
		
		return shortname;
	}
	
	public String ShortName()
	{
		logger.trace_function();
		
		return shortname;
	}


	public String GetFQName()
	{
		logger.trace_function();
		
		return fqobj;
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

}