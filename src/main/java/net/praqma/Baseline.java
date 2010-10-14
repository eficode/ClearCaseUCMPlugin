package net.praqma;


class Baseline extends ClearBase
{
	private String fqobj              = null;
	private String user               = null;
	private Component component       = null;
	private String depends_on_closure = null;
	private String plevel             = null;
	private String shortname          = null;
	private Stream stream             = null;
	private String pvob               = null;
	
	
	
	public Baseline( String fqobj, boolean trusted )
	{

		
		/*  $fqobj = 'baseline:'.$fqobj;
    	 *	$fqobj =~ s/baseline:baseline:/baseline:/;
    	 * Prefix the object with baseline:
         */
		if( !fqobj.startsWith( "baseline:" ) )
		{
			fqobj = "baseline:" + fqobj;
		}
		
		this.fqobj = fqobj;
		String[] res = TestComponent( fqobj );
		
		this.pvob  = res[1];
		
		if( !trusted )
		{
			Load();
		}
	}
	
	
	public void Load()
	{
		logger.trace_function();
		
		String cmd = "desc -fmt %n" + delim + "%[component]p" + delim + "%[bl_stream]p" + delim + "%[plevel]p" + delim + "%u " + fqobj;
		String result = Cleartool.run( cmd );
		
		//my ($shortname, $component, $stream, $plevel, $user) = split /$delim/, $retval;
		String[] rs = result.split( delim );
		this.shortname = rs[0];
		this.component = new Component( rs[1] + "@" + this.pvob, true );
		this.stream    = new Stream( rs[2] + "@" + this.pvob, true );
		this.plevel    = rs[3];
		this.user      = rs[4];
		
	}
	
	public String GetFQName()
	{
		return this.fqobj;
	}
	
	public String GetUser()
	{
		logger.trace_function();
		
		if( user == null )
		{
			Load();
		}
		
		return user;
	}
	
	public Component GetComponent()
	{
		logger.trace_function();
		
		if( component == null )
		{
			Load();
		}
		
		return component;
	}
	
	public Stream GetStream()
	{
		logger.trace_function();
		
		if( stream == null )
		{
			Load();
		}
		
		return stream;
	}
	
	public String GetDependencies()
	{
		logger.trace_function();
		
		if( this.depends_on_closure == null )
		{
			return this.depends_on_closure;
		}
		
		// cleartool('desc -fmt %[depends_on_closure]p '.$self->{'fqobj'});
		String cmd = "desc -fmt %[depends_on_closure]p " + fqobj;
		String result = Cleartool.run( cmd );
		
		String[] rs = result.split( " " );
		
		return "";
	}
	

	
}