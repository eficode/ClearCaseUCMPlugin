package net.praqma.hudson;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import net.praqma.clearcase.PVob;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.exceptions.UnableToLoadEntityException;
import net.praqma.clearcase.exceptions.ViewException;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.clearcase.ucm.utils.BuildNumber;
import net.praqma.clearcase.ucm.utils.VersionList;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.ucm.view.SnapshotView.LoadRules;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.ucm.view.SnapshotView.Components;
import net.praqma.hudson.exception.ScmException;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.Logger.LogLevel;
import net.praqma.util.debug.appenders.Appender;

public abstract class Util {

	private static Logger logger = Logger.getLogger();

	public static Project.PromotionLevel getLevel( String level ) {
		if( level.equalsIgnoreCase( "any" ) ) {
			return null;
		} else {
			return Project.getPlevelFromString( level );
		}
	}

	public Stream getDeveloperStream( String streamname, PVob pvob, Stream buildIntegrationStream, Baseline foundationBaseline ) throws ScmException {
		Stream devstream = null;

		try {
			if( Stream.streamExists( streamname + pvob ) ) {
				devstream = Stream.get( streamname, pvob );
			} else {
				devstream = Stream.create( buildIntegrationStream, streamname + pvob, true, foundationBaseline );
			}
			
			devstream.load();
		} catch( Exception e ) {
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
			try {
				act.load();
				buffer.append( "<activity>" );
				buffer.append( ( "<actName>" + act.getShortname() + "</actName>" ) );
				buffer.append( ( "<author>" + act.getUser() + "</author>" ) );
				//List<Version> versions = act.changeset.versions;
				VersionList versions = new VersionList( act.changeset.versions ).getLatest();
				String temp = null;
				for( Version v : versions ) {
					try {
						temp = "<file>" + v.getSFile() + " (" + v.getVersion() + ") user: " + v.blame() + "</file>";
					} catch( ClearCaseException e ) {
						logger.warning( "Could not generate log" );
					}
					buffer.append( temp );
				}
				buffer.append( "</activity>" );
			} catch( ClearCaseException e ) {
				logger.warning( "Unable to use activity \"" + act.getNormalizedName() + "\": " + e.getMessage() );
			}
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
		} catch( Exception e ) {
			throw new ScmException( "Could not make workspace (for viewroot " + viewroot.toString() + "). Cause: " + e.getMessage() );

		}

		hudsonOut.println( "[" + Config.nameShort + "] Determine if view tag exists" );
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
					hudsonOut.println( "[" + Config.nameShort + "] Trying to delete " + path );
					try {
						path.deleteRecursive();
					} catch( Exception e ) {
						throw new ScmException( "Unable to recursively prepare view root: " + e.getMessage() );
					}
					makeView( stream, workspace, listener, loadModule, viewroot, viewtag );
				}
			} catch( ClearCaseException ucmE ) {
				try {
					hudsonOut.println( "[" + Config.nameShort + "] Regenerating invalid view root" );
					UCMView.end( viewtag );
					SnapshotView.regenerateViewDotDat( viewroot, viewtag );
				} catch( ClearCaseException ucmEx ) {
					ucmEx.print( hudsonOut );
					throw new ScmException( "Could not make workspace - could not regenerate view: " + ucmEx.getMessage() + " Type: " + "" );
				} catch( IOException e ) {
					throw new ScmException( "Could not make workspace - could not regenerate view: " + e.getMessage() );
				}
			} catch( Exception e ) {
				hudsonOut.println( "[" + Config.nameShort + "] Failed making workspace: " + e.getMessage() );
				throw new ScmException( "Failed making workspace: " + e.getMessage() );
			}

			hudsonOut.println( "[" + Config.nameShort + "] Getting snapshotview" );
			try {
				snapview = SnapshotView.get( viewroot );
			} catch( ClearCaseException e ) {
				e.print( hudsonOut );
				throw new ScmException( "Could not get view for workspace. " + e.getMessage() );
			} catch( IOException e ) {
				throw new ScmException( "Could not get view for workspace. " + e.getMessage() );
			}
		} else {
			try {
				hudsonOut.println( "[" + Config.nameShort + "] Creating new view" );
				snapview = SnapshotView.create( stream, viewroot, viewtag );

				hudsonOut.println( "[" + Config.nameShort + "] Created new view in local workspace: " + viewroot.getAbsolutePath() );
			} catch( ClearCaseException e ) {
				e.print( hudsonOut );
				throw new ScmException( "View not found in this region, but views with viewtag '" + viewtag + "' might exist in the other regions. Try changing the region Hudson or the slave runs in." );
			} catch( IOException e ) {
				throw new ScmException( "Unable to create view: " + e.getMessage() );
			}
		}

		if( update ) {
			try {
				hudsonOut.println( "[" + Config.nameShort + "] Updating view using " + loadModule.toLowerCase() + " modules." );
				snapview.Update( true, true, true, false, new LoadRules( snapview, Components.valueOf( loadModule.toUpperCase() ) ) );
			} catch( ClearCaseException e ) {
				e.print( hudsonOut );
				if( e instanceof ViewException ) {
					if( ((ViewException)e).getType().equals( ViewException.Type.REBASING ) ) {
						hudsonOut.println( "The view is currently being used to rebase another stream" );
					}
				}
				throw new ScmException( "Could not update snapshot view. " + e.getMessage() );
			}
		}

		return snapview;
	}

	public static void initializeAppender( AbstractBuild<?, ?> build, Appender appender ) {
		appender.setSubscribeAll( false );
		appender.lockToCurrentThread();
		//appender.setTemplate( "%datetime %level %space %stack %message%newline" );

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
			} catch( Exception e ) {
				/* Just don't do it */
			}
		}
	}
	
}
