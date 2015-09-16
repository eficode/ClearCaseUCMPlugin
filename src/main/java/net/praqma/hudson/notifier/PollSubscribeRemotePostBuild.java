package net.praqma.hudson.notifier;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.TagException;
import net.praqma.clearcase.exceptions.TagException.Type;
import net.praqma.clearcase.exceptions.UnableToPromoteBaselineException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Tag;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.Config;
import net.praqma.hudson.scm.Unstable;
import net.praqma.hudson.scm.pollingmode.PollSubscribeMode;
import org.jenkinsci.remoting.RoleChecker;

/**
 * 
 * @author wolfgang
 * 
 */
public class PollSubscribeRemotePostBuild implements FileCallable<Status> {

	private static final long serialVersionUID = 1L;
	private final String displayName;
	private final String buildNumber;
	private final Result result;
	private final Baseline sourcebaseline;
	private final Baseline targetbaseline;
	private final Stream sourcestream;
	private final Stream targetstream;
	private boolean makeTag = false;
	private boolean recommend = false;
	private final Status status;
	private final BuildListener listener;
	private PrintStream hudsonOut = null;
	private Unstable unstable;
    private final List<Baseline> consideredBaselines;
    private PollSubscribeMode mode;

    //Any in this case...means that we do not promote the source baseline (The one that triggered polling)
    private boolean isPromotionSkipped;
    
    public PollSubscribeRemotePostBuild setUnstable(Unstable unstable) {
        this.unstable = unstable;
        return this;
    }
    
    public PollSubscribeRemotePostBuild setSkipPromotion(Boolean skipPromote) {
        this.isPromotionSkipped = skipPromote;
        return this;
    }
    
    public PollSubscribeRemotePostBuild setDoRecommend(Boolean doRecommend) {
        this.recommend = doRecommend;
        return this;
    }
    
    public PollSubscribeRemotePostBuild setDoMakeTag(Boolean doMakeTag) {
        this.makeTag = doMakeTag;
        return this;
    }
    
    public PollSubscribeRemotePostBuild setMode(PollSubscribeMode mode) {
        this.mode = mode;
        return this;
    }

	public PollSubscribeRemotePostBuild( Result result, Status status, BuildListener listener, Baseline sourcebaseline, Baseline targetbaseline, Stream sourcestream, Stream targetstream, String displayName, String buildNumber, List<Baseline> consideredBaselines) {
		this.displayName = displayName;
		this.buildNumber = buildNumber;
		this.sourcebaseline = sourcebaseline;
		this.targetbaseline = targetbaseline;
		this.sourcestream = sourcestream;
		this.targetstream = targetstream;
		this.result = result;
		this.status = status;
		this.listener = listener;
        this.consideredBaselines = consideredBaselines;
	}
    
    @Override
	public Status invoke( File workspace, VirtualChannel channel ) throws IOException {
		hudsonOut = listener.getLogger();
		Logger logger = Logger.getLogger(PollSubscribeRemotePostBuild.class.getName()  );
		logger.info( "Starting PostBuild task" );
		String noticeString = "";

		/* Create the Tag object */
		Tag tag = null;
		if( makeTag ) {
            logger.fine( "Trying to get/make tag" );
			try {
				// Getting tag to set buildstatus
				tag = sourcebaseline.getTag( this.displayName, this.buildNumber );
				status.setTagAvailable( true );
			} catch( Exception e ) {
				hudsonOut.println( "Unable to get tag for " + sourcebaseline.getNormalizedName() );
			}
		}

		/* The build was a success and the deliver did not fail */
		if( result.equals( Result.SUCCESS ) ) {
            
			status.setRecommended( true );

			if( status.isTagAvailable() ) {
                logger.fine( "Tag is available" );
				if( status.isStable() ) {
					tag.setEntry( "buildstatus", "SUCCESS" );
				} else {
					tag.setEntry( "buildstatus", "UNSTABLE" );
				}
			}

			try {
                /* Promote baseline, not any */
                if( !isPromotionSkipped ) {
                    if( hasRemoteMastership() ) {
                        logger.fine( "Source/Target masterships differ, hasRemoteMastership = true" );
                        printPostedOutput( sourcebaseline );
                        noticeString = "*";
                    } else {
                        Project.PromotionLevel pl;
                        if( !status.isStable() && !unstable.treatSuccessful() ) {
                            /* Treat the not stable build as unsuccessful */
                            pl = sourcebaseline.reject();
                            hudsonOut.println( CCUCMNotifier.logShortPrefix + " Baseline " + sourcebaseline.getShortname() + " is " + pl.toString() + "." );
                        } else {
                            /* Treat the build as successful */
                            pl = sourcebaseline.promote();
                            
                            /* Set each baseline that was considered in the calculation */
                            if(mode.isCascadePromotion()) {
                                for(Baseline bl : consideredBaselines) {
                                    try {
                                        bl.setPromotionLevel(pl);
                                    } catch (UnableToPromoteBaselineException ex) {
                                        throw new IOException( String.format("Unable to promote baseline %s", bl.getFullyQualifiedName()), ex);
                                    }
                                }
                            }
                            hudsonOut.print( CCUCMNotifier.logShortPrefix + " Baseline " + sourcebaseline.getShortname() + " promoted to " + pl.toString() );
                            if( !status.isStable() ) {
                                hudsonOut.println( ", even though the build is unstable." );
                            } else {
                                hudsonOut.println( "." );
                            }
                        }
                        status.setPromotedLevel( pl );
                    }
                } else {
                    status.setPromotedLevel( sourcebaseline.getPromotionLevel( false ) );
                }

				/* Recommend the Baseline */
				if( recommend ) {
					try {
						targetstream.recommendBaseline( targetbaseline );
						hudsonOut.println( CCUCMNotifier.logShortPrefix+" Baseline " + targetbaseline.getShortname() + " is now recommended." );
					} catch( ClearCaseException e ) {
						status.setStable( false );
						status.setRecommended( false );
						hudsonOut.println( CCUCMNotifier.logShortPrefix +" Could not recommend Baseline " + targetbaseline.getShortname() + ": " + e.getMessage() );
						logger.warning( "Could not recommend baseline: " + e.getMessage() );
					}
				}
			} catch( Exception e ) {
				status.setStable( false );
				/*
				 * as it will not make sense to recommend if we cannot promote,
				 * we do this:
				 */
				if( recommend ) {
					status.setRecommended( false );
					hudsonOut.println( CCUCMNotifier.logShortPrefix +" Could not promote baseline " + sourcebaseline.getShortname() + " and will not recommend " + targetbaseline.getShortname() + ". " + e.getMessage() );
					logger.warning( "Could not promote baseline and will not recommend. " + e.getMessage() );
				} else {
					/*
					 * As we will not recommend if we cannot promote, it's ok to
					 * break method here
					 */
					hudsonOut.println( CCUCMNotifier.logShortPrefix+" Could not promote baseline " + sourcebaseline.getShortname() + ". " + e.getMessage() );
					logger.warning( "Could not promote baseline. " + e.getMessage() );
                    logger.log(Level.SEVERE, "Error: ", e);
				}
			}

		} else { /* The build failed */
			status.setRecommended( false );

			hudsonOut.println( "[" + Config.nameShort + "] Build failed." );

			if( status.isTagAvailable() ) {
				tag.setEntry( "buildstatus", "FAILURE" );
			}

            /* Reject baseline, not any */
            if( !isPromotionSkipped ) {
                try {
                    if( hasRemoteMastership() ) {
                        printPostedOutput( sourcebaseline );
                        noticeString = "*";
                    } else {
                        logger.warning( "Rejecting baseline" );
                        Project.PromotionLevel pl = sourcebaseline.reject();
                        status.setPromotedLevel( pl );
                        hudsonOut.println( CCUCMNotifier.logShortPrefix + " Baseline " + sourcebaseline.getShortname() + " is " + sourcebaseline.getPromotionLevel( true ).toString() + "." );
                    }
                } catch( Exception e ) {
                    status.setStable( false );
                    hudsonOut.println( CCUCMNotifier.logShortPrefix +" Could not reject baseline " + sourcebaseline.getShortname() );
                    logger.log( Level.WARNING, "Could not reject baseline", e );
                }
            } else {
                status.setPromotedLevel( sourcebaseline.getPromotionLevel( false ) );
            }

		}

		Exception failBuild = null;
		
		/* Persist the Tag */
		if( makeTag ) {
			if( tag != null ) {
				try {
					if( hasRemoteMastership() ) {
						hudsonOut.println( CCUCMNotifier.logShortPrefix + " Baseline not marked with tag as it has different mastership" );
					} else {
						tag = tag.persist();
						hudsonOut.println( CCUCMNotifier.logShortPrefix + " Baseline now marked with tag: \n" + tag.stringify() );
					}
				} catch( Exception e ) {
					hudsonOut.println( CCUCMNotifier.logShortPrefix + " Could not change tag in ClearCase. Contact ClearCase administrator to do this manually." );
					if( e instanceof TagException && ((TagException) e).getType().equals( Type.NO_SUCH_HYPERLINK ) ) {
						logger.severe( "Hyperlink type not found, failing build" );
						hudsonOut.println( "Hyperlink type not found, failing build" );
						failBuild = e;
					} else {
						ExceptionUtils.print( e, hudsonOut, false );
						ExceptionUtils.log( e, true );
					}
				}
			} else {
				logger.warning( "Tag object was null" );
				hudsonOut.println( CCUCMNotifier.logShortPrefix + " Tag object was null, tag not set." );
			}
		}
        
		String newPLevel = sourcebaseline.getPromotionLevel( true ).toString();

        logger.info("Writing build description for poll subscribe");
        status.setBuildDescr( setDisplaystatusSubscribe( newPLevel + noticeString, sourcebaseline.getShortname() ) );

		
		if( failBuild != null ) {
			throw new IOException( failBuild );
		}		
		
		logger.info( "Remote post build finished normally" );
		return status;
	}

	private void printPostedOutput( Baseline sourcebaseline ) throws ClearCaseException {
		hudsonOut.println( CCUCMNotifier.logShortPrefix + " Baseline " + sourcebaseline.getShortname() + " was a posted delivery, and has a different mastership." );
		hudsonOut.println( CCUCMNotifier.logShortPrefix + " Its promotion level cannot be updated, but is left as " + sourcebaseline.getPromotionLevel( true ).toString() );
	}

	private boolean hasRemoteMastership() throws ClearCaseException {
		return !sourcebaseline.getMastership().equals( targetbaseline.getMastership() );
	}

	private String setDisplaystatusSubscribe( String plevel, String fqn ) {
		String s = "";

		s += "<small>" + fqn + " <b>" + plevel + "</b></small>";

		if( recommend ) {
			if( status.isRecommended() ) {
				s += "<br/><B><small>Recommended</small></B>";
			} else {
				s += "<br/><B><small>Could not recommend</small></B>";
			}
		}
		return s;
	}

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        
    }
}