package net.praqma;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;


class Baseline extends ClearBase
{
	private String fqobj                  = null;
	private String user                   = null;
	private Component component           = null;
	private ArrayList<Baseline> depends_on_closure = null;
	private Plevel plevel                = null;
	private String shortname              = null;
	private Stream stream                 = null;
	private String pvob                   = null;
	
	private boolean build_in_progess      = false;
	private String diffs                  = "";
	
	
	
	public Baseline( String fqobj, boolean trusted )
	{
		logger.trace_function();
		
		/*  $fqobj = 'baseline:'.$fqobj;
    	 *	$fqobj =~ s/baseline:baseline:/baseline:/;
    	 * Prefix the object with baseline:
         */
		if( !fqobj.startsWith( "baseline:" ) )
		{
			fqobj = "baseline:" + fqobj;
		}
		
		this.fqobj   = fqobj;
		this.fqname  = fqobj;
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
		this.plevel    = GetPlevelFromString( rs[3] );
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
	
	public ArrayList<Baseline> GetDependencies()
	{
		logger.trace_function();
		
		if( this.depends_on_closure == null )
		{
			return this.depends_on_closure;
		}
		
		// cleartool('desc -fmt %[depends_on_closure]p '.$self->{'fqobj'});
		String cmd = "desc -fmt %[depends_on_closure]p " + fqobj;
		String result = Cleartool.run( cmd );
		
		this.depends_on_closure = new ArrayList<Baseline>();
		
		/* White space separated list */
		String[] rs = result.split( " " );
		
		for( int i = 0 ; i < rs.length ; i++ )
		{
			/* Remove empty matches */
			if( rs[i].matches( "/s+" ) )
			{
				continue;
			}
			
			String baseln = rs[i].trim();
			Baseline baseline = new Baseline( baseln, true );
			
			this.depends_on_closure.add( baseline );
		}
		
		return this.depends_on_closure;
	}
	
	public Plevel GetNextPlevel()
	{
		logger.trace_function();
		
		for( int i = 0 ; i < Plevel.values().length ; i++ )
		{
			if( this.plevel.equals( Plevel.values()[i] ) )
			{
				try
				{
					this.plevel = Plevel.values()[i+1];
				}
				catch( Exception e )
				{
					/* Do nothing.. */
				}
				
				break;
			}
		}
		
		return this.plevel;
		
	}
	
	public String GetShortname()
	{
		logger.trace_function();
		
		if( shortname == null )
		{
			Load();
		}
		
		return shortname;
	}
	
	public String GetPlevel()
	{
		logger.trace_function();

		// cleartool('desc -fmt %[plevel]p '.$self->get_fqname())
		String cmd = "desc -fmt %[plevel]p " + this.GetFQName();
		return Cleartool.run( cmd );
	}
	
	/**
	 * Not complete... Waiting for Lars!
	 */
	public void Promote()
	{
		logger.trace_function();
		
		int i = 0;
		
		while( i < Plevel.values().length )
		{
			if( plevel.equals( Plevel.values()[i] ) )
			{
				// $self->{'plevel'} = $PLEVEL_ENUM[++$i];
				//this.plevel = 
			}
			i++;
		}
	}
	
	
	public boolean BuildInProgess()
	{
		logger.trace_function();
		
		// cleartool('desc -fmt %['.ATTR_BUILD_IN_PROGRESS.']NSa '.$self->get_fqname());
		String cmd = "desc -fmt %['.ATTR_BUILD_IN_PROGRESS.']NSa " + this.GetFQName();
		String result = Cleartool.run( cmd );
		
		return ( result.matches( BUILD_IN_PROGRESS_ENUM_TRUE ) ) ? true : false;
	}
	
	public int MarkBuildInProgess()
	{
		logger.trace_function();
		
		if( !build_in_progess )
		{
			// cleartool('mkattr -default '.ATTR_BUILD_IN_PROGRESS.' '.$self->get_fqname());
			String cmd = "mkattr -default " + ATTR_BUILD_IN_PROGRESS + " " + this.GetFQName();
			Cleartool.run( cmd );
		}
		
		return 1;
	}
	
	/* Not verified */
	public String GetComponentName()
	{
		return this.component.GetName();
	}
	
	public int UnMarkBuildInProgess()
	{
		logger.trace_function();
		
		if( build_in_progess )
		{
			// cleartool('mkattr -default '.ATTR_BUILD_IN_PROGRESS.' '.$self->get_fqname());
			String cmd = "rmattr -default " + ATTR_BUILD_IN_PROGRESS + " " + this.GetFQName();
			Cleartool.run( cmd );
		}
		
		return 1;
	}
	
	/* NOTICE! Inner for loop does not comply with baseline.pm 
	 * The return type is NOT determined yet!!!
	 * */
	public static ArrayList<Baseline> StaticExpandBls( ArrayList<Baseline> bls )
	{
		logger.trace_function();
		
		HashMap<String, Baseline> exp_bls = new HashMap<String, Baseline>();

		for( int i = 0 ; i < bls.size() ; i++ )
		{
			System.out.println( "Baseline:\t" + bls.get( i ) );
			
			exp_bls.put( bls.get( i ).GetComponentName(), bls.get( i ) );
			
			for( int j = 0 ; j < exp_bls.size() ; j++ )
			{
				exp_bls.put( exp_bls.get( j ).GetComponentName(), exp_bls.get( j ) );
			}
		}
		
		SortedSet<String> sortedset = new TreeSet<String>( exp_bls.keySet() );
		Iterator<String> it = sortedset.iterator();
		
		/* May change */
		ArrayList<Baseline> rbls = new ArrayList<Baseline>();
	    while ( it.hasNext() )
	    {
	        rbls.add( exp_bls.get( it.next() ) );
	    }

	    return rbls;
	}
	
	
	public int SetPromotionLevel( Plevel plevel )
	{
		logger.trace_function();
		
		//cleartool('chbl -level '.$plevel.' '.$self->get_fqname() )
		String cms = "chbl -level " + plevel.GetName() + " " + this.GetFQName();
		
		return 1;
	}
	
	public Plevel GetPromotionLevel(  )
	{
		logger.trace_function();
		
		//cleartool('desc -fmt %[plevel]p '.$self->get_fqname()
		String cmd = "desc -fmt %[plevel]p " + this.GetFQName();
		String result = Cleartool.run( cmd );
				
		return GetPlevelFromString( result );
	}
	
	/* Waiting for Lars. */
	public void GetActivities()
	{
		logger.trace_function();
	}
	
	public ArrayList<String> GetDiff( String format, String nmerge, String viewroot )
	{
		logger.trace_function();
		
		String sw_nmerge = ( nmerge.equals( "0" ) ? " -nmerge " : "" );
		
		// cleartool('diffbl -pre -act -ver '.$sw_nmerge.$self->get_fqname );
		String cmd = "diffbl -pre -act -ver " + sw_nmerge + this.GetFQName();
		this.diffs = Cleartool.run( cmd );
		
		String msg = this.diffs;
		
		if( viewroot != null )
		{
			msg = msg.replace( viewroot, "" );
		}
		
		ArrayList<String> list = new ArrayList<String>();
		
		logger.log( "Format = " + format );
		
		if( format.equals( "list" ) )
		{
			msg = msg.replaceAll( "^>>.*$", "" );
			msg = msg.replaceAll( "\\@\\@.*$", "" );
			msg = msg.replaceAll( "^\\s+", "" );
			
			String[] groslist = msg.split( "\n" );
			
			HashMap<String, String> hash = new HashMap<String, String>();
			for( int i = 0 ; i < groslist.length ; i++ )
			{
				hash.put( groslist[i], "" );
			}
			
			SortedSet<String> sortedset = new TreeSet<String>( hash.keySet() );
			Iterator<String> it = sortedset.iterator();
			
		    while ( it.hasNext() )
		    {
		        list.add( hash.get( it.next() ) );
		    }
		}
		
		if( format.equals( "scalar" ) )
		{
			list.add( msg );
		}
		
		return list;
		
	}
	
}