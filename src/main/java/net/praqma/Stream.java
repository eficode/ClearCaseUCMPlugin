package net.praqma;

class Stream extends ClearBase
{
	private String fqstream   = null;
	private String found_bls  = null;
	private String rec_bls    = null;
	private String latest_bls = null;
	private String parent     = null;
	private String brtype     = null;
	private String viewroot   = null;
	private String activities = null;
	private String shortname  = null;
	private String pvob       = null;	
	
	public Stream( String fqstream, boolean trusted )
	{
		/* Delete the object prefix, if it exists: */
		if( fqstream.startsWith( "stream:" ) )
		{
			fqstream.substring( 0, 7 );
		}
		
		this.fqstream = fqstream;
		String[] res = TestComponent( fqstream );
		
		this.shortname = res[0];
		this.pvob      = res[1];
		
		if( !trusted )
		{
			String cmd = "desc stream:" + fqstream;
			String result = Cleartool.run( cmd );
		}
	}
	
	public static Stream Create( String stream_fqname, Stream parent_stream, String comment, Baseline baseline, boolean readonly )
	{
		String args_bl = "";
		String args_cm = " -nc ";
		String args_ro = "";
		
		if( baseline != null )
		{
			args_bl = " -baseline " + baseline.GetFQName();
		}
		if( comment.length() > 0 )
		{
			args_cm = " -c " + comment;
		}
		if( readonly )
		{
			args_ro = " -readonly ";
		}
		
		String cmd = "mkstream " + args_cm + " " + args_bl + " " + args_ro + " -in stream:" + parent_stream.GetFQName() + " " + stream_fqname;
		Cleartool.run( cmd );
		
		return new Stream( stream_fqname, false );
	}
	
	public String GetFQName()
	{
		return this.fqstream;
	}
	
	public String GetPvob()
	{
		return pvob;
	}

	
}