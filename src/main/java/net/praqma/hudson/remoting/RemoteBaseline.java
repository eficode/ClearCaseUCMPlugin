package net.praqma.hudson.remoting;

import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.Cool;
import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.utils.BuildNumber;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.hudson.exception.ScmException;
import net.praqma.util.debug.PraqmaLogger;
import net.praqma.util.debug.PraqmaLogger.Logger;

public class RemoteBaseline {

	private String versionFrom;
	private String buildnumberMajor;
	private String buildnumberMinor;
	private String buildnumberPatch;
	private String buildnumberSequenceSelector;
	private BuildListener listener;

	private String streamTarget;
	private String component;
	private String buildNumber;
	private String baselineName;
	private String viewroot;

	private Logger logger;

	public RemoteBaseline(Logger logger, BuildListener listener,
			String streamTarget, String component, String buildNumber,
			String baselineName, String viewroot, String versionFrom,
			String buildnumberMajor, String buildnumberMinor,
			String buildnumberPatch, String buildnumberSequenceSelector) {
		this.versionFrom = versionFrom;
		this.buildnumberMajor = buildnumberMajor;
		this.buildnumberMinor = buildnumberMinor;
		this.buildnumberPatch = buildnumberPatch;
		this.buildnumberSequenceSelector = buildnumberSequenceSelector;

		this.streamTarget = streamTarget;
		this.component = component;
		this.buildNumber = buildNumber;
		this.baselineName = baselineName;
		this.viewroot = viewroot;

		this.logger = logger;
	}

	public Integer invoke(File workspace, VirtualChannel channel)
			throws IOException {
		PraqmaLogger.getLogger(logger);
		/* Make sure that the local log file is not written */
		logger.setLocalLog(null);
		Cool.setLogger(logger);
		PrintStream out = listener.getLogger();
		UCM.setContext(UCM.ContextType.CLEARTOOL);
		
		out.println("[PUCM] Starting remote baseline");

		Stream target = null;
		try {
			logger.info("Trying to create target Stream " + streamTarget);
			target = UCMEntity.getStream(streamTarget);
		} catch (UCMException e) {
			logger.debug("could not create target Stream object: "
					+ e.getMessage());
			if (e.stdout != null) {
				out.println(e.stdout);
			}
			throw new IOException(
					"[PUCM] Could not create target Stream object: "
							+ e.getMessage());
		}

		Component component = null;
		try {
			component = UCMEntity.getComponent(this.component);
		} catch (UCMException e) {
			logger.debug("could not create Component object:" + e.getMessage());
			if (e.stdout != null) {
				out.println(e.stdout);
			}
			throw new IOException("[PUCM] Could not create Component object: "
					+ e.getMessage());
		}
		
		out.println("[PUCM] Created target stream + component");

		/* Trying to verify the build number attributes */
		/* Four level version number */
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
					number += this.buildNumber;
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
		
		out.println("[PUCM] Create the baseline");

		/* Create the baseline */
		Baseline newbl = null;
		System.out.println("The baseline is " + baselineName + number);
		try {
			logger.info("Creating new baseline " + baselineName + number);
			newbl = Baseline.create(baselineName + number, component, new File(
					viewroot), false, false);
			out.println("[PUCM] Created baseline " + baselineName + number);
		} catch (UCMException e) {
			logger.warning("Could not get view for workspace. "
					+ e.getMessage());
			out.println("[PUCM] Failed creating baseline " + baselineName
					+ number);
			if (e.stdout != null) {
				out.println(e.stdout);
			}
			throw new IOException("Could not create baseline: "
					+ e.getMessage());
		}
		
		out.println("[PUCM] DONE");

		return null;
	}
}
