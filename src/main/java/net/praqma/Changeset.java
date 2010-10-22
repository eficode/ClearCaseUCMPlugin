package net.praqma;

import java.util.ArrayList;

class Changeset extends ClearBase
{
	public ArrayList<Version> versions = new ArrayList<Version>();
	
	private String user       = null;
	private Activity activity = null;
	
	public Changeset( String name, boolean trusted )
	{
		logger.trace_function();
		
		/* TODO: The name string should be examined to see if it is a correct fqname */
		
		this.fqname = name;
		
		if( !trusted )
		{
			this.Load();
		}
	}
	
	public void Load()
	{
		logger.trace_function();
		
		this.isLoaded = true;
	}
	
	public String GetUser()
	{
		logger.trace_function();
		if( !this.isLoaded ) Load();
		return this.user;
	}
	
	/**
	 * TODO Blame is not implemented yet
	 */
	public String Blame()
	{
		logger.trace_function();
		if( !this.isLoaded ) Load();
		return Blame( null );
	}
	public String Blame( String[] dontblame )
	{
		logger.trace_function();
		if( !this.isLoaded ) Load();
		return this.user;		
	}
}