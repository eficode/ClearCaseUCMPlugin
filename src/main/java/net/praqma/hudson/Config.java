package net.praqma.hudson;

import java.util.List;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.CleartoolException;
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

    /*Below method is obsolete - remove when everything works*/
    /**
     * @deprecated
     */
    public static Stream devStream(String pvob) throws ScmException {
        Stream devStream = null;
        try {
            devStream = Stream.get("Hudson_Server_dev@" + pvob);
            devStream.load();
        } catch (ClearCaseException e) {
            throw new ScmException("Could not get developer stream. " + e.getMessage());
        }
        return devStream;
    }

    public static Stream getIntegrationStream(Baseline bl, String buildProject) throws ScmException {
        Stream stream = null;
        Project project = null;


        /* If the build project was not given as a parameter to the job, try to find
         * hudson, Hudson, jenkins or Jenkins */
        if (buildProject == null) {
            try {
                project = UCMEntity.getProject("hudson", bl.getPvob());
            } catch (CleartoolException eh) {
                try {
                    project = UCMEntity.getProject("Hudson@" + bl.getPvobString());
                } catch (CleartoolException eH) {
                    try {
                        project = UCMEntity.getProject("jenkins@" + bl.getPvobString());
                    } catch (CleartoolException ej) {
                        try {
                            project = UCMEntity.getProject("Jenkins@" + bl.getPvobString());
                        } catch (CleartoolException eJ) {
                            logger.warning("The build Project was not found.");

                            /* Use the integration stream */
                            try {
                                project = bl.getStream().getProject();
                            } catch (CleartoolException ucme) {
                                throw new ScmException("Could not get the build Project.");
                            }
                        }
                    }
                }
            }
        } else {
            try {
                project = UCMEntity.getProject(buildProject + "@" + bl.getPvobString(), false);
            } catch (Exception e) {
                //throw new ScmException( "Could not find project 'hudson' in " + pvob + ". You can install the Poject with: \"cleartool mkproject -c \"The special Hudson Project\" -in rootFolder@\\your_pvob hudson@\\your_pvob\"." );
                logger.warning("The build Project was not found.");

                try {
                    project = bl.getStream().getProject();
                } catch (CleartoolException ucme) {
                    throw new ScmException("Could not get the Project.");
                }
            }
        }

        try {
            stream = project.getIntegrationStream();
        } catch (Exception e) {
            throw new ScmException("Could not get integration stream from " + project.getShortname());
        }

        return stream;
    }

    public static String getPvob(Stream stream) {

        return "@" + stream.getPvobString();
    }
}
