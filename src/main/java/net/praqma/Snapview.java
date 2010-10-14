package net.praqma;


class Snapview extends ClearBase
{
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
	}
	
	
	
	public void StaticViewrootIsValid( String viewroot )
	{
		logger.trace_function();
		
		String viewdotdatpname = viewroot + "\\view.dat";
	}
}