package net.praqma.hudson;

import java.util.ArrayList;
import java.util.List;

import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.hudson.exception.ScmException;
import net.praqma.utils.Debug;

public class Config
{
	protected static Debug logger = Debug.GetLogger();

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
			System.out.println( "PUCM is running on real ClearCase " );
		}
	}

	public static Stream devStream()
	{
		return UCMEntity.GetStream( "stream:Hudson_Server_dev@\\Cool_PVOB", false );
	}

	public static Stream getIntegrationStream() throws ScmException
	{
		Stream stream = null;
		Project project = null;
		try
		{
			project = UCMEntity.GetProject( "hudson@\\Cool_PVOB", false );

		}
		catch ( Exception e )
		{
			throw new ScmException( "Could not find project 'hudson' in \\Cool_PVOB - please check and retry" );
		}
		try
		{
			stream = project.GetStream();
		}
		catch ( Exception e )
		{
			throw new ScmException( "Could not get integration stream from " + project.GetShortname() );
		}

		return stream;
	}
	
	public static String getPvob(){
		return "@\\Cool_PVOB";
	}

}
