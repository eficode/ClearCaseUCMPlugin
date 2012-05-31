package net.praqma.hudson;

import java.util.List;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.exceptions.UnableToLoadEntityException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.hudson.exception.ScmException;
import net.praqma.util.debug.Logger;

public class Config {

	public static String nameShort = "CCUCM";
	public static String nameLong = "ClearCase UCM";
	public static String logVar = "ccucm_log";
	public static String levelVar = "ccucm_loglevel";
	public static String logAllVar = "ccucm_logall";

	protected static Logger logger = Logger.getLogger();

	private Config() {
	}

	public static List<String> getLevels() {
		List<String> levels = Project.getPromotionLevels();
		levels.add( "ANY" );
		return levels;
	}

	/* Below method is obsolete - remove when everything works */
	/**
	 * @deprecated
	 */
	public static Stream devStream( String pvob ) throws ScmException {
		Stream devStream = null;
		try {
			devStream = Stream.get( "Hudson_Server_dev@" + pvob );
			devStream.load();
		} catch( ClearCaseException e ) {
			throw new ScmException( "Could not get developer stream. " + e.getMessage() );
		}
		return devStream;
	}

	public static Stream getIntegrationStream( Baseline bl, String buildProject ) throws ScmException {
		Stream stream = null;
		Project project = null;

		/*
		 * If the build project was not given as a parameter to the job, try to
		 * find hudson, Hudson, jenkins or Jenkins
		 */
		if( buildProject == null ) {
			try {
				project = Project.get( "hudson", bl.getPVob() );
			} catch( Exception eh ) {
				try {
					project = Project.get( "Hudson", bl.getPVob() );
				} catch( Exception eH ) {
					try {
						project = Project.get( "jenkins", bl.getPVob() );
					} catch( Exception ej ) {
						try {
							project = Project.get( "Jenkins", bl.getPVob() );
						} catch( Exception eJ ) {
							logger.warning( "The build Project was not found." );
							project = bl.getStream().getProject();
						}
					}
				}
			}
		} else {
			try {
				project = Project.get( buildProject, bl.getPVob() );
			} catch( Exception e ) {
				//throw new ScmException( "Could not find project 'hudson' in " + pvob + ". You can install the Poject with: \"cleartool mkproject -c \"The special Hudson Project\" -in rootFolder@\\your_pvob hudson@\\your_pvob\"." );
				logger.warning( "The build Project was not found." );

				project = bl.getStream().getProject();
			}
		}

		try {
			project.load();
		} catch( ClearCaseException e1 ) {
			project = bl.getStream().getProject();
			logger.warning( "The project could not be loaded, using " + project.getNormalizedName() );
		}

		try {
			stream = project.getIntegrationStream();
		} catch( Exception e ) {
			throw new ScmException( "Could not get integration stream from " + project.getShortname() );
		}

		return stream;
	}

}
