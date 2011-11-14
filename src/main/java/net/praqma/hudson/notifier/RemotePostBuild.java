package net.praqma.hudson.notifier;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Tag;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.hudson.Config;
import net.praqma.hudson.scm.Unstable;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.appenders.StreamAppender;

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
    private Baseline sourcebaseline;
    private Baseline targetbaseline;
    private Stream sourcestream;
    private Stream targetstream;
    private boolean makeTag = false;
    private boolean recommend = false;
    private Status status;
    private BuildListener listener;
    private String id = "";
    private PrintStream hudsonOut = null;
    private Unstable unstable;
    private Pipe pipe = null;
    private Set<String> subscriptions;

    public RemotePostBuild(Result result, Status status, BuildListener listener,
            /* Values for */
            boolean makeTag, boolean recommended, Unstable unstable,
            /* Common values */
            Baseline sourcebaseline, Baseline targetbaseline, Stream sourcestream, Stream targetstream, String displayName, String buildNumber, Pipe pipe, Set<String> subscriptions) {


        this.displayName = displayName;
        this.buildNumber = buildNumber;

        this.id = "[" + displayName + "::" + buildNumber + "]";

        this.sourcebaseline = sourcebaseline;
        this.targetbaseline = targetbaseline;
        this.sourcestream = sourcestream;
        this.targetstream = targetstream;

        this.unstable = unstable;

        this.result = result;

        this.makeTag = makeTag;
        this.recommend = recommended;

        this.status = status;
        this.listener = listener;

        this.pipe = pipe;
        this.subscriptions = subscriptions;
    }

    public Status invoke(File workspace, VirtualChannel channel) throws IOException {
        hudsonOut = listener.getLogger();
        UCM.setContext(UCM.ContextType.CLEARTOOL);

        Logger logger = Logger.getLogger();

        StreamAppender app = null;
        if (pipe != null) {
            PrintStream toMaster = new PrintStream(pipe.getOut());
            app = new StreamAppender(toMaster);
            Logger.addAppender(app);
            app.setSubscriptions(subscriptions);
        }

        String newPLevel = "";

        logger.info("Starting PostBuild task");

        /* Create the Tag object */
        Tag tag = null;
        if (makeTag) {
            try {
                // Getting tag to set buildstatus
                tag = sourcebaseline.getTag(this.displayName, this.buildNumber);
                status.setTagAvailable(true);
            } catch (UCMException e) {
                hudsonOut.println("[" + Config.nameShort + "] Could not get Tag: " + e.getMessage());
                logger.warning(id + "Could not get Tag: " + e.getMessage());
            }
        }

        /* The build was a success and the deliver did not fail */
        if (result.equals(Result.SUCCESS) && status.isStable()) {

            if (status.isTagAvailable()) {
                tag.setEntry("buildstatus", "SUCCESS");
            }

            try {
                Project.Plevel pl = sourcebaseline.promote();
                status.setPromotedLevel(pl);
                hudsonOut.println("[" + Config.nameShort + "] Baseline " + sourcebaseline.getShortname() + " promoted to " + sourcebaseline.getPromotionLevel(true).toString() + ".");
            } catch (UCMException e) {
                status.setStable(false);
                /*
                 * as it will not make sense to recommend if we cannot
                 * promote, we do this:
                 */
                if (recommend) {
                    status.setRecommended(false);
                    hudsonOut.println("[" + Config.nameShort + "] Could not promote baseline " + sourcebaseline.getShortname() + " and will not recommend " + targetbaseline.getShortname() + ". " + e.getMessage());
                    logger.warning(id + "Could not promote baseline and will not recommend. " + e.getMessage());
                } else {
                    /*
                     * As we will not recommend if we cannot promote, it's
                     * ok to break method here
                     */
                    hudsonOut.println("[" + Config.nameShort + "] Could not promote baseline " + sourcebaseline.getShortname() + ". " + e.getMessage());
                    logger.warning(id + "Could not promote baseline. " + e.getMessage());
                }
            }

            /* Recommend the Baseline */
            if (recommend) {
                try {
                    targetstream.recommendBaseline(targetbaseline);
                    hudsonOut.println("[" + Config.nameShort + "] Baseline " + targetbaseline.getShortname() + " is now recommended.");
                } catch (UCMException e) {
                    status.setStable(false);
                    status.setRecommended(false);
                    hudsonOut.println("[" + Config.nameShort + "] Could not recommend Baseline " + targetbaseline.getShortname() + ": " + e.getMessage());
                    logger.warning(id + "Could not recommend baseline: " + e.getMessage());
                }
            }
        } /* The build failed or the deliver failed */ else {
            /* Do not set as recommended at all */
            if (recommend) {
                status.setRecommended(false);
            }

            /* The build failed */
            if (result.equals(Result.FAILURE)) {
                hudsonOut.println("[" + Config.nameShort + "] Build failed.");

                if (status.isTagAvailable()) {
                    tag.setEntry("buildstatus", "FAILURE");
                }

                try {
                    logger.warning(id + "Demoting baseline");
                    Project.Plevel pl = sourcebaseline.demote();
                    status.setPromotedLevel(pl);
                    hudsonOut.println("[" + Config.nameShort + "] Baseline " + sourcebaseline.getShortname() + " is " + sourcebaseline.getPromotionLevel(true).toString() + ".");
                } catch (Exception e) {
                    status.setStable(false);
                    // throw new NotifierException(
                    // "Could not demote baseline. " + e.getMessage() );
                    hudsonOut.println("[" + Config.nameShort + "] Could not demote baseline " + sourcebaseline.getShortname() + ". " + e.getMessage());
                    logger.warning(id + "Could not demote baseline. " + e.getMessage());
                }

            } /*
             * The build is unstable, or something in the middle.... TODO Maybe
             * not else if
             */ else if (!result.equals(Result.FAILURE)) {
                if (status.isTagAvailable()) {
                    tag.setEntry("buildstatus", "UNSTABLE");
                }

                try {
                    Project.Plevel pl = Project.Plevel.INITIAL;

                    /* Treat the build as successful */
                    if (unstable.treatSuccessful()) {
                        /* Promote */
                        pl = sourcebaseline.promote();
                        hudsonOut.println("[" + Config.nameShort + "] Baseline " + sourcebaseline.getShortname() + " is promoted, even though the build is unstable.");

                        /* Recommend the Baseline */
                        if (recommend) {
                            try {
                                targetstream.recommendBaseline(targetbaseline);
                                hudsonOut.println("[" + Config.nameShort + "] Baseline " + targetbaseline.getShortname() + " is now recommended.");
                            } catch (Exception e) {
                                status.setStable(false);
                                status.setRecommended(false);
                                hudsonOut.println("[" + Config.nameShort + "] Could not recommend baseline " + targetbaseline.getShortname() + ": " + e.getMessage());
                                logger.warning(id + "Could not recommend baseline. Reason: " + e.getMessage());
                            }
                        }
                    } else {
                        pl = sourcebaseline.demote();
                    }
                    status.setPromotedLevel(pl);
                    hudsonOut.println("[" + Config.nameShort + "] Baseline " + sourcebaseline.getShortname() + " is " + sourcebaseline.getPromotionLevel(true).toString() + ".");
                } catch (Exception e) {
                    status.setStable(false);
                    hudsonOut.println("[" + Config.nameShort + "] Could not demote baseline " + sourcebaseline.getShortname() + ". " + e.getMessage());
                    logger.warning(id + "Could not demote baseline. " + e.getMessage());
                }

            } /* Result not handled by CCUCM */ else {
                tag.setEntry("buildstatus", result.toString());
                logger.log(id + "Buildstatus (Result) was " + result + ". Not handled by plugin.");
                hudsonOut.println("[" + Config.nameShort + "] Baselines not changed. Buildstatus: " + result);
            }
        }

        /* Persist the Tag */
        if (makeTag) {
            if (tag != null) {
                try {
                    tag = tag.persist();
                    hudsonOut.println("[" + Config.nameShort + "] Baseline now marked with tag: \n" + tag.stringify());
                } catch (Exception e) {
                    hudsonOut.println("[" + Config.nameShort + "] Could not change tag in ClearCase. Contact ClearCase administrator to do this manually.");
                }
            } else {
                logger.warning(id + "Tag object was null");
                hudsonOut.println("[" + Config.nameShort + "] Tag object was null, tag not set.");
            }
        }
        try {
            newPLevel = sourcebaseline.getPromotionLevel(true).toString();
        } catch (UCMException e) {
            logger.log(id + " Could not get promotionlevel.");
            hudsonOut.println("[" + Config.nameShort + "] Could not get promotion level.");
        }

        if (this.sourcestream.equals(this.targetstream)) {
            status.setBuildDescr(setDisplaystatusSelf(newPLevel, targetbaseline.getShortname()));
        } else {
            status.setBuildDescr(setDisplaystatus(sourcebaseline.getShortname(), newPLevel, targetbaseline.getShortname(), status.getErrorMessage()));
        }


        logger.info(id + "Remote post build finished normally");
        Logger.removeAppender(app);
        return status;
    }

    private String setDisplaystatusSelf(String plevel, String fqn) {
        String s = "";

        // Get shortname
        s += "<small>" + fqn + " <b>" + plevel + "</b></small>";

        if (recommend) {
            if (status.isRecommended()) {
                s += "<br/><B><small>Recommended</small></B>";
            } else {
                s += "<br/><B><small>Could not recommend</small></B>";
            }
        }
        return s;
    }

    private String setDisplaystatus(String source, String plevel, String target, String error) {
        String s = "";

        if (plevel.equals("REJECTED")) {
            try {
                s += "<small>" + source + " made by " + sourcebaseline.getUser() + " was <b>" + plevel + "</b></small>";

            } catch (Exception e) {
                hudsonOut.print(e);
            }
        } else {
            s += "<small>" + source + " <b>" + plevel + "</b></small>";
        }
        if (status.isRecommended() && recommend) {
            s += "<br/><small>" + target + " <b>recommended</b></small>";
        }

        if (error != null) {
            s += "<br/><small>Failed with <b>" + error + "</b></small>";
        }

        return s;
    }
}