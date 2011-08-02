package net.praqma.hudson.notifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;

import jcifs.dcerpc.msrpc.netdfs;

import org.kohsuke.stapler.StaplerRequest;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.Cool;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.hudson.exception.NotifierException;
import net.praqma.hudson.remoting.RemoteDeliverComplete;
import net.praqma.hudson.remoting.Util;
import net.praqma.hudson.scm.PucmScm;
import net.praqma.hudson.scm.PucmState.State;
import net.praqma.util.debug.PraqmaLogger;
import net.praqma.util.debug.PraqmaLogger.Logger;
import net.sf.json.JSONObject;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.Future;
import hudson.remoting.Pipe;
import hudson.remoting.VirtualChannel;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

/**
 * PucmNotifier perfoms the user-chosen PUCM post-build actions
 *
 * @author Troels Selch
 * @author Margit Bennetzen
 *
 */
public class PucmNotifier extends Notifier {
    /* Old skool promotion */

    private boolean promote = false;
    private int promoteAction = __UNKNOWN_PROMOTE;
    private boolean recommended;
    //private Baseline baseline;
    private PrintStream hudsonOut;
    private boolean makeTag;
    private boolean setDescription;
    private Status status;
    private String id = "";
    private Logger logger = null;
    private UCMDeliver ucmDeliverObj = null;
    private String jobName = "";
    private Integer jobNumber = 0;
    public static final int __UNKNOWN_PROMOTE = 99;
    public static final int __NO_PROMOTE = 100;
    public static final int __PROMOTE_STABLE = 101;
    public static final int __PROMOTE_UNSTABLE = 102;

    /**
     * This constructor is used in the inner class <code>DescriptorImpl</code>.
     *
     * @param promote <ol start="0"><li>Baseline will not be promoted after the build</li>
     * <li>Baseline will be promoted after the build if stable</li>
     * <li>Baseline will be promoted after the build if unstable</li></ol>
     * @param recommended
     *            if <code>true</code>, the baseline will be marked
     *            'recommended' in ClearCase.
     * @param makeTag
     *            if <code>true</code>, pucm will set a Tag() on the baseline in
     *            ClearCase.
     * @param ucmDeliver The special deliver object, in which all the deliver parameters are encapsulated.

     */
    public PucmNotifier(boolean promote, boolean recommended, boolean makeTag, boolean setDescription, UCMDeliver ucmDeliver, int promoteAction) {
        this.promote = promote;
        this.promoteAction = promoteAction;
        this.recommended = recommended;
        this.makeTag = makeTag;
        this.setDescription = setDescription;

        /* Advanced */
        this.ucmDeliverObj = ucmDeliver;
    }

    /**
     * This indicates whether to let pucm run after(true) the job is done or before(false)
     */
    @Override
    public boolean needsToRunAfterFinalized() {
        return false;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        
        boolean result = true;
        hudsonOut = listener.getLogger();
        
        /* Preparing the logger */
        logger = PraqmaLogger.getLogger();
        logger.subscribeAll();
        File rdir = new File(logger.getPath());
        logger.setLocalLog(new File(rdir + System.getProperty("file.separator") + "log.log"));

        /* Prepare job variables */
        jobName = build.getParent().getDisplayName().replace(' ', '_');
        jobNumber = build.getNumber();

        /*
        logger.unsubscribeAll();
        if (build.getBuildVariables().get("include_classes") != null) {
            String[] is = build.getBuildVariables().get("include_classes").toString().split(",");
            for (String i : is) {
                logger.subscribe(i.trim());
            }
        }
        */

        Cool.setLogger(logger);

        status = new Status();

        this.id = "[" + jobName + "::" + jobNumber + "]";

        SCM scmTemp = null;
        /*TODO result is always true!!!!...
         * so we could move the if blok unless it should not always be true
         */
        if (result) {
            scmTemp = build.getProject().getScm();
            if (!(scmTemp instanceof PucmScm)) {
                listener.fatalError("[PUCM] Not a PUCM scm. This Post build action can only be used when polling from ClearCase with PUCM plugin.");
                result = false;
            }
            scmTemp.toString();
        }

        State pstate = null;
        Baseline baseline = null;

        /* Only do this part if a valid PucmScm build */
        if (result) {
            /* Retrieve the pucm state */
            pstate = PucmScm.pucm.getState(jobName, jobNumber);
            pstate.getLogger().debug("The valid state: " + pstate.stringify());

            /* Validate the state */
            if (pstate.doPostBuild() && pstate.getBaseline() != null) {
                logger.debug(id + "Post build");

                /* This shouldn't actually be necessary!?
                 * TODO Maybe the baseline should be re-Load()ed instead of creating a new object?  */
                String bl = pstate.getBaseline().getFullyQualifiedName();

                /* If no baselines found bl will be null and the post build section will not proceed */
                if (bl != null) {
                    try {
                        baseline = UCMEntity.getBaseline(bl);
                    } catch (UCMException e) {
                        logger.warning(id + "Could not initialize baseline.");
                        baseline = null;
                    }

                    if (baseline == null) {
                        /* If baseline is null, the user has already been notified in Console output from PucmScm.checkout() */
                        result = false;
                    }
                } else {
                    result = false;
                }

            } else {
                // Not performing any post build actions.
                result = false;
            }
        }

        /* There's a valid baseline, lets process it */
        if (result) {

            try {
                processBuild(build, launcher, listener, pstate);
                if (setDescription) {
                    build.setDescription(status.getBuildDescr());
                    hudsonOut.println("[PUCM] Description set to - " + status.getBuildDescr());
                }

            } catch (NotifierException ne) {
                hudsonOut.println(ne.getMessage());
            } catch (IOException e) {
                hudsonOut.println("[PUCM] Couldn't set build description.");
            }
        } else {
            String d = build.getDescription();
            if (d != null) {
                build.setDescription((d.length() > 0 ? d + "<br/>" : "") + "Nothing to do");
            }

            build.setResult(Result.NOT_BUILT);
        }

        /*
         * Removing baseline and job from collection, do this no matter what as
         * long as the SCM is pucm
         */
        if ((scmTemp instanceof PucmScm) && baseline != null) {
            boolean done2 = pstate.remove();
            logger.debug(id + "Removing job " + build.getNumber() + " from collection: " + done2);

            //logger.debug( "PUCM FINAL=" + PucmScm.pucm.stringify() );

            if (pstate.isMultiSite()) {
                /* Trying to store baseline */
                //logger.debug( id + "Trying to store baseline" );
                if (!PucmScm.storedBaselines.addBaseline(pstate.getBaseline())) {
                    logger.warning(id + "Storing baseline failed.");
                }
            }
        }
        
        hudsonOut.println( "[PUCM] Post build steps done" );

        return result;
    }

    /**
     * This is where all the meat is. When the baseline is validated, the actual post build steps are performed. <br>
     * First the baseline is delivered(if chosen), then tagged, promoted and recommended.
     * @param build The build object in which the post build action is selected
     * @param launcher The launcher of the build
     * @param listener The listener of the build
     * @param pstate The {@link PucmState} of the build.
     * @throws NotifierException
     */
    private void processBuild(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, State pstate) throws NotifierException {
        Result buildResult = build.getResult();

        VirtualChannel ch = launcher.getChannel();

        if (ch == null) {
            logger.debug("The channel was null");
        }

        FilePath workspace = build.getExecutor().getCurrentWorkspace();

        if (workspace == null) {
            logger.warning("Workspace is null");
            throw new NotifierException("Workspace is null");
        }

        hudsonOut.println("[PUCM] Build result: " + buildResult);

        /* Poll child feature */
        if( pstate.getPolling().isPollingChilds() && pstate.needsToBeCompleted() ) {
            status.setBuildStatus(buildResult);
            
            boolean complete = buildResult.isBetterThan(Result.FAILURE);
            
            try {                
                hudsonOut.print("[PUCM] Trying to " + ( complete ? "complete" : "cancel" ) + " the deliver. ");
                Util.completeRemoteDeliver( workspace, listener, pstate, complete );
                hudsonOut.println("Success.");
                
                /* If deliver was completed, create the baseline */
                if( complete ) {
                    Baseline childBase = pstate.getBaseline();
                    try {
                        hudsonOut.print("[PUCM] Creating baseline on stream. ");
                        //Baseline.create(childBase.getShortname(), childBase.getComponent(), pstate.getSnapView().GetViewRoot(), true, true);
                        net.praqma.hudson.Util.CreateNumber(listener, build.getNumber(), pstate.getBaselineInformation().versionFrom, 
                        		pstate.getBaselineInformation().buildnumberMajor, pstate.getBaselineInformation().buildnumberMinor, 
                        		pstate.getBaselineInformation().buildnumberPatch, pstate.getBaselineInformation().buildnumberSequenceSelector, 
                        		pstate.getStream().getDefaultTarget(), pstate.getComponent());
                        hudsonOut.println(" Success.");
                    } catch( UCMException e ) {
                        hudsonOut.println(" Failed.");
                        logger.warning( "Failed to create baseline on stream" );
                        logger.warning( e );
                    }
                }
                
            } catch( Exception e ) {
                status.setBuildStatus(buildResult);
                status.setStable(false);
                hudsonOut.println("Failed.");
                logger.warning(e);
                
                /* If trying to complete and it failed, try to cancel it */
                if( complete ) {
                    try{
                        hudsonOut.print("[PUCM] Trying to cancel the deliver. ");
                        Util.completeRemoteDeliver( workspace, listener, pstate, false );
                        hudsonOut.println("Success.");
                    } catch( Exception e1 ) {
                        hudsonOut.println(" Failed.");
                        logger.warning( "Failed to cancel deliver" );
                        logger.warning( e );
                    }
                } else {
                    logger.warning( "Failed to cancel deliver" );
                    logger.warning( e );
                }
            }

            
        /* Regular PUCM */
        } else {
                
            logger.debug(id + "Trying to run remote tasks");
            if (ucmDeliverObj != null && ucmDeliverObj.ucmDeliver) {
                logger.debug(id + "UCM deliver");
    
    
                try {
                    final Pipe pipe = Pipe.createRemoteToLocal();
    
                    Future<Integer> i = null;
    
                    i = workspace.actAsync(new RemoteDeliver(buildResult, status, listener, pstate.getComponent().getFullyQualifiedName(),
                            pstate.getLoadModule(), pstate.getBaseline().getFullyQualifiedName(), build.getParent().getDisplayName(), Integer.toString(build.getNumber()), ucmDeliverObj, logger, pipe));
                    InputStream is = pipe.getIn();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder sb = new StringBuilder();
    
                    int j = i.get();
                } catch (IOException e) {
                    status.setStable(false);
                    logger.warning("COULD NOT DELIVER: " + e.getMessage());
                    logger.warning(e);
                    hudsonOut.println("[PUCM] Error: The deliver failed: " + e.getMessage());
                } catch (InterruptedException e) {
                    status.setStable(false);
                    logger.warning("COULD NOT DELIVER111: " + e.getMessage());
                    logger.warning(e);
                    hudsonOut.println("[PUCM] Error: The deliver failed: " + e.getMessage());
                } catch (ExecutionException e) {
                    status.setStable(false);
                    logger.warning("COULD NOT DELIVER(Excecution): " + e.getMessage());
                    logger.warning(e);
                    hudsonOut.println("[PUCM] Error: The deliver failed: " + e.getMessage());
                }
    
                logger.debug(id + "UCM deliver DONE");
            }
        }
    
        /* Remote post build step, common to all types */
        try {
            logger.debug(id + "Remote post build step");
            hudsonOut.println("[PUCM] Performing common post build steps");

            final Pipe pipe = Pipe.createRemoteToLocal();

            Future<Status> f = null;

            f = workspace.actAsync(new RemotePostBuild(buildResult, status, listener, makeTag, promoteAction, recommended, pstate.getBaseline().getFullyQualifiedName(), pstate.getStream().getFullyQualifiedName(), build.getParent().getDisplayName(), Integer.toString(build.getNumber()), logger/*, pout*/, pipe));

            status = f.get();

            logger.empty(status.getLog());
        } catch (Exception e) {
            status.setStable(false);
            logger.debug(id + "Something went wrong: " + e.getMessage());
            logger.warning(e);
            hudsonOut.println("[PUCM] Error: Post build failed: " + e.getMessage());
        }

        /* If the promotion level of the baseline was changed on the remote */
        if (status.getPromotedLevel() != null) {
            pstate.getBaseline().setPromotionLevel(status.getPromotedLevel());
            logger.debug(id + "Baselines promotion level sat to " + status.getPromotedLevel().toString());
        }
        
        status.setBuildStatus(buildResult);
        
        if (!status.isStable()) {
            build.setResult(Result.UNSTABLE);
        }
    }

    public boolean getPromote() {
        return promoteAction > PucmNotifier.__NO_PROMOTE;
    }

    public int getPromoteAction() {
        return promoteAction;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public boolean isMakeTag() {
        return makeTag;
    }

    public boolean isSetDescription() {
        return setDescription;
    }

    /* Advanced */
    public boolean isUcmDeliver() {
        if (ucmDeliverObj != null) {
            return ucmDeliverObj.ucmDeliver;
        } else {
            return false;
        }
    }

    public String getAlternateTarget() {
        if (ucmDeliverObj != null) {
            return ucmDeliverObj.alternateTarget;
        } else {
            return "";
        }
    }

    public String getBaselineName() {
        if (ucmDeliverObj != null) {
            return ucmDeliverObj.baselineName;
        } else {
            return "";
        }
    }

    public String getVersionFrom() {
        if (ucmDeliverObj != null) {
            return ucmDeliverObj.versionFrom;
        } else {
            return "";
        }
    }

    public String getBuildnumberSequenceSelector() {
        if (ucmDeliverObj != null) {
            return ucmDeliverObj.buildnumberSequenceSelector;
        } else {
            return "";
        }
    }

    public String getbuildnumberMajor() {
        if (ucmDeliverObj != null) {
            return ucmDeliverObj.buildnumberMajor;
        } else {
            return "";
        }
    }

    public String getbuildnumberMinor() {
        if (ucmDeliverObj != null) {
            return ucmDeliverObj.buildnumberMinor;
        } else {
            return "";
        }
    }

    public String getbuildnumberPatch() {
        if (ucmDeliverObj != null) {
            return ucmDeliverObj.buildnumberPatch;
        } else {
            return "";
        }
    }

    /**
     * This class is used by Hudson to define the plugin.
     *
     * @author Troels Selch
     * @author Margit Bennetzen
     *
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(PucmNotifier.class);
            //logger.trace_function();
            load();
        }

        @Override
        public String getDisplayName() {
            //logger.trace_function();
            return "Praqmatic UCM";
        }

        /**
         * Hudson uses this method to create a new instance of
         * <code>PucmNotifier</code>. The method gets information from Hudson
         * config page. This information is about the configuration, which
         * Hudson saves.
         */
        @Override
        public Notifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            int promoteAction = PucmNotifier.__NO_PROMOTE;
            try {
                promoteAction = Integer.parseInt(req.getParameter("Pucm.promoteAction"));
            } catch (NumberFormatException e) {
                throw new FormException("Could not parse integer: " + e.getMessage(), "Pucm.promotedAction");
                /* No op */
            }

            /* Old promote field */
            boolean promote = req.getParameter("Pucm.promote") != null;

            boolean recommended = req.getParameter("Pucm.recommended") != null;
            boolean makeTag = req.getParameter("Pucm.makeTag") != null;
            boolean setDescription = req.getParameter("Pucm.setDescription") != null;

            boolean ucmDeliver = req.getParameter("Pucm.ucmDeliver") != null;
            String alternateTarget = req.getParameter("Pucm.alternateTarget");
            String baselineName = req.getParameter("Pucm.baselineName");
            //      boolean apply4level = req.getParameter("Pucm.apply4level") != null;
            String versionFrom = req.getParameter("Pucm.versionFrom");
            String buildnumberSequenceSelector = req.getParameter("Pucm.buildnumberSequenceSelector");

            String buildnumberMajor = req.getParameter("Pucm.buildnumberMajor");
            String buildnumberMinor = req.getParameter("Pucm.buildnumberMinor");
            String buildnumberPatch = req.getParameter("Pucm.buildnumberPatch");

            UCMDeliver d = new UCMDeliver();

            d.ucmDeliver = ucmDeliver;
            d.alternateTarget = alternateTarget;
            d.baselineName = baselineName;
            //d.apply4level      = apply4level;
            d.versionFrom = versionFrom;
            d.buildnumberSequenceSelector = buildnumberSequenceSelector;
            d.buildnumberMajor = buildnumberMajor;
            d.buildnumberMinor = buildnumberMinor;
            d.buildnumberPatch = buildnumberPatch;

            save();

            return new PucmNotifier(promote, recommended, makeTag, setDescription, d, promoteAction);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> arg0) {
            //logger.trace_function();
            return true;
        }
    }
}
