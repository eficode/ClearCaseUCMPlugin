package net.praqma.clearcase.objects;

import net.praqma.debug.Debug;
import net.praqma.clearcase.cleartool.Cleartool;
import net.praqma.clearcase.Cmd;
import net.praqma.clearcase.Utilities;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Snapview extends View
{
	
	private String viewtag    = null;
	private String viewroot   = null;
	private Activity cact     = null;
	private Stream stream     = null;
	private String activities = null;
	private String pvob       = null;
	
	protected ViewType viewType = ViewType.SNAPVIEW;
	
	protected static final String rx_view_uuid  = "view_uuid:(.*)";
	protected static final String rx_components = "all|mod";
	protected static final String rx_log        = "/(^\\s*log has been written to)\\s*\"(.*?)\"";
	
	private Snapview( String viewroot, boolean trusted )
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
		//this.stream   = new Stream( fqstreamstr, false );
		this.stream   = Stream.GetObject( fqstreamstr, false );
		this.pvob     = this.stream.GetPvob();
	}
	
	/* The overridden "factory" method for creating Clearcase objects */
	public static Snapview GetObject( String fqname, boolean trusted )
	{
		logger.trace_function();
		
		logger.log( "Retrieving Baseline " + fqname );
		
		if( objects.containsKey( fqname ) )
		{
			return (Snapview)objects.get( fqname );
		}
		
		logger.log( "Creating the Baseline " + fqname );
		Snapview obj = new Snapview( fqname, trusted );
		objects.put( fqname, obj );
		
		return obj;
	}
	
	
	public Snapview Create( String tag, String viewroot, Stream stream )
	{
		/*  die throw Failure( 'msg' => 'Stream is not valid' )
            unless $params{stream}->isa("Stream"); */
		
		tag = tag.trim();
		viewroot = viewroot.trim();
		
		String cmd = "rd /S /Q " + viewroot;
		Cmd.run( cmd );
		
		// cleartool( "mkview -snap -tag " . $params{tag} . " -stream " . $params{stream}->get_fqname . " " . $params{viewroot} );
		cmd = "mkview -snap -tag " + tag + " -stream " + stream.GetFQName() + " " + viewroot;
		Cleartool.run( cmd );
		
		return new Snapview( viewroot, true );
	}
	
	/**
	 * Incomplete function!
	 * 
	 * @param components
	 * @param loadrules
	 * @param updtlog
	 * @return
	 */
	public int Update( String components, String loadrules, String updtlog )
	{
		logger.trace_function();
		
		if( components != null )
		{
			/* components does not contain all or mod */
			if( !components.matches( rx_components ) )
			{
				System.err.println( "ERROR: Snapview->set_update( ): Legal values to components parameter is 'all' or 'mod'\n" );
				logger.log( "ERROR: Snapview->set_update( ): Legal values to components parameter is 'all' or 'mod'", "warning" );
				return 0;
			}
			
			loadrules = " -add_loadrules ";
			
			if( components.equalsIgnoreCase( "all" ) )
			{
				Iterator it = this.GetStream().GetLatestBls( false ).iterator();
				while( it.hasNext() )
				{
					String rule = ((Baseline)it.next()).GetComponent().GetRootDir();
					rule = rule.replaceAll( "^\\", " " );
					loadrules += rule;
				}
			}
			else
			{
				logger.log( "Added an extra \"\\\" to the cleartool commands" );
				// cleartool( "desc -fmt \%[project]p stream:" . $self->get_stream()->get_fqname() );
				String cmd = "desc -fmt \\%[project]p stream:" + this.GetStream().GetFQName();
				String result = Cleartool.run( cmd );
				
				String fqproject = result + "@" + this.GetPvob();
				
				// cleartool( "desc -fmt \%[mod_comps]p project:" . $fqproject );
				cmd = "desc -fmt \\%[mod_comps]p project:" + fqproject;
				result = Cleartool.run( cmd );
				
				String[] rs = result.split( "\\s+" );
				for( int i = 0 ; i < rs.length ; i++ )
				{
					//Component component = new Component( rs[i] + "@" + this.GetPvob(), true );
					Component component = Component.GetObject( rs[i] + "@" + this.GetPvob(), true );
					
					String rule = component.GetRootDir();
					rule = rule.replaceAll( "^\\", " " );
					loadrules += rule;
				}
			}
			
			if( loadrules != null )
			{
				loadrules = " -add_loadrules " + loadrules;
			}
		}
		
		String cwd = System.getProperty( "user.dir" );
		System.setProperty( "user.dir", this.GetViewroot() );
		
		// cleartool( "update -force -overwrite" . $loadrules );
		String cmd = "update -force -overwrite" + loadrules;
		String result = Cleartool.run( cmd );
		
		System.setProperty( "user.dir", cwd );
		
		/* Not complete, waiting for Lars. snapview.pm line 219 */
		if( updtlog != null )
		{
			String updtlogref = updtlog;
			//result = result.m
		}
		
		return 1;
	}
	
	/**
	 * Potential incomplete
	 * Waiting for Lars to complete the Perl code.
	 */
	public void Remove()
	{
		logger.trace_function();
		
		// cleartool( "rmview -force " . $self->get_viewroot() );
		String cmd = "rmview -force " + this.GetViewroot();
	}
	
	/**
	 * 
	 * @param baseline
	 */
	public void rebase( Baseline baseline )
	{
		logger.trace_function();

		if( baseline == null )
		{
			System.err.println( "required parameters are missing" );
			logger.log( "required parameters are missing", "error" );
			System.exit( 1 );
		}
		
		logger.log( "The third argument in the call to rebase, could be nonempty/empty/null?!" );
		this.GetStream().Rebase( baseline, this, " " );
		
	}
	
	public int Swipe( boolean printlist, String exclude_root )
	{
		logger.trace_function();
		
		ArrayList<File> view_root = new ArrayList<File>();
		
		if( exclude_root == null )
		{
			File[] files = new File( this.GetViewroot() ).listFiles();
			
			for( int i = 0 ; i < files.length ; i++ )
			{
				/* Only files, that are writable */
				if( !files[i].isDirectory() && files[i].canWrite() )
				{
					/* Exclude view.dat, *.work.txt */
					if( !files[i].getName().equalsIgnoreCase( "view.dat" ) || !files[i].getName().matches( ".*\\.work\\.txt$" ) )
					{
						view_root.add( files[i] );
					}
				}
			}
		}
		
		
		/* CHW: Project specific code! */
		// cleartool_qx( "ls -short -recurse -view_only " . $self->get_viewroot() . "\\265_Rc\\Rc_Ui\\Src\\Impl\\TextLib\\LanguageCompiler " . $self->get_viewroot() . "\\265_Rc\\Rc_SubSys\\265utils" );
		String cmd = "ls -short -recurse -view_only " + this.GetViewroot() + "\\265_Rc\\Rc_Ui\\Src\\Impl\\TextLib\\LanguageCompiler " + this.GetViewroot() + "\\265_Rc\\Rc_SubSys\\265utils";
		String result = Cleartool.run( cmd );
		logger.log( "Assuming the result is returned as a line separated list!" );
		String[] rs = result.split( linesep );
		
		logger.log( "Converting String[] to ArrayList<String> and sort()!" );
		view_root.addAll( Arrays.asList( Utilities.StringsToFiles( rs ) ) );
		Collections.sort( view_root );
		
		/* CHW: This shouldn't be necessary */
		ArrayList<File> priv = Utilities.GetNonEmptyArrayElements( view_root );
		int total = view_root.size();
		
		priv = Utilities.GetArrayElements( priv, ".*CHECKEDOUT$", false );
		int co = total - priv.size();
		total = priv.size();
		
		priv = Utilities.GetArrayElements( priv, ".*\\.keep$", false );
		int keep = total - priv.size();
		total = priv.size();
		
		priv = Utilities.GetArrayElements( priv, ".*\\.contrib", false );
		int ctr = total - priv.size();
		total = priv.size();
		
		if( printlist )
		{
		    System.out.println( "Files and directories that will be deleted:" + linesep + "+++++++++++++++++++++++++++++++++++++++++++" + linesep );
		    for( int i = 0 ; i < priv.size() ; i++ )
		    {
		    	System.out.println( "'" + priv.get( i ).toString().trim() + "'" + linesep );
		    }
		    System.out.println( "-------------------------------------------" + linesep );
		}
		
		/* CHW: MAYBE this is correct?! */
		int count = priv.size() + 1;
		
		System.out.println( "Found " + count + " view-private elements to delete" + linesep + "Not listing CHECKEDOUT files ("+co+")"+linesep+"Not listing .contrib files ("+ctr+")" + linesep + "Not listing .keep files ("+keep+")"+linesep );
		
		int dircount  = 0;
		int filecount = 0;
		ArrayList<File> dirs = new ArrayList<File>();
		
		for( int i = 0 ; i < priv.size() ; i++ )
		{
			if( priv.get( i ).isDirectory() )
			{
				dirs.add( priv.get( i ) );
			}
			else
			{
				if( !priv.get( i ).delete() )
				{
					logger.log( "Could not delete the view-private file \"" + priv.get( i ) + "\"", "warning" );
				}
				
				/* CHW: What does the "&&" do? $res && $filecount++; */
				filecount++;
				
			}
		}
		
		
		while( !dirs.isEmpty() )
		{
			File dir = dirs.remove( dirs.size() - 1 );
			if( !dir.delete() )
			{
				logger.log( "Could not delete the view-private directory \"" + dir.getName() + "\"", "warning" );
			}
			
			dircount++;
		}
		
		int total2 = dircount + filecount;
		System.out.println( "Succesfully deleted "+total+" view-private elements (" + filecount + " files and " + dircount + " directories)" + linesep );
		
		return ( total == count ) ? 1 : 0;
	}
	
	public Activity GetCact()
	{
		logger.trace_function();
		
		if( this.cact != null )
		{
			return this.cact;
		}
		
		// cleartool( 'lsactivity -s -cact -view ' . $self->{'viewtag'} );
		String cmd = "lsactivity -s -cact -view " + this.viewtag;
		String result = Cleartool.run( cmd );
		
		if( result.length() > 0 )
		{
			result = result.trim();
			//Activity act = new Activity( result + "@" + this.pvob, false );
			Activity act = Activity.GetObject( result + "@" + this.pvob, false );
			this.cact = act;
		}
		
		return this.cact;
	}
	
	public Activity SetActivity( Activity act )
	{
		logger.trace_function();
		
		// 'setactivity -view ' . $self->{'viewtag'} . ' ' . $act->get_fqname() );
		String cmd = "setactivity -view " + this.viewtag + " " + act.GetFQName();
		Cleartool.run( cmd );
		this.cact = act;
		
		return this.cact;
	}
	
	public boolean StaticViewExists( String viewtag )
	{
		logger.trace_function();
		
		String cmd = "lsview " + viewtag;
		try
		{
			Cleartool.run( cmd );
			return true;
		}
		catch( Exception e )
		{
			return false;
		}
	}
	
	
	public int StaticRegenViewDotDat( String dir, String tag )
	{
		logger.trace_function();
		
		String view_dat_pn = dir + filesep + "view.dat";
		//String[] result = Cleartool.run( "lsview -l " + tag, true );
		//ArrayList<String> list = Utilities.GetArrayElements( new ArrayList<String>( Arrays.asList( result ) ), "^View uuid:", true );
		logger.debug( "This is assumed to be a one liner result!" );
		String result = Cleartool.run( "lsview -l " + tag );
		Pattern pattern = Pattern.compile( "^View uuid:\\s*(.*)$" );
		Matcher match = pattern.matcher( result );
		
		/* No uuid is found */
		if( !match.find() )
		{
			logger.log( "No uuid found!", "warning" );
			System.err.println( "No uuid found!" );
			System.exit( 1 );
		}
		
		logger.debug( "The rx result is \"" + match.group( 1 ) + "\"" );
		
		/* Trim uuid */
		String viewuuid = match.group( 1 ).trim();
		viewuuid        = viewuuid.replaceAll( "[:\\.]", "" );
		
		// cleartool( 'lsview -uuid ' . $viewuuid ) );
		/* CHW: Is this correct? What is the return value of cleartool if the uuid does not exist? */
		logger.debug( "Running the uuid cleartool command!" );
		if( Cleartool.run( "lsview --uid " + viewuuid ).length() == 0 )
		{
			logger.log( "Could not read view uuid on view-tag " + tag, "warning" );
			System.err.println( "Could not read view uuid on view-tag " + tag );
			System.exit( 1 );
		}
		
		File f = new File( dir );
		if( f.exists() )
		{
			logger.warning( "The view-root " + dir + " already exist - reuse may be problematic" );
		}
		else
		{
			logger.log( "Making the directory: " + dir );
			f.mkdir();
		}
		
		try
		{
			logger.debug( "Trying to create/truncate the file: " + view_dat_pn );
			DataOutputStream out = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( view_dat_pn, false ) ) );
			out.writeBytes( "ws_oid:00000000000000000000000000000000 view_uuid:" + viewuuid );
			out.close();
		}
		catch ( FileNotFoundException e )
		{
			logger.log( "Could not create the file: " + view_dat_pn, "error" );
			System.err.println( "Could not create the file: " + view_dat_pn );
			
			e.printStackTrace();
			System.exit( 1 );
		}
		catch ( Exception e )
		{
			logger.log( "An error occured while working on the file: " + view_dat_pn, "error" );
			System.err.println( "An error occured while working on the file: " + view_dat_pn );
			
			e.printStackTrace();
			System.exit( 1 );
		}
		
		// cmd("attrib +h +r $view_dat_pn");
		Cmd.run( "attrib +h +r " + view_dat_pn );

		return 1;
	}
	
	
	/**
	 * 
	 * @return
	 */
	public String GetViewTag()
	{
		logger.trace_function();
		return this.viewtag;
	}
	
	public String GetPvob()
	{
		logger.trace_function();
		return this.pvob;
	}
	
	public String GetViewroot()
	{
		logger.trace_function();
		return this.viewroot;
	}
	
	public Stream GetStream()
	{
		logger.trace_function();
		return this.stream;
	}
	
	public ArrayList<Activity> GetActivities()
	{
		return this.stream.GetActivities();
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