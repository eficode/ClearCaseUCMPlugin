package net.praqma.hudson;

import hudson.model.BuildListener;

import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.Cool;
import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.utils.BuildNumber;
import net.praqma.hudson.exception.ScmException;
import net.praqma.util.debug.PraqmaLogger;
import net.praqma.util.debug.PraqmaLogger.Logger;

public abstract class Util {
	
	private static Logger logger;
	
	static {
		logger = PraqmaLogger.getLogger( );
		logger.setLocalLog( null );
		Cool.setLogger( logger );
		UCM.setContext( UCM.ContextType.CLEARTOOL );
	}

	public static String CreateNumber( BuildListener listener, int buildNumber, String versionFrom, String buildnumberMajor, String buildnumberMinor,
			String buildnumberPatch, String buildnumberSequenceSelector, Stream target, Component component ) throws IOException {
		
		PrintStream out = listener.getLogger();
		
		String number = "";
		/* Get version number from project+component */
		if (versionFrom.equals("project")) {
			logger.debug("Using project setting");
			out.println("[PUCM] Using project");

			try {
				Project project = target.getProject();
				number = BuildNumber.getBuildNumber(project);
			} catch (UCMException e) {
				logger.warning("Could not get four level version");
				logger.warning(e);
				if (e.stdout != null) {
					out.println(e.stdout);
				}
				throw new IOException("Could not get four level version: "
						+ e.getMessage());
			}
		}
		/* Get version number from project+component */
		else if (versionFrom.equals("settings")) {
			logger.debug("Using settings");
			out.println("[PUCM] Using settings");

			/* Verify settings */
			if (buildnumberMajor.length() > 0 && buildnumberMinor.length() > 0
					&& buildnumberPatch.length() > 0) {
				number = "__" + buildnumberMajor + "_" + buildnumberMinor + "_"
						+ buildnumberPatch + "_";

				/* Get the sequence number from the component */
				if (buildnumberSequenceSelector.equals("component")) {

					logger.debug("Get sequence from project " + component);

					try {
						Project project = target.getProject();
						int seq = BuildNumber.getNextBuildSequence(project);
						number += seq;
					} catch (UCMException e) {
						logger.warning("Could not get sequence number from component");
						logger.warning(e);
						if (e.stdout != null) {
							out.println(e.stdout);
						}
						throw new IOException(
								"Could not get sequence number from component: "
										+ e.getMessage());
					}
				}
				/* Use the current build number from jenkins */
				else {
					logger.debug("Getting sequence from build number");
					number += buildNumber;
				}
			} else {
				logger.warning("Creating error message");
				String error = (buildnumberMajor.length() == 0 ? "Major missing. "
						: "")
						+ (buildnumberMinor.length() == 0 ? "Minor missing. "
								: "")
						+ (buildnumberPatch.length() == 0 ? "Patch missing. "
								: "");

				logger.warning("Missing information in build numbers: " + error);
				throw new IOException("Missing build number information: "
						+ error);
			}
		} else {
			/* No op = none */
		}
		
		return number;
	}
	
	public Stream getDeveloperStream(String streamname, String pvob, Stream buildIntegrationStream, Baseline foundationBaseline) throws ScmException {
		Stream devstream = null;

		try {
			if (Stream.streamExists(streamname + pvob)) {
				devstream = Stream.getStream(streamname + pvob, false);
			} else {
				devstream = Stream.create(buildIntegrationStream, streamname + pvob, true, foundationBaseline);
			}
		} catch (Exception e) {
			throw new ScmException("Could not get stream: " + e.getMessage());
		}

		return devstream;
	}
}
