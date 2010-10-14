package net.praqma;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class Snapview extends ClearBase
{
	
	private String viewtag    = null;
	private String viewroot   = null;
	private String cact       = null;
	private Stream stream     = null;
	private String activities = null;
	private String pvob       = null;
	
	protected static final String rx_view_uuid = "view_uuid:(.*)";
	
	public Snapview( String viewroot )
	{
		logger.trace_function();
		
		String cwd = System.getProperty( "user.dir" );
		String newdir = viewroot != null ? viewroot : cwd;
		
		if( !cwd.equals( newdir ) )
		{
			/* Experimental!!! */
			System.setProperty( "user.dir", newdir );
		}
		
		// cleartool("pwv -root");
		String cmd = "pwv -root";
		String wvroot = Cleartool.run( cmd ).trim();
		
		String viewtag = this.StaticViewrootIsValid( wvroot );
		
		// cleartool( 'lsstream -fmt %Xn -view ' . $viewtag );
		cmd = "lsstream -fmt %Xn -view " + viewtag;
		String fqstreamstr = Cleartool.run( cmd ).trim();
		
		/* Still experimental!!! */
		System.setProperty( "user.dir", cwd );
		
		this.viewtag  = viewtag;
		this.viewroot = viewroot;
		this.stream   = new Stream( fqstreamstr, false );
		this.pvob     = this.stream.GetPvob();
	}
	
	
	public void Create( String tag, String viewroot, Stream stream )
	{
		/*  die throw Failure( 'msg' => 'Stream is not valid' )
            unless $params{stream}->isa("Stream"); */
		
		tag = tag.trim();
		viewroot = viewroot.trim();
		
	}
	
	public String StaticViewrootIsValid( String viewroot )
	{
		logger.trace_function();
		
		String viewdotdatpname = viewroot + filesep + "view.dat";
		
		FileReader fr = null;
		try
		{
			fr = new FileReader( viewdotdatpname );
		}
		catch ( FileNotFoundException e1 )
		{
			logger.log( "\"" + viewdotdatpname + "\" not found!", "warning" );
			return "0";
		}
		BufferedReader br = new BufferedReader( fr );
		String line;
		StringBuffer result = new StringBuffer();
		try
		{
			while( ( line = br.readLine() ) != null )
			{
				result.append( line );
			}
		}
		catch ( IOException e )
		{
			logger.log( "Couldn't read lines from " + viewdotdatpname, "warning" );
			result.append( "" );
		}
		
		Pattern pattern = Pattern.compile( rx_view_uuid );
		Matcher match   = pattern.matcher( result.toString() );
		
		/* A match is found */
		String uuid = "";
		try
		{
			uuid = match.group( 1 ).trim();
		}
		catch( IllegalStateException e )
		{
			logger.log( "UUID not found!", "warning" );
			return "0";
		}
		
		return uuid;
	}
}