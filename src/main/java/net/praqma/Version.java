package net.praqma;

import java.io.File;

class Version extends ClearBase
{
	private String kind        = null;
	private String date        = null;
	private String user        = null;
	private String machine     = null;
	private boolean checkedout = false;
	private String comment     = null;
	private String branch      = null;
	
	
	public Version( String pathName, boolean trusted )
	{
		logger.trace_function();
		
		/* The path name must be specified and correct(exist) */
		File path = new File( pathName );
		if( !path.equals( path ) )
		{
			return;
		}
		
		String fqname = pathName.matches( "^\\S\\\\.*" ) ? pathName : System.getProperty( "user.dir" ) + filesep + pathName;
		
		this.fqname = fqname;
		
		if( !trusted )
		{
			this.Load();
		}
	}
	
	public void Load()
	{
		logger.trace_function();
		
		/* TODO comment can be multiline!!!! */
		// my $cmd = 'desc -fmt date                user                machine             comment             checkedout          kind                 branch               xname
		String cmd = "desc -fmt %d" + this.delim + "%u" + this.delim + "%h" + this.delim + "%c" + this.delim + "%Rf" + this.delim + "%m" + this.delim + "%Vn" + this.delim + "%Xn " + this.fqname;
		String result = Cleartool.run( cmd );
		String[] rs = result.split( delim );
		
		/* date(0) - user(1) - machine(2) - comment(3) - checkedout(4) - kind(5) - branch(6) - xname(7) */
		this.date       = rs[0];
		this.user       = rs[1];
		this.machine    = rs[2];
		this.comment    = rs[3];
		this.checkedout = rs[4].length() > 0 ? true : false;
		this.kind       = rs[5];
		this.branch     = rs[6];
		
		this.isLoaded = true;
		
	}
	
	/*
	 * GETTERS
	 * 
	 */
	
	public boolean IsCheckedOut()
	{
		logger.trace_function();
		if( !this.isLoaded ) Load();
		return this.checkedout;
	}
	
	public String GetDate()
	{
		logger.trace_function();
		if( !this.isLoaded ) Load();
		return this.date;
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
	
	public String GetMachine()
	{
		logger.trace_function();
		if( !this.isLoaded ) Load();
		return this.machine;
	}
	
	public String GetBranch()
	{
		logger.trace_function();
		if( !this.isLoaded ) Load();
		return this.branch;
	}
	
}





