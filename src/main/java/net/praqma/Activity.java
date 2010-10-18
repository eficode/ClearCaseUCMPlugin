package net.praqma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

class Activity extends ClearBase
{
	private String fqactivity           = null;
	private String shortname            = null;
	private ArrayList<String> changeset = null;
	private String stream               = null;
	private String pvob                 = null;
	
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
	
	public void Create( String id, String comment )
	{
		logger.trace_function();
		
		if( id == null )
		{
			logger.warning( "ERROR: Activity::create(): Nothing to create!" );
			System.err.println( "ERROR: Activity::create(): Nothing to create!" );
			return;
		}
		
		String commentswitch = comment != null ? " -comment " + comment : "";
		
		Snapview view = new Snapview( null );
		
		// my $cmd = 'cleartool mkact '.$commentswitch.' '.$options{'id'}.' 2>&1';
		String cmd = "mkact " + commentswitch + " " + id;
		String result = Cleartool.run( cmd );
		/* CHW: I cannot figure out what is newed! */
		// return $package->new($options{'id'}.'@'.$view->get_pvob(),1);
	}
	
	public String toString()
	{
		logger.trace_function();
		
		return "";
	}
	
	public ArrayList<String> GetChangeSet()
	{
		logger.trace_function();
		
		if( this.changeset != null )
		{
			return this.changeset;
		}
		
		// cleartool desc -fmt %[versions]Cp activity:'.$self->{'fqactivity'}.' 2>&1';
		String cmd = "desc -fmt %[versions]Cp activity:" + this.fqactivity;
		String result = Cleartool.run( cmd );
		ArrayList<String> cset = new ArrayList<String>();
		String[] rs = result.split( ", " );
		for( int i = 0 ; i < rs.length ; i++ )
		{
			cset.add( rs[i] );
		}
		
		logger.debug( "Applying cset to " );
		this.changeset = cset;
		
		return this.changeset;
	}
	
	public String GetShortname()
	{
		logger.trace_function();
		
		return this.shortname;
	}
	
	public ArrayList<String> GetChangeSetAsElements()
	{
		logger.trace_function();
		
		/* CHW: Shouldn't we check if the changelog exists? */
		
		logger.debug( "Splitting changelog at \\@\\@" );
		HashMap<String, String> cs = new HashMap<String, String>();
		for( int i = 0 ; i < this.changeset.size() ; i++ )
		{
			// split /\@\@/, $cs;
			String[] csa = this.changeset.get( i ).split( "\\@\\@@" );
			cs.put( csa[0], "" );
		}
		
		/* CHW: Experimental sorting. UNTESTED! */
		logger.debug( "Experimental sorting. UNTESTED!" );
		SortedSet<String> sortedset = new TreeSet<String>( cs.keySet() );
		Iterator<String> it = sortedset.iterator();
		
		ArrayList<String> r = new ArrayList<String>();
		
	    while ( it.hasNext() )
	    {
	        r.add( cs.get( it.next() ) );
	    }
		
		return r;
	}
	
	
}