package net.praqma;


class Baseline extends ClearBase
{
	private String fqobj        = null;
	private String user         = null;
	private Component component = null;
	private String plevel       = null;
	private String shortname    = null;
	private Stream stream       = null;
	private String pvob         = null;
	
	
	
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
	}
	
	
	public void Load()
	{
		String cmd = "desc -fmt %n" + delim + "%[component]p" + delim + "%[bl_stream]p" + delim + "%[plevel]p" + delim + "%u " + fqobj;
		String result = Cleartool.run( cmd );
		
		//my ($shortname, $component, $stream, $plevel, $user) = split /$delim/, $retval;
		String[] rs = result.split( delim );
		this.component = new Component( rs[0] + "@" + this.pvob, true );
		this.stream    = new Stream( rs[1] + "@" + this.pvob, true );
		this.plevel    = rs[2];
		this.user      = rs[3];
		
	}
	
	public String GetFQName()
	{
		return this.fqobj;
	}
	
	public String GetUser()
	{
		return this.user;
	}
	

	
}