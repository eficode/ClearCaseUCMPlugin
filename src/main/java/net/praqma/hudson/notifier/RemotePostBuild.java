package net.praqma.hudson.notifier;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.Cool;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Tag;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.hudson.Config;
import net.praqma.util.debug.PraqmaLogger;
import net.praqma.util.debug.PraqmaLogger.Logger;

/**
 * 
 * @author wolfgang
 * 
 */
class RemotePostBuild implements FileCallable<Status> {
	private static final long serialVersionUID = 1L;
	private String displayName;
	private String buildNumber;

	private Result result;

	private String baseline;
	private String stream;

	private boolean makeTag = false;
	private int promote = 0;
	private boolean promoteUnstable = false;
	private boolean recommend = false;
	private Status status;
	private BuildListener listener;

	private String id = "";

	private Logger logger = null;
	private PrintStream hudsonOut = null;

	private Pipe pipe = null;
	private BufferedWriter bw = null;
	private PipedOutputStream pout = null;

	public RemotePostBuild( Result result, Status status, BuildListener listener,
							/* Values for */
							boolean makeTag, int promote, boolean recommended,
							/* Common values */
							String baseline, String stream, String displayName, String buildNumber, Logger logger/*
																												 * ,
																												 * PipedOutputStream
																												 * pout
																												 */, Pipe pipe ) {
		this.displayName = displayName;
		this.buildNumber = buildNumber;

		this.id = "[" + displayName + "::" + buildNumber + "]";

		this.baseline = baseline;
		this.stream = stream;

		this.result = result;

		this.makeTag = makeTag;
		this.promote = promote;
		this.recommend = recommended;

		this.status = status;
		this.listener = listener;

		this.logger = logger;
		this.pipe = pipe;
		/*
		 * this.pout = pout;
		 */
	}

	public Status invoke( File workspace, VirtualChannel channel ) throws IOException {
		PraqmaLogger.getLogger( logger );
		/* Make sure that the local log file is not written */
		logger.setLocalLog( null );
		Cool.setLogger( logger );
		hudsonOut = listener.getLogger();
		UCM.setContext( UCM.ContextType.CLEARTOOL );

		String newPLevel = "";

		status.addToLog( logger.info( "Starting PostBuild task" ) );

		/* Create the baseline object */
		Baseline baseline = null;
		try {
			baseline = UCMEntity.getBaseline( this.baseline );
		} catch (UCMException e) {
			status.addToLog( logger.debug( id + "could not create Baseline object:" + e.getMessage() ) );
			throw new IOException( "[" + Config.nameShort + "] Could not create Baseline object: " + e.getMessage() );
		}

		/* Create the stream object */
		Stream stream = null;
		try {
			stream = UCMEntity.getStream( this.stream );
		} catch (UCMException e) {
			status.addToLog( logger.debug( id + "could not create Stream object:" + e.getMessage() ) );
			throw new IOException( "[" + Config.nameShort + "] Could not create Stream object: " + e.getMessage() );
		}

		status.addToLog( logger.warning( id + "Stream and component created" ) );

		/* Create the Tag object */
		Tag tag = null;
		if( makeTag ) {
			try {
				// Getting tag to set buildstatus
				tag = baseline.getTag( this.displayName, this.buildNumber );
				status.setTagAvailable( true );
			} catch (UCMException e) {
				hudsonOut.println( "[" + Config.nameShort + "] Could not get Tag: " + e.getMessage() );
				status.addToLog( logger.warning( id + "Could not get Tag: " + e.getMessage() ) );
			}
		}

		/* The build was a success and the deliver did not fail */
		if( result.equals( Result.SUCCESS ) && status.isStable() ) {
			if( status.isTagAvailable() ) {
				tag.setEntry( "buildstatus", "SUCCESS" );
			}

			if( promote > CCUCMNotifier.__NO_PROMOTE ) {
				try {
					Project.Plevel pl = baseline.promote();
					status.setPromotedLevel( pl );
					status.setPLevel( true );
					hudsonOut.println( "[" + Config.nameShort + "] Baseline promoted to " + baseline.getPromotionLevel( true ).toString() + "." );
				} catch (UCMException e) {
					status.setStable( false );
					/*
					 * as it will not make sense to recommend if we cannot
					 * promote, we do this:
					 */
					if( recommend ) {
						status.setRecommended( false );
						hudsonOut.println( "[" + Config.nameShort + "] Could not promote baseline and will not recommend. " + e.getMessage() );
						status.addToLog( logger.warning( id + "Could not promote baseline and will not recommend. " + e.getMessage() ) );
					} else {
						/*
						 * As we will not recommend if we cannot promote, it's
						 * ok to break method here
						 */
						hudsonOut.println( "[" + Config.nameShort + "] Could not promote baseline. " + e.getMessage() );
						status.addToLog( logger.warning( id + "Could not promote baseline. " + e.getMessage() ) );
					}
				}
			}
			/* Recommend the Baseline */
			if( recommend ) {
				try {
					if( status.isPLevel() ) {
						stream.recommendBaseline( baseline );
						hudsonOut.println( "[" + Config.nameShort + "] Baseline " + baseline.getShortname() + " is now recommended." );
					}
				} catch ( UCMException e ) {
					status.setStable( false );
					status.setRecommended( false );
					hudsonOut.println( "[" + Config.nameShort + "] Could not recommend Baseline: " + e.getMessage() );
					status.addToLog( logger.warning( id + "Could not recommend baseline: " + e.getMessage() ) );
				}
			}
		}
		/* The build failed or the deliver failed */
		else {
			/* Do not set as recommended at all */
			if( recommend ) {
				status.setRecommended( false );
			}

			/* The build failed */
			if( result.equals( Result.FAILURE ) ) {
				hudsonOut.println( "[" + Config.nameShort + "] Build failed." );

				if( status.isTagAvailable() ) {
					tag.setEntry( "buildstatus", "FAILURE" );
				}

				if( promote > CCUCMNotifier.__NO_PROMOTE ) {
					try {
						status.addToLog( logger.warning( id + "Demoting baseline" ) );
						Project.Plevel pl = baseline.demote();
						status.setPromotedLevel( pl );
						status.setPLevel( true );
						hudsonOut.println( "[" + Config.nameShort + "] Baseline is " + baseline.getPromotionLevel( true ).toString() + "." );
					} catch (Exception e) {
						status.setStable( false );
						// throw new NotifierException(
						// "Could not demote baseline. " + e.getMessage() );
						hudsonOut.println( "[" + Config.nameShort + "] Could not demote baseline. " + e.getMessage() );
						status.addToLog( logger.warning( id + "Could not demote baseline. " + e.getMessage() ) );
					}
				}
			}
			/*
			 * The build is unstable, or something in the middle.... TODO Maybe
			 * not else if
			 */
			else if( !result.equals( Result.FAILURE ) ) {
				if( status.isTagAvailable() ) {
					tag.setEntry( "buildstatus", "UNSTABLE" );
				}

				if( promote > CCUCMNotifier.__NO_PROMOTE ) {
					try {
						Project.Plevel pl = Project.Plevel.INITIAL;

						if( promote == CCUCMNotifier.__PROMOTE_UNSTABLE ) {
							pl = baseline.promote();
							hudsonOut.println( "[" + Config.nameShort + "] Baseline is promoted, even though the build is unstable." );
						} else {
							pl = baseline.demote();
						}
						status.setPromotedLevel( pl );
						status.setPLevel( true );
						hudsonOut.println( "[" + Config.nameShort + "] Baseline is " + baseline.getPromotionLevel( true ).toString() + "." );
					} catch (Exception e) {
						status.setStable( false );
						hudsonOut.println( "[" + Config.nameShort + "] Could not demote baseline. " + e.getMessage() );
						status.addToLog( logger.warning( id + "Could not demote baseline. " + e.getMessage() ) );
					}
				}
				/* Recommend the Baseline */
				if( recommend ) {
					try {
						if( status.isPLevel() ) {
							stream.recommendBaseline( baseline );
							hudsonOut.println( "[" + Config.nameShort + "] Baseline " + baseline.getShortname() + " is now recommended." );
						}
					} catch (Exception e) {
						status.setStable( false );
						status.setRecommended( false );
						hudsonOut.println( "[" + Config.nameShort + "] Could not recommend baseline. Reason: " + e.getMessage() );
						status.addToLog( logger.warning( id + "Could not recommend baseline. Reason: " + e.getMessage() ) );
					}
				}

			}
			/* Result not handled by CCUCM */
			else {
				tag.setEntry( "buildstatus", result.toString() );
				status.addToLog( logger.log( id + "Buildstatus (Result) was " + result + ". Not handled by plugin." ) );
				hudsonOut.println( "[" + Config.nameShort + "] Baseline not changed. Buildstatus: " + result );
			}
		}

		/* Persist the Tag */
		if( makeTag ) {
			if( tag != null ) {
				try {
					tag = tag.persist();
					hudsonOut.println( "[" + Config.nameShort + "] Baseline now marked with tag: \n" + tag.stringify() );
				} catch (Exception e) {
					hudsonOut.println( "[" + Config.nameShort + "] Could not change tag in ClearCase. Contact ClearCase administrator to do this manually." );
				}
			} else {
				logger.warning( id + "Tag object was null" );
				hudsonOut.println( "[" + Config.nameShort + "] Tag object was null, tag not set." );
			}
		}

		try {
			newPLevel = baseline.getPromotionLevel( true ).toString();
		} catch (UCMException e) {
			logger.log( id + " Could not get promotionlevel." );
			hudsonOut.println( "[" + Config.nameShort + "] Could not get promotion level." );
		}

		status.setBuildDescr( setDisplaystatus( newPLevel, baseline.getShortname() ) );

		status.addToLog( logger.warning( id + "Remote post build finished normally" ) );

		/*
		 * if( out != null ) { out.close(); }
		 */

		return status;
	}

	private String setDisplaystatus( String plevel, String fqn ) {
		String s = "";

		// Get shortname
		s += "<small>" + fqn + "</small>";

		// Get plevel:
		s += "<BR/><small>" + plevel + "</small>";

		if( recommend ) {
			if( status.isRecommended() ) {
				s += "<BR/><B><small>Recommended</small></B>";
			} else {
				s += "<BR/><B><small>Could not recommend</small></B>";
			}
		}
		return s;
	}

}