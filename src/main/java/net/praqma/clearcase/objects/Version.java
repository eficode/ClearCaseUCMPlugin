package net.praqma.clearcase.objects;

import net.praqma.debug.Debug;
import net.praqma.clearcase.cleartool.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version extends ClearBase
{
	private String kind        = null;
	private String date        = null;
	private String user        = null;
	private String machine     = null;
	private boolean checkedout = false;
	private String comment     = null;
	private String branch      = null;
	
	/* New stuff */
	private String file        = null;
	private int revision       = 0;
	
	private static String rx_revision = "(\\d+)$";
	private static Pattern p_revision = Pattern.compile( "(\\d+)$" );
	
	
	
	private Version( String pathName, boolean trusted )
	{
		logger.trace_function();
		
		/* The path name must be specified and correct(exist) */
		File path = new File( pathName );
		if( !path.equals( path ) )
		{
			/* TODO should this generate an error? */
			return;
		}
		
		String fqname = pathName.matches( "^\\S:\\\\.*" ) ? pathName : System.getProperty( "user.dir" ) + filesep + pathName;
		
		this.fqname = fqname;
		
		Matcher m = p_revision.matcher( this.fqname );
		if( m.find() )
		{
			this.revision = Integer.parseInt( m.group( 1 ) );
		}
		else
		{
			this.revision = 0;
		}
		
		String tmp = this.fqname;
		tmp        = tmp.replaceFirst( "(?m)^>>.*$", "" );
		tmp        = tmp.replaceFirst( "(?m)\\@\\@.*$", "" );
		tmp        = tmp.replaceFirst( "(?m)^\\s+", "" );
		this.file  = tmp;
		
		if( !trusted )
		{
			this.Load();
		}
	}
	
	/* The overridden "factory" method for creating Clearcase objects */
	public static Version GetObject( String fqname, boolean trusted )
	{
		logger.trace_function();
		
		logger.log( "Retrieving Version " + fqname );
		
		if( objects.containsKey( fqname ) )
		{
			return (Version)objects.get( fqname );
		}
		
		logger.log( "The Version " + fqname + " doesn't exist. Creating it." );
		Version obj = new Version( fqname, trusted );
		objects.put( fqname, obj );
		
		return obj;
	}
	
	public void Load()
	{
		logger.trace_function();
		
		/* TODO comment can be multiline!!!! */
		// my $cmd = 'desc -fmt date                user                machine             comment             checkedout          kind                 branch               xname
		//String cmd = "desc -fmt %d" + this.delim + "%u" + this.delim + "%h" + this.delim + "%c" + this.delim + "%Rf" + this.delim + "%m" + this.delim + "%Vn" + this.delim + "%Xn " + this.fqname;
		//String result = Cleartool.run( cmd );
		HashMap<String, String> result = CTF.LoadVersion( this.fqname );
		
		this.date       = result.get( "date" );
		this.user       = result.get( "user" );
		this.machine    = result.get( "machine" );
		this.comment    = result.get( "comment" );
		this.checkedout = result.get( "checkedout" ).length() > 0 ? true : false;
		this.kind       = result.get( "kind" );
		this.branch     = result.get( "branch" );
		
		this.loaded = true;
	}
	
	public void Load2()
	{
		logger.trace_function();
		
		/* TODO comment can be multiline!!!! */
		// my $cmd = 'desc -fmt date                user                machine             comment             checkedout          kind                 branch               xname
		//String cmd = "desc -fmt %d" + this.delim + "%u" + this.delim + "%h" + this.delim + "%c" + this.delim + "%Rf" + this.delim + "%m" + this.delim + "%Vn" + this.delim + "%Xn " + this.fqname;
		//String result = Cleartool.run( cmd );
		
		
		
//		String result = CTF.LoadVersion( this.fqname );
//		String[] rs = result.split( delim );
//		
//		/* date(0) - user(1) - machine(2) - comment(3) - checkedout(4) - kind(5) - branch(6) - xname(7) */
//		this.date       = rs[0];
//		this.user       = rs[1];
//		this.machine    = rs[2];
//		this.comment    = rs[3];
//		this.checkedout = rs[4].length() > 0 ? true : false;
//		this.kind       = rs[5];
//		this.branch     = rs[6];
//		
//		this.loaded = true;
		
	}
	
	/*
	 * GETTERS
	 *
	 */
	
	public static ArrayList<Version> GetUnique( ArrayList<Version> list )
	{
		HashMap<String, Version> unique = new HashMap<String, Version>();
		for( Version v : list )
		{
			/* If the key already exists */
			if( unique.containsKey( v.file ) )
			{
				Version v1 = unique.get( v.file );
				/* Replace with newer revisions */
				if( v.revision > v1.revision )
				{
					unique.put( v.file, v );		
				}
			}
			/* If it doesn't, put it in! */
			else
			{
				unique.put( v.file, v );
			}
		}

		ArrayList<Version> result = new ArrayList<Version>();

		logger.log( "Experimental sorting. TESTED!", "experimental" );
		SortedSet<String> sortedset = new TreeSet<String>( unique.keySet() );
		
	    for( String v : sortedset )
	    {
	    	result.add( unique.get( v ) );
	    }
	    
	    return result;
	}
	
	public String GetFilename()
	{
		return this.file;
	}

	public int GetRevision()
	{
		return this.revision;
	}
	
	public boolean IsCheckedOut()
	{
		logger.trace_function();
		if( !this.loaded ) Load();
		return this.checkedout;
	}
	
	public String GetDate()
	{
		logger.trace_function();
		if( !this.loaded ) Load();
		return this.date;
	}
	
	public String GetUser()
	{
		logger.trace_function();
		if( !this.loaded ) Load();
		return this.user;
	}
	
	/**
	 * TODO Blame is not implemented yet
	 */
	public String Blame()
	{
		logger.trace_function();
		if( !this.loaded ) Load();
		return Blame( null );
	}
	public String Blame( String[] dontblame )
	{
		logger.trace_function();
		if( !this.loaded ) Load();
		return this.user;		
	}
	
	public String GetMachine()
	{
		logger.trace_function();
		if( !this.loaded ) Load();
		return this.machine;
	}
	
	public String GetBranch()
	{
		logger.trace_function();
		if( !this.loaded ) Load();
		return this.branch;
	}
	
}





