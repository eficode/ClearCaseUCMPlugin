package net.praqma;

import java.util.ArrayList;

class Stream extends ClearBase
{
	private String fqstream                = null;
	private String found_bls               = null;
	private String rec_bls                 = null;
	private ArrayList<Baseline> latest_bls = null;
	private String parent                  = null;
	private String brtype                  = null;
	private String viewroot                = null;
	private ArrayList<Activity> activities = null;
	private String shortname               = null;
	private String pvob                    = null;	
	
	public Stream( String fqstream, boolean trusted )
	{
		logger.trace_function();
		
		/* Delete the object prefix, if it exists: */
		if( fqstream.startsWith( "stream:" ) )
		{
			logger.debug( "Removing \"stream:\" from name" );
			fqstream.substring( 0, 7 );
		}
		
		this.fqstream = fqstream;
		this.fqname   = fqstream;
		String[] res  = TestComponent( fqstream );
		
		this.shortname = res[0];
		this.pvob      = res[1];
		
		if( !trusted )
		{
			String cmd = "desc stream:" + fqstream;
			Cleartool.run( cmd );
		}
		
		
	}
	
	public static Stream Create( String stream_fqname, Stream parent_stream, String comment, Baseline baseline, boolean readonly )
	{
		logger.trace_function();
		
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
	
	
	public void Recommend()
	{
		
	}
	
	/**
	 * 
	 * @param baseline
	 * @param view
	 * @param complete
	 * @return
	 */
	public int Rebase( Baseline baseline, View view, String complete )
	{
		logger.trace_function();
		
		Snapview sview = (Snapview)view;
		
		if( baseline == null && view == null )
		{
			System.err.println( "required parameters are missing" );
			logger.log( "required parameters are missing", "error" );
			System.exit( 1 );
		}
		
		if( complete != null )
		{
			complete = complete.length() == 0 ? " -complete " : complete;
		}
		else
		{
			complete = " -complete ";
		}
		
		// cleartool( "rebase $complete -force -view " . $params{view}->get_viewtag(). " -stream " . $self->get_fqname(). " -baseline " . $params{baseline}->get_fqname()
		String cmd = "rebase " + complete + " -force -view " + sview.GetViewTag() + " -stream " + this.GetFQName() + " -baseline " + baseline.GetFQName();
		Cleartool.run( cmd );
		
		return 1;
	}
	
	public void Remove()
	{
		logger.trace_function();
	}
	
	public String GetPvob()
	{
		logger.trace_function();
		return pvob;
	}
	
	public void GetSingleTopComponent()
	{
		logger.trace_function();
	}
	
	public void GetSingleLatestBaseline()
	{
		logger.trace_function();
	}
	
	public void GetRecBls()
	{
		logger.trace_function();
	}
	
	
	/**
	 * 
	 * @param expanded
	 * @return
	 */
	public ArrayList<Baseline> GetLatestBls( boolean expanded )
	{
		logger.trace_function();
		
		if( this.latest_bls == null )
		{
			// 'cleartool desc -fmt %[latest_bls]p stream:' . $self->{'fqstream'} . ' 2>&1';
			String cmd = "desc -fmt %[latest_bls]p stream:" + this.fqstream;
			String result = Cleartool.run( cmd );
			
			String[] rs = result.split( " " );
			
			for( int i = 0 ; i < rs.length ; i++ )
			{
				if( rs[i].matches( "\\S+" ) )
				{
					this.latest_bls.add( new Baseline( rs[i].trim(), true ) );
				}				
			}
		}
				
		if( expanded )
		{
			return Baseline.StaticExpandBls( this.latest_bls );
		}
		
		return this.latest_bls;
	}
	
	public void GetFoundBls()
	{
		logger.trace_function();
	}
	
	public String GetFQName()
	{
		logger.trace_function();
		return this.fqstream;
	}
	
	public String Shortname()
	{
		logger.trace_function();
		return this.shortname;
	}
	
	public void BetBrType()
	{
		logger.trace_function();
	}
	
	
	public ArrayList<Activity> GetActivities()
	{
		logger.trace_function();
		
		if( this.activities != null )
		{
			return this.activities;
		}
		
		// 'desc -fmt %[activities]p stream:' . $self->{'fqstream'} );
		String cmd = "desc -fmt %[activities]p stream:" + this.fqstream;
		String result = Cleartool.run( cmd );
		this.activities = new ArrayList<Activity>();
		
		String[] rs = result.split( " " );
		for( int i = 0 ; i < rs.length ; i++ )
		{
			this.activities.add( new Activity( rs[i] + "@" + this.pvob, true ) );
		}
		
		return this.activities;
	}
	
	public void GetFullChangeSetAsElements()
	{
		logger.trace_function();
	}
	
	public void DiffRecLatest()
	{
		logger.trace_function();
	}
	
	public static void GetStreamOfWorkingView()
	{
		logger.trace_function();
	}
	
	public static void StreamExists()
	{
		logger.trace_function();
	}
	

	


	
}