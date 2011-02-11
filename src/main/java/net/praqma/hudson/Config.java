package net.praqma.hudson;

import java.util.ArrayList;
import java.util.List;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.hudson.exception.ScmException;
import net.praqma.util.debug.Logger;

public class Config
{
	protected static Logger logger = Logger.getLogger();

	private Config()
	{
	}

	public static List<String> getLevels()
	{
		logger.trace_function();
		List<String> levels = new ArrayList<String>();
		levels.add( "INITIAL" );
		levels.add( "BUILT" );
		levels.add( "TESTED" );
		levels.add( "RELEASED" );
		levels.add( "REJECTED" );
		return levels;
	}

	public static void setContext()
	{
		boolean useTestbase = false;
		if ( useTestbase )
		{
			/*
			 * Examples to use from testbase.xml: stream =
			 * "STREAM_TEST1@\PDS_PVOB" component = "COMPONENT_TEST1@\PDS_PVOB"
			 * Level to poll = "INITIAL"
			 */
			UCM.SetContext( UCM.ContextType.XML );
			System.out.println( "PUCM is running on a testbase" );
		}
		else
		{
			UCM.SetContext( UCM.ContextType.CLEARTOOL );
		}
	}

	/*Below method is obsolete - remove when everything works
	public static Stream devStream( String pvob ) throws ScmException
	{
		Stream devStream = null;
		try
		{
			devStream = UCMEntity.GetStream( "Hudson_Server_dev@" + pvob, false );
		}
		catch ( UCMException e )
		{
			throw new ScmException( "Could not get developer stream. " + e.getMessage() );
		}
		return devStream;
	}*/

	public static Stream getIntegrationStream( String pvob ) throws ScmException
	{
		Stream stream = null;
		Project project = null;
		try
		{
			project = UCMEntity.GetProject( "hudson" + pvob, false );

		}
		catch ( Exception e )
		{
			throw new ScmException( "Could not find project 'hudson' in " + pvob + ". You can install the Poject with: \"cleartool mkproject -c \"The special Hudson Project\" -in rootFolder@\\your_pvob hudson@\\your_pvob\"." );
		}
		
		try
		{
			stream = project.getIntegrationStream();
		}
		catch ( Exception e )
		{
			throw new ScmException( "Could not get integration stream from " + project.GetShortname() );
		}

		return stream;
	}

	public static String getPvob( Stream stream )
	{

		return "@" + stream.GetPvob();
	}

}
