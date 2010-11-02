package net.praqma.clearcase.objects;

import net.praqma.debug.Debug;

import java.util.ArrayList;

public class Changeset extends ClearBase
{
	public ArrayList<Version> versions = new ArrayList<Version>();
	
	private String user       = null;
	private Activity activity = null;
	
	private Changeset( String name, boolean trusted )
	{
		logger.trace_function();
		
		/* TODO: The name string should be examined to see if it is a correct fqname */
				
		this.fqname = name;
		
		if( !trusted )
		{
			this.Load();
		}
	}
	
	/* The overridden "factory" method for creating Clearcase objects */
	public static Changeset GetObject( String fqname, boolean trusted )
	{
		logger.trace_function();
		
		logger.log( "Retrieving Changeset " + fqname );
		
		if( objects.containsKey( fqname ) )
		{
			return (Changeset)objects.get( fqname );
		}
		
		logger.log( "Creating the Changeset " + fqname );
		Changeset obj = new Changeset( fqname, trusted );
		objects.put( fqname, obj );
		
		return obj;
	}
	
	public void Load()
	{
		logger.trace_function();
		
		// cleartool desc -fmt %[versions]Cp activity:'.$self->{'fqactivity'}.' 2>&1';
		//String cmd = "desc -fmt %[versions]Cp activity:" + this.fqactivity;
		//String result = Cleartool.run( cmd );
		String result = CTF.GetChangeset( this.activity.GetFQName() );
		
		String[] rs = result.split( ", " );
		
		for( int i = 0 ; i < rs.length ; i++ )
		{
			this.versions.add( Version.GetObject( rs[i], true ) );
		}
		
		this.loaded = true;
	}
	
	public void SetActivity( Activity activity )
	{
		this.activity = activity;
	}
	
	public String GetUser()
	{
		logger.trace_function();
		if( !this.loaded ) Load();
		return this.user;
	}
	
	public String Stringify()
	{
		logger.trace_function();
		
		String s = "Outputting Changeset " + this.fqname + linesep;
		
		for( Version v: this.versions )
		{
			s += "Version: " + v.GetFQName() + linesep;
		}
		
		return s;
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
}