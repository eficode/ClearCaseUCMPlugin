package net.praqma.hudson;

import java.util.List;
import java.util.logging.Logger;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.hudson.exception.ScmException;

public class Config {

	public static String nameShort = "CCUCM";
	public static String nameLong = "ClearCase UCM";    

	protected static final Logger logger = Logger.getLogger( Config.class.getName() );

	private Config() { }

	public static List<String> getLevels() {
		List<String> levels = Project.getPromotionLevels();
		return levels;
	}

	public static Stream getIntegrationStream( Baseline bl, String buildProject ) throws ScmException {
		Stream stream = null;
		Project project = null;

		if( buildProject == null ) {
			try {
				project = Project.get( "hudson", bl.getPVob() ).load();
			} catch( Exception eh ) {
				try {
					project = Project.get( "Hudson", bl.getPVob() ).load();
				} catch( Exception eH ) {
					try {
						project = Project.get( "jenkins", bl.getPVob() ).load();
					} catch( Exception ej ) {
						try {
							project = Project.get( "Jenkins", bl.getPVob() ).load();
						} catch( Exception eJ ) {
							logger.fine( "Using current project as build project" );
							try {
								project = bl.getStream().load().getProject();
							} catch( Exception e ) {
								throw new ScmException( "Could not get a build Project", null );
							}
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
			throw new ScmException( "Could not get integration stream from " + project.getShortname(), e );
		}

		return stream;
	}

}
