package net.praqma.hudson;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.clearcase.ucm.utils.BuildNumber;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.ucm.view.SnapshotView.Components;
import net.praqma.hudson.exception.ScmException;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.Logger.LogLevel;
import net.praqma.util.debug.appenders.Appender;

public abstract class Util {

	private static Logger logger = Logger.getLogger();

	public static Project.Plevel getLevel( String level ) {
		if( level.equalsIgnoreCase( "any" ) ) {
			return null;
		} else {
			return Project.getPlevelFromString( level );
		}
	}

	public static String CreateNumber( BuildListener listener, int buildNumber, String versionFrom, String buildnumberMajor, String buildnumberMinor,
			String buildnumberPatch, String buildnumberSequenceSelector, Stream target, Component component ) throws IOException {

		PrintStream out = listener.getLogger();

		String number = "";
		/* Get version number from project+component */
		if (versionFrom.equals("project")) {
			logger.debug("Using project setting");

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

			/* Verify settings */
			if(buildnumberMajor.length() > 0 &&
			   buildnumberMinor.length() > 0 &&
			   buildnumberPatch.length() > 0) {

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

	public Stream getDeveloperStream( String streamname, String pvob, Stream buildIntegrationStream, Baseline foundationBaseline ) throws ScmException {
		Stream devstream = null;

		try {
			if( Stream.streamExists( streamname + pvob ) ) {
				devstream = Stream.getStream( streamname + pvob, false );
			} else {
				devstream = Stream.create( buildIntegrationStream, streamname + pvob, true, foundationBaseline );
			}
		} catch (Exception e) {
			throw new ScmException( "Could not get stream: " + e.getMessage() );
		}

		return devstream;
	}

	public static String createChangelog( List<Activity> changes, Baseline bl ) {
		StringBuffer buffer = new StringBuffer();

		buffer.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
		buffer.append( "<changelog>" );
		buffer.append( "<changeset>" );
		buffer.append( "<entry>" );
		buffer.append( ( "<blName>" + bl.getShortname() + "</blName>" ) );
		for( Activity act : changes ) {
			buffer.append( "<activity>" );
			buffer.append( ( "<actName>" + act.getShortname() + "</actName>" ) );
			try {
				buffer.append( ( "<author>" + act.getUser() + "</author>" ) );
			} catch (UCMException e) {
				buffer.append( ( "<author>Unknown</author>" ) );
			}
			List<Version> versions = act.changeset.versions;
			String temp = null;
			for( Version v : versions ) {
				try {
					temp = "<file>" + v.getSFile() + " (" + v.getVersion() + ") user: " + v.blame() + "</file>";
				} catch (UCMException e) {
					logger.warning( "Could not generate log" );
				}
				buffer.append( temp );
			}
			buffer.append( "</activity>" );
		}
		buffer.append( "</entry>" );
		buffer.append( "</changeset>" );

		buffer.append( "</changelog>" );

		return buffer.toString();
	}

	public static SnapshotView makeView( Stream stream, File workspace, BuildListener listener, String loadModule, File viewroot, String viewtag ) throws ScmException {
		return makeView( stream, workspace, listener, loadModule, viewroot, viewtag, true );
	}

	public static SnapshotView makeView( Stream stream, File workspace, BuildListener listener, String loadModule, File viewroot, String viewtag, boolean update ) throws ScmException {

		PrintStream hudsonOut = listener.getLogger();
		SnapshotView snapview = null;

		hudsonOut.println( "[" + Config.nameShort + "] View root: " + viewroot.getAbsolutePath() );
		hudsonOut.println( "[" + Config.nameShort + "] View tag : " + viewtag );

		boolean pathExists = false;

		/*
		 * Determine if there is a view path if not, create it
		 */
		try {
			if( viewroot.exists() ) {
				pathExists = true;
				hudsonOut.println( "[" + Config.nameShort + "] Reusing view root" );
			} else {
				if( viewroot.mkdir() ) {
				} else {
					throw new ScmException( "Could not create folder for view root:  " + viewroot.toString() );
				}
			}
		} catch (Exception e) {
			throw new ScmException( "Could not make workspace (for viewroot " + viewroot.toString() + "). Cause: " + e.getMessage() );

		}

		/* Only do this is if the path existed */

		if( UCMView.viewExists( viewtag ) ) {
			hudsonOut.println( "[" + Config.nameShort + "] Reusing view tag" );
			try {
				String vt = SnapshotView.viewrootIsValid( viewroot );
				hudsonOut.println( "[" + Config.nameShort + "] UUID resulted in " + vt );
				/* Not the correct view tag given the view */
				if( !vt.equals( viewtag ) && pathExists ) {
					hudsonOut.println( "[" + Config.nameShort + "] View tag is not the same as " + vt );
					/* Delete view */
					FilePath path = new FilePath( viewroot );
					try {
						path.deleteRecursive();
					} catch (Exception e) {
						throw new ScmException( "Unable to recursively prepare view root: " + e.getMessage() );
					}
					makeView( stream, workspace, listener, loadModule, viewroot, viewtag );
				}
			} catch (UCMException ucmE) {
				try {
					hudsonOut.println( "[" + Config.nameShort + "] Regenerating invalid view root" );
					SnapshotView.endView( viewtag );
					SnapshotView.regenerateViewDotDat( viewroot, viewtag );
				} catch (UCMException ucmEx) {
					if( ucmEx.stdout != null ) {
						hudsonOut.println( ucmEx.stdout );
					}
					throw new ScmException( "Could not make workspace - could not regenerate view: " + ucmEx.getMessage() + " Type: " + "" );
				}
			}

			hudsonOut.println( "[" + Config.nameShort + "] Getting snapshotview" );
			try {
				snapview = UCMView.getSnapshotView( viewroot );
			} catch (UCMException e) {
				if( e.stdout != null ) {
					hudsonOut.println( e.stdout );
				}
				throw new ScmException( "Could not get view for workspace. " + e.getMessage() );
			}
		} else {
			try {
				snapview = SnapshotView.create( stream, viewroot, viewtag );

				hudsonOut.println( "[" + Config.nameShort + "] Created new view in local workspace: " + viewroot.getAbsolutePath() );
			} catch (UCMException e) {
				if( e.stdout != null ) {
					hudsonOut.println( e.stdout );
				}
				throw new ScmException( "View not found in this region, but views with viewtag '" + viewtag + "' might exist in the other regions. Try changing the region Hudson or the slave runs in." );
			}
		}

		if( update ) {
			try {
				hudsonOut.println( "[" + Config.nameShort + "] Updating view using " + loadModule.toLowerCase() + " modules." );
				snapview.Update( true, true, true, false, Components.valueOf( loadModule.toUpperCase() ), null );
			} catch (UCMException e) {
				if( e.stdout != null ) {
					hudsonOut.println( e.stdout );
				}
				throw new ScmException( "Could not update snapshot view. " + e.getMessage() );
			}
		}

		return snapview;
	}

	public static void initializeAppender( AbstractBuild<?, ?> build, Appender appender ) {
		appender.setSubscribeAll( true );

		/* Log classes */
		if( build.getBuildVariables().get( Config.logVar ) != null ) {
			String[] is = build.getBuildVariables().get( Config.logVar ).toString().split( "," );
			logger.fatal( "Logging " + is );
			for( String i : is ) {
				appender.subscribe( i.trim() );
			}
		}

		/* Log all */
		if( build.getBuildVariables().get( Config.logAllVar ) != null ) {
			logger.fatal( "Logging all" );
			appender.setSubscribeAll( true );
		}

		/* Log level */
		if( build.getBuildVariables().get( Config.levelVar ) != null ) {
			try {
				LogLevel level = LogLevel.valueOf( build.getBuildVariables().get( Config.levelVar ) );
				logger.fatal( "Logging " + level );
				appender.setMinimumLevel( level );
			} catch (Exception e) {
				/* Just don't do it */
			}
		}
	}
}
