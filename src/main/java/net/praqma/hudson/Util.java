package net.praqma.hudson;

import hudson.model.BuildListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import net.praqma.clearcase.Cool;
import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.clearcase.ucm.utils.BaselineDiff;
import net.praqma.clearcase.ucm.utils.BuildNumber;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.ucm.view.SnapshotView.COMP;
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
	
	public static String createChangelog(BaselineDiff changes, Baseline bl) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		buffer.append("<changelog>");
		buffer.append("<changeset>");
		buffer.append("<entry>");
		buffer.append(("<blName>" + bl.getShortname() + "</blName>"));
		for (Activity act : changes) {
			buffer.append("<activity>");
			buffer.append(("<actName>" + act.getShortname() + "</actName>"));
			try {
				buffer.append(("<author>" + act.getUser() + "</author>"));
			} catch (UCMException e) {
				buffer.append(("<author>Unknown</author>"));
			}
			List<Version> versions = act.changeset.versions;
			String temp = null;
			for (Version v : versions) {
				try {
					temp = "<file>" + v.getSFile() + " (" + v.getRevision() + ") user: " + v.blame() + "</file>";
				} catch (UCMException e) {
					logger.warning( "Could not generate log" );
				}
				buffer.append(temp);
			}
			buffer.append("</activity>");
		}
		buffer.append("</entry>");
		buffer.append("</changeset>");

		buffer.append("</changelog>");

		return buffer.toString();
	}
	
	public static SnapshotView makeView(Stream stream, File workspace, BuildListener listener, String loadModule, File viewroot, String viewtag) throws ScmException {
		
		PrintStream hudsonOut = listener.getLogger();
		SnapshotView snapview = null;
		
        hudsonOut.println("[PUCM] View root: " + viewroot.getAbsolutePath());
        hudsonOut.println("[PUCM] View tag : " + viewtag);        

        try {
            if (viewroot.exists()) {
                hudsonOut.println("[PUCM] Reusing view root");
            } else {
                if (viewroot.mkdir()) {
                } else {
                    throw new ScmException("Could not create folder for view root:  " + viewroot.toString());
                }
            }
        } catch (Exception e) {
            throw new ScmException("Could not make workspace (for viewroot " + viewroot.toString() + "). Cause: " + e.getMessage());

        }

        if (UCMView.ViewExists(viewtag)) {
            hudsonOut.println("[PUCM] Reusing view tag");
            try {
                SnapshotView.ViewrootIsValid(viewroot);
            } catch (UCMException ucmE) {
                try {
                    hudsonOut.println("[PUCM] Regenerating invalid view root");
                    SnapshotView.RegenerateViewDotDat(viewroot, viewtag);
                } catch (UCMException ucmEx) {
                    if (ucmEx.stdout != null) {
                        hudsonOut.println(ucmEx.stdout);
                    }
                    throw new ScmException("Could not make workspace - could not regenerate view: " + ucmEx.getMessage() + " Type: " + "");
                }
            }

            hudsonOut.println("[PUCM] Getting snapshotview...");
            try {
                snapview = UCMView.GetSnapshotView(viewroot);
            } catch (UCMException e) {
                if (e.stdout != null) {
                    hudsonOut.println(e.stdout);
                }
                throw new ScmException("Could not get view for workspace. " + e.getMessage());
            }
        } else {
            try {
                snapview = SnapshotView.Create(stream, viewroot, viewtag);

                hudsonOut.println("[PUCM] Created new view in local workspace: " + viewroot.getAbsolutePath());
            } catch (UCMException e) {
                if (e.stdout != null) {
                    hudsonOut.println(e.stdout);
                }
                throw new ScmException("View not found in this region, but views with viewtag '" + viewtag
                        + "' might exist in the other regions. Try changing the region Hudson or the slave runs in.");
            }
        }

        try {
            hudsonOut.println("[PUCM] Updating view using " + loadModule.toLowerCase() + " modules...");
            snapview.Update(true, true, true, false, COMP.valueOf(loadModule.toUpperCase()), null);
        } catch (UCMException e) {
            if (e.stdout != null) {
                hudsonOut.println(e.stdout);
            }
            throw new ScmException("Could not update snapshot view. " + e.getMessage());
        }

        return snapview;
    }
}
