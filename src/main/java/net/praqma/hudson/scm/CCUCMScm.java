package net.praqma.hudson.scm;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.util.FormValidation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.praqma.clearcase.exceptions.DeliverException;
import net.praqma.clearcase.exceptions.DeliverException.Type;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.ucm.entities.*;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.Config;
import net.praqma.hudson.Util;
import net.praqma.hudson.exception.CCUCMException;
import net.praqma.hudson.exception.DeliverNotCancelledException;
import net.praqma.hudson.exception.TemplateException;
import net.praqma.hudson.nametemplates.NameTemplate;
import net.praqma.hudson.notifier.CCUCMNotifier;
import net.praqma.hudson.remoting.*;
import net.praqma.hudson.remoting.deliver.GetChanges;
import net.praqma.hudson.remoting.deliver.MakeDeliverView;
import net.praqma.hudson.remoting.deliver.StartDeliver;
import static net.praqma.hudson.scm.CCUCMScm.getLastAction;
import net.praqma.hudson.scm.Polling.PollingType;
import net.praqma.util.execute.AbnormalProcessTerminationException;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

/**
 * is responsible for everything regarding Hudsons connection to ClearCase
 * pre-build. This class defines all the files required by the user. The
 * information can be entered on the config page.
 */
public class CCUCMScm extends SCM {

    
    private static final Logger logger = Logger.getLogger(CCUCMScm.class.getName());
    
    private Boolean multisitePolling;
    private Project.PromotionLevel plevel;
    private String loadModule;
    private String component;
    private String stream;
    private String bl;
    private StringBuffer pollMsgs = new StringBuffer();
    private Stream integrationstream;
    private String buildProject;
    private String jobName = "";
    private Integer jobNumber;
    private boolean forceDeliver;
    
    /**
     * Determines whether to remove the view private files or not
     */
    private boolean removeViewPrivateFiles;
    private boolean trimmedChangeSet;
    private boolean discard;

    /* Old notifier fields */
    private boolean recommend;
    private boolean makeTag;
    private boolean setDescription;
    private Unstable treatUnstable;
    private boolean createBaseline;
    private String nameTemplate;
    private Polling polling;
    private String viewtag = "";
    private Baseline lastBaseline;
    private String levelToPoll;
    private boolean addPostBuild = true;

    /**
     * Default constructor, mainly used for unit tests.
     */
    public CCUCMScm() {
        discard = false;
    }

    /**
     * To support backwards compatibility.
     *
     * @since 1.4.0
     */
    public CCUCMScm(String component, String levelToPoll, String loadModule, boolean newest, String polling, String stream, String treatUnstable,
            boolean createBaseline, String nameTemplate, boolean forceDeliver, boolean recommend, boolean makeTag, boolean setDescription, String buildProject) {

        this(component, levelToPoll, loadModule, newest, polling, stream, treatUnstable, createBaseline, nameTemplate, forceDeliver, recommend, makeTag, setDescription, buildProject, true, false, false);
    }

    @DataBoundConstructor
    public CCUCMScm(String component, String levelToPoll, String loadModule, boolean newest, String polling, String stream, String treatUnstable,
            boolean createBaseline, String nameTemplate, boolean forceDeliver, boolean recommend, boolean makeTag, boolean setDescription, String buildProject, boolean removeViewPrivateFiles, boolean trimmedChangeSet, boolean discard) {

        this.component = component;
        this.loadModule = loadModule;
        this.stream = stream;
        this.buildProject = buildProject;

        this.polling = new Polling(polling);
        this.treatUnstable = new Unstable(treatUnstable);

        this.createBaseline = createBaseline;
        this.nameTemplate = nameTemplate;

        this.forceDeliver = forceDeliver;
        this.removeViewPrivateFiles = removeViewPrivateFiles;
        this.trimmedChangeSet = trimmedChangeSet;

        this.recommend = recommend;
        this.makeTag = makeTag;
        this.setDescription = setDescription;
        this.plevel = Util.getLevel(levelToPoll);
        this.levelToPoll = levelToPoll;
        this.discard = discard;
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {
        /* Prepare job variables */

        jobName = build.getParent().getDisplayName().replace(' ', '_');
        jobNumber = build.getNumber();

        PrintStream out = listener.getLogger();

        /* Printing short description of build */
        String version = Hudson.getInstance().getPlugin("clearcase-ucm-plugin").getWrapper().getVersion();
        out.println("[" + Config.nameShort + "] ClearCase UCM Plugin version " + version);
        out.println("[" + Config.nameShort + "] Allow for slave polling: " + this.getSlavePolling());
        out.println("[" + Config.nameShort + "] Poll for posted deliveries: " + this.getMultisitePolling());

        logger.info("ClearCase UCM plugin v. " + version);

        /* Check for ClearCase on remote */
        try {
            workspace.act(new RemoteClearCaseCheck());
        } catch (AbnormalProcessTerminationException e) {
            build.setDescription(e.getMessage());
            throw e;
        }

        /* Make build action */
        /* Store the configuration */
        CCUCMBuildAction action = null;
        try {
            action = getBuildAction();
        } catch (UnableToInitializeEntityException e) {
            Util.println(out, e);
            throw new AbortException(e.getMessage());
        }

        action.setBuild(build);
        build.addAction(action);
        action.setListener(listener);

        /* Determining the user has parameterized a Baseline */
        String baselineInput = getBaselineValue(build);

        logger.fine("PUBLISHERS BEFORE: " + build.getProject().getPublishersList());
        if (addPostBuild) {
            ensurePublisher(build);
        }
        out.println("PUBLISHERS AFTER: " + build.getProject().getPublishersList());

        /* The special Baseline parameter case */
        if (build.getBuildVariables().get(baselineInput) != null) {
            logger.fine("Baseline parameter: " + baselineInput);
            action.setPolling(new Polling(PollingType.none));
            polling = action.getPolling();
            try {
                resolveBaselineInput(build, baselineInput, action, listener);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Resolving baseline input failed", e);
                Util.println(out, "No Baselines found");
            }
        } else {
            out.println("[" + Config.nameShort + "] Polling streams: " + polling.toString());
            try {
                resolveBaseline(workspace, build.getProject(), action, listener);
            } catch (CCUCMException e) {
                logger.warning(e.getMessage());
                /* If the promotion level is not set, ANY, use the last found Baseline */
                if (plevel == null) {
                    logger.fine("Promotion level was null [=ANY], finding the last built baseline");
                    CCUCMBuildAction last = getLastAction(build.getProject());
                    if (action != null) {
                        action.setBaseline(last.getBaseline());
                    } else {
                        build.setDescription("No valid baselines found");
                        throw new AbortException("No valid baselines found");
                    }
                } else {
                    build.setDescription("No valid baselines found");
                    throw new AbortException("No valid baselines found");
                }
            } catch (IOException e) {
                Exception cause = (Exception) e.getCause();
                if (cause != null) {
                    action.setResolveBaselineException(cause);
                    build.setDescription(cause.getMessage());
                    throw new AbortException(cause.getMessage());
                } else {
                    throw new AbortException("Unable to list baselines");
                }
            }
        }

        /* Try to save */
        build.save();

        boolean result = true;

        /* If a baseline is found */
        if (action.getBaseline() != null) {
            out.println("[" + Config.nameShort + "] Using " + action.getBaseline().getNormalizedName());
            
            if (polling.isPollingSelf() || !polling.isPolling()) {
                logger.fine("Initializing workspace");
                result = initializeWorkspace(build, workspace, changelogFile, listener, action);
            } else {
                /* Only start deliver when NOT polling self */
                logger.fine("Deliver");
                SnapshotView snapshotView = initializeDeliverView(build, action, listener);
                generateChangeLog(build, action, listener, changelogFile, snapshotView);
            }

            action.setViewTag(viewtag);
        }

        out.println("[" + Config.nameShort + "] Pre build steps done");

        /* We'll switch it back after using it, because it is only for testing purposes */
        addPostBuild = true;


        /* If there's a result let's find out whether a baseline is found or not */
        if (action.getBaseline() == null) {
            out.println("[" + Config.nameShort + "] Finished processing, no baseline found");
        } else {
            out.println("[" + Config.nameShort + "] Finished processing " + action.getBaseline());
        }

        logger.fine("ENDING CHECKOUT");

        return result;
    }

    @Override
    public void postCheckout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener) throws IOException, InterruptedException {

        logger.fine("BEGINNING POSTCHECKOUT");

        /* This is really only interesting if child or sibling polling */
        if (polling.isPollingOther()) {

            CCUCMBuildAction state = build.getAction(CCUCMBuildAction.class);

            PrintStream consoleOutput = listener.getLogger();

            try {
                logger.fine("Starting deliver");
                StartDeliver sd = new StartDeliver(listener, state.getStream(), state.getBaseline(), state.getSnapshotView(), loadModule, state.doForceDeliver(), state.doRemoveViewPrivateFiles());
                workspace.act(sd);

                consoleOutput.println("[" + Config.nameShort + "] Deliver successful");

                /* Deliver failed */
            } catch (Exception e) {
                consoleOutput.println("[" + Config.nameShort + "] Deliver failed");

                /* Check for exception types */
                Exception cause = (Exception) net.praqma.util.ExceptionUtils.unpackFrom(IOException.class, e);

                consoleOutput.println("[" + Config.nameShort + "] Cause: " + cause.getClass());

                /* Re-throw */
                try {
                    logger.log(Level.WARNING, "", cause);
                    throw cause;
                } catch (DeliverException de) {

                    consoleOutput.println("[" + Config.nameShort + "] " + de.getType());

                    /* We need to store this information anyway */
                    state.setViewPath(de.getDeliver().getViewContext());
                    state.setViewTag(de.getDeliver().getViewtag());

                    /* The deliver is started, cancel it */
                    if (de.isStarted()) {
                        try {
                            consoleOutput.print("[" + Config.nameShort + "] Cancelling deliver. ");
                            RemoteUtil.completeRemoteDeliver(workspace, listener, state.getBaseline(), state.getStream(), de.getDeliver().getViewtag(), de.getDeliver().getViewContext(), false);
                            consoleOutput.println("Success");

                            /* Make sure, that the post step is not run */
                            state.setNeedsToBeCompleted(false);

                        } catch (Exception ex) {
                            consoleOutput.println("[" + Config.nameShort + "] Failed to cancel deliver");
                            consoleOutput.println(ExceptionUtils.getFullStackTrace(e));
                            logger.warning(ExceptionUtils.getFullStackTrace(e));
                        }
                    } else {
                        logger.fine("No need for completing deliver");
                        state.setNeedsToBeCompleted(false);
                    }

                    /* Write something useful to the output */
                    if (de.getType().equals(Type.MERGE_ERROR)) {
                        consoleOutput.println("[" + Config.nameShort + "] Changes need to be manually merged, The stream " + state.getBaseline().getStream().getShortname() + " must be rebased to the most recent baseline on " + state.getStream().getShortname() + " - During the rebase the merge conflict should be solved manually. Hereafter create a new baseline on " + state.getBaseline().getStream().getShortname() + ".");
                        state.setError("merge error");
                    }

                    /* Force deliver not cancelled */
                } catch (DeliverNotCancelledException e1) {
                    consoleOutput.println("[" + Config.nameShort + "] Failed to force cancel existing deliver");
                    state.setNeedsToBeCompleted(false);
                } catch (Exception e1) {
                    logger.log(Level.WARNING, "", e);
                    e.printStackTrace(consoleOutput);
                }

                throw new AbortException("Unable to start deliver");
            }
        }

        logger.fine("ENDING POSTCHECKOUT");
    }

    public void ensurePublisher(AbstractBuild build) throws IOException {
        Describable describable = build.getProject().getPublishersList().get(CCUCMNotifier.class);
        if (describable == null) {
            logger.info("Adding notifier to project");
            build.getProject().getPublishersList().add(new CCUCMNotifier());
        }
    }

    public void setAddPostBuild(boolean addPostBuild) {
        this.addPostBuild = addPostBuild;
    }

    private boolean checkInput(TaskListener listener) {
        PrintStream out = listener.getLogger();
        out.println("[" + Config.nameShort + "] Verifying input");

        /* Check baseline template */
        if (createBaseline) {
            /* Sanity check */
            if (polling.isPollingOther()) {
                if (nameTemplate != null && nameTemplate.length() > 0) {

                    if (nameTemplate.matches("^\".+\"$")) {
                        nameTemplate = nameTemplate.substring(1, nameTemplate.length() - 1);
                    }

                    try {
                        NameTemplate.testTemplate(nameTemplate);
                    } catch (TemplateException e) {
                        out.println("[" + Config.nameShort + "] The template could not be parsed correctly: " + e.getMessage());
                        return false;
                    }
                } else {
                    out.println("[" + Config.nameShort + "] A valid template must be provided to create a Baseline");
                    return false;
                }
            } else {
                out.println("[" + Config.nameShort + "] You cannot create a baseline in this mode");
                return false;
            }
        }

        /* Check polling vs plevel */
        if (plevel == null) {
            if (polling.isPollingSelf()) {
                return true;
            } else {
                out.println("[" + Config.nameShort + "] You cannot poll any on other than self");
                return false;
            }
        }

        return true;
    }

    private boolean initializeWorkspace(AbstractBuild<?, ?> build, FilePath workspace, File changelogFile, BuildListener listener, CCUCMBuildAction action) throws IOException, InterruptedException {

        PrintStream consoleOutput = listener.getLogger();

        EstablishResult er = null;
        CheckoutTask ct = new CheckoutTask(listener, jobName, build.getNumber(), action.getStream(), loadModule, action.getBaseline(), buildProject, (plevel == null), action.doRemoveViewPrivateFiles());
        er = workspace.act(ct);
        //String changelog = er.getMessage();
        String changelog = "";
        changelog = Util.createChangelog(er.getActivities(), action.getBaseline(), trimmedChangeSet, discard, er.getView().getViewRoot(), er.getView().getReadOnlyLoadLines());
        action.setActivities(er.getActivities());

        this.viewtag = er.getViewtag();

        /* Write change log */
        try {
            FileOutputStream fos = new FileOutputStream(changelogFile);
            fos.write(changelog.getBytes());
            fos.close();
        } catch (IOException e) {
            logger.fine("Could not write change log file");
            consoleOutput.println("[" + Config.nameShort + "] Could not write change log file");
        }


        return true;
    }

    /**
     * Resolve the {@link Baseline} parameter and store the information in the
     * Action
     *
     * @param build
     * @param baselineInput
     * @param action
     * @param listener
     * @throws UnableToInitializeEntityException
     * @throws CCUCMException
     */
    public void resolveBaselineInput(AbstractBuild<?, ?> build, String baselineInput, CCUCMBuildAction action, BuildListener listener) throws UnableToInitializeEntityException, IOException, InterruptedException {

        PrintStream consoleOutput = listener.getLogger();

        String baselinename = build.getBuildVariables().get(baselineInput);
        action.setBaseline(Baseline.get(baselinename));

        /* Load the baseline */
        action.setBaseline((Baseline) RemoteUtil.loadEntity(build.getWorkspace(), action.getBaseline(), true));

        action.setStream(action.getBaseline().getStream());
        consoleOutput.println("[" + Config.nameShort + "] Starting parameterized build with a Baseline.");

        action.setComponent(action.getBaseline().getComponent());
        action.setStream(action.getBaseline().getStream());
    }

    public String getBaselineValue(AbstractBuild<?, ?> build) {
        Collection<?> c = build.getBuildVariables().keySet();
        Iterator<?> i = c.iterator();

        while (i.hasNext()) {
            String next = i.next().toString();
            if (next.equalsIgnoreCase("baseline")) {
                return next;
            }
        }

        return null;
    }

    /**
     * Resolve the {@link Baseline} to be build
     *
     * @param workspace
     * @param project
     * @param action
     * @param listener
     * @throws CCUCMException is thrown if no valid baselines are found
     */
    private void resolveBaseline(FilePath workspace, AbstractProject<?, ?> project, CCUCMBuildAction action, BuildListener listener) throws IOException, InterruptedException, CCUCMException {
        logger.fine("Resolving Baseline from the Stream " + action.getStream().getNormalizedName());
        PrintStream out = listener.getLogger();

        printParameters(out);

        /* The Stream must be loaded */
        action.setStream((Stream) RemoteUtil.loadEntity(workspace, action.getStream(), getSlavePolling()));

        List<Baseline> baselines = null;

        /* We need to discriminate on promotion level, JENKINS-16620 */
        Date date = null;
        if (plevel == null) {
            CCUCMBuildAction lastAction = getLastAction(project);
            if (lastAction != null) {
                date = lastAction.getBaseline().getDate();
            }
        }

        /* Find the Baselines and store them, none of the methods returns null! At least an empty list */
        /* Old skool self polling */
        if (polling.isPollingSelf()) {
            baselines = getValidBaselinesFromStream(workspace, plevel, action.getStream(), action.getComponent(), date);
        } else {
            baselines = getBaselinesFromStreams(workspace, listener, out, action.getStream(), action.getComponent(), polling.isPollingChilds(), date);
        }

        /* if we did not find any baselines we should return false */
        if (baselines.size() < 1) {
            throw new CCUCMException("No valid Baselines found");
        }

        /* Select and load baseline */
        action.setBaseline(selectBaseline(baselines, plevel, workspace));

        /* Print the baselines to jenkins out */
        printBaselines(baselines, out);
        out.println("");
    }

    /**
     * Initialize the deliver view
     */
    public SnapshotView initializeDeliverView(AbstractBuild<?, ?> build, CCUCMBuildAction state, BuildListener listener) throws IOException, InterruptedException {
        FilePath workspace = build.getWorkspace();
        PrintStream consoleOutput = listener.getLogger();

        logger.fine("Initializing deliver view");
        MakeDeliverView mdv = new MakeDeliverView(listener, build.getParent().getDisplayName(), loadModule, state.getStream());
        SnapshotView view = workspace.act(mdv);
        state.setViewPath(view.getViewRoot());
        state.setViewTag(view.getViewtag());
        state.setSnapshotView(view);
        this.viewtag = view.getViewtag();

        return view;
    }

    /**
     * Generate the change log for poll/sibling mode
     * @param build
     * @throws java.lang.InterruptedException
     */
    public void generateChangeLog(AbstractBuild<?, ?> build, CCUCMBuildAction state, BuildListener listener, File changelogFile, SnapshotView snapshotView) throws IOException, InterruptedException {
        FilePath workspace = build.getWorkspace();
        PrintStream consoleOutput = listener.getLogger();

        logger.fine("Generating change log");
        logger.fine(String.format( "Trim changeset = %s, Discard changes under read-only = %s", trimmedChangeSet, discard ) );

        GetChanges gc = new GetChanges(listener, state.getStream(), state.getBaseline(), snapshotView.getPath());
        List<Activity> activities = workspace.act(gc);

        String changelog = "";
        changelog = Util.createChangelog(activities, state.getBaseline(), trimmedChangeSet, discard, new File(snapshotView.getPath()), snapshotView.getReadOnlyLoadLines());
        state.setActivities(activities);

        /* Write change log */
        try {
            FileOutputStream fos = new FileOutputStream(changelogFile);
            fos.write(changelog.getBytes());
            fos.close();
        } catch (IOException e) {
            logger.fine("Could not write change log file");
            consoleOutput.println("[" + Config.nameShort + "] Could not write change log file");
        }
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ChangeLogParserImpl();
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, Map<String, String> env) {
        super.buildEnvVars(build, env);

        String CC_BASELINE = "";
        String CC_VIEWPATH = "";
        String CC_VIEWTAG = "";

        try {

            CCUCMBuildAction action = build.getAction(CCUCMBuildAction.class);
            CC_BASELINE = action.getBaseline().getFullyQualifiedName();
        } catch (Exception e1) {
            if (build != null) {
                System.out.println( String.format ( "Failure in build environment variables (buildEnvVars) for job %s %nFull trace written to log", build.getProject().getName()) ); 
            } else {
                System.out.println( String.format ( "Build is null in buildEnvVars.%nTrace written to log as no other information can be gathered" ) ); 
            }
            logger.log(Level.WARNING, "Exception caught in buildEnvVars method", e1);
        }

        /* View tag */
        CC_VIEWTAG = viewtag;

        /* View path */
        String workspace = env.get("WORKSPACE");
        if (workspace != null) {
            CC_VIEWPATH = workspace + File.separator + "view";
        } else {
            CC_VIEWPATH = "";
        }

        env.put("CC_BASELINE", CC_BASELINE);
        env.put("CC_VIEWTAG", CC_VIEWTAG);
        env.put("CC_VIEWPATH", CC_VIEWPATH);
    }

    /**
     * This method polls the version control system to see if there are any
     * changes in the source code.
     *
     * @param project
     * @param launcher
     * @param workspace
     * @param listener
     * @param rstate
     * @return 
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    @Override
    public PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState rstate) throws IOException, InterruptedException {

        workspace.act(new RemoteClearCaseCheck());

        jobName = project.getDisplayName().replace(' ', '_');
        jobNumber = project.getNextBuildNumber();

        PollingResult p = PollingResult.NO_CHANGES;

        /*
        See [FB11107]. This is also related to JENKINS-14806. Discovered during investigation of performance issues. We need to talk
        to Leif and possibly Lars to figure out why we distinguish between multisite and non multisite polling
        with this method.         
        */
        if (this.getMultisitePolling()) {
            /* multisite polling and a build is in progress */
            if (project.isBuilding()) {
                logger.info("A build already building - cancelling poll");
                return PollingResult.NO_CHANGES;
            }
        } else {
            /* not multisite polling and a the project is already in queue */
            if (project.isInQueue()) {
                logger.fine("A build already in queue - cancelling poll");
                return PollingResult.NO_CHANGES;
            }
        }

        logger.fine("Need for polling");

        PrintStream out = listener.getLogger();

        Stream stream = null;
        Component component = null;
        try {
            stream = Stream.get(this.stream);
            component = Component.get(this.component);
        } catch (UnableToInitializeEntityException e) {
            Util.println(out, e);
            throw new AbortException("Unable initialize ClearCase entities");
        }

        logger.fine("Let's go!");

        /* Check input */        
        if (checkInput(listener)) {

            printParameters(out);

            List<Baseline> baselines = null;

            /* We need to discriminate on promotion level, JENKINS-16620.
             *
             * This is ONLY for ANY!
             * */
            Date date = null;
            if (plevel == null) {
                CCUCMBuildAction lastAction = getLastAction(project);
                if (lastAction != null) {
                    date = lastAction.getBaseline().getDate();
                }
            }

            /* Old skool self polling */
            if (polling.isPollingSelf()) {
                baselines = getValidBaselinesFromStream(workspace, plevel, stream, component, date);
            } else {
                /* Find the Baselines and store them */
                baselines = getBaselinesFromStreams(workspace, listener, out, stream, component, polling.isPollingChilds(), date);
            }

            if (baselines.size() > 0) {                
                p = PollingResult.BUILD_NOW;
            }

        }

        return p;
    }

    /**
     * Get the {@link Baseline}s from a {@link Stream}s related Streams.
     *
     * @return A list of {@link Baseline}'s
     */
    private List<Baseline> getBaselinesFromStreams(FilePath workspace, TaskListener listener, PrintStream consoleOutput, Stream stream, Component component, boolean pollingChildStreams, Date date) {

        List<Stream> streams = null;
        List<Baseline> baselines = new ArrayList<Baseline>();

        try {
            streams = RemoteUtil.getRelatedStreams(workspace, listener, stream, pollingChildStreams, this.getSlavePolling(), this.getMultisitePolling());
        } catch (Exception e1) {
            Throwable root = ExceptionUtils.getRootCause(e1);
            logger.log(Level.WARNING, "Could not get related streams from " + stream, root);
            consoleOutput.println("[" + Config.nameShort + "] No streams found");
            return baselines;
        }

        consoleOutput.println("[" + Config.nameShort + "] Scanning " + streams.size() + " stream" + (streams.size() == 1 ? "" : "s") + " for baselines.");

        int c = 1;
        for (Stream s : streams) {
            try {
                consoleOutput.printf("[" + Config.nameShort + "] [%02d] %s ", c, s.getShortname());
                c++;
                List<Baseline> found = RemoteUtil.getRemoteBaselinesFromStream(workspace, component, s, plevel, this.getSlavePolling(), this.getMultisitePolling(), date);
                for (Baseline b : found) {
                    baselines.add(b);
                }
                consoleOutput.println(found.size() + " baseline" + (found.size() == 1 ? "" : "s") + " found");
            } catch (Exception e) {
                Throwable root = ExceptionUtils.getRootCause(e);
                logger.log(Level.WARNING, "Could not get baselines from " + s, root);
                consoleOutput.println("No baselines: " + root.getMessage());
            }
        }

        consoleOutput.println("");

        return baselines;
    }

    /**
     * Given the {@link Stream}, {@link Component} and
     * {@link net.praqma.clearcase.ucm.entities.Project.PromotionLevel} a list
     * of valid {@link Baseline}s is returned.
     *
     * @param workspace
     * @param plevel
     * @param stream
     * @param component
     * @return A list of {@link Baseline}'s
     * @throws CCUCMException
     */
    private List<Baseline> getValidBaselinesFromStream(FilePath workspace, Project.PromotionLevel plevel, Stream stream, Component component, Date date) throws IOException, InterruptedException {
        logger.fine("Retrieving valid baselines.");
        return RemoteUtil.getRemoteBaselinesFromStream(workspace, component, stream, plevel, this.getSlavePolling(), this.getMultisitePolling(), date);
    }

    /**
     * Returns the last {@link CCUCMBuildAction}, that has a valid
     * {@link Baseline}
     *
     * @param project
     * @return An Action
     */
    public static CCUCMBuildAction getLastAction(AbstractProject<?, ?> project) {
        for (AbstractBuild<?, ?> b = project.getLastBuild(); b != null; b = b.getPreviousBuild()) {
            CCUCMBuildAction action = b.getAction(CCUCMBuildAction.class);
            if (action != null && action.getBaseline() != null) {
                return action;
            }
        }

        return null;
    }

    private CCUCMBuildAction getBuildAction() throws UnableToInitializeEntityException {
        CCUCMBuildAction action = new CCUCMBuildAction(Stream.get(stream), Component.get(component));

        action.setDescription(setDescription);
        action.setMakeTag(makeTag);
        action.setRecommend(recommend);
        action.setForceDeliver(forceDeliver);
        action.setPromotionLevel(plevel);
        action.setUnstable(treatUnstable);
        action.setLoadModule(loadModule);
        action.setRemoveViewPrivateFiles(removeViewPrivateFiles);
        action.setTrimmedChangeSet(trimmedChangeSet);

        /* Deliver and template */
        action.setCreateBaseline(createBaseline);

        /* Trim template, strip out quotes */
        if (nameTemplate.matches("^\".+\"$")) {
            nameTemplate = nameTemplate.substring(1, nameTemplate.length() - 1);
        }
        action.setNameTemplate(nameTemplate);

        action.setPolling(polling);

        return action;
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        SCMRevisionState scmRS = null;

        if (bl != null) {
            scmRS = new SCMRevisionStateImpl();
        }
        return scmRS;
    }

    private Baseline selectBaseline(List<Baseline> baselines, Project.PromotionLevel plevel, FilePath workspace) throws IOException, InterruptedException {
        Baseline selected = null;
        if (baselines.size() > 0) {
            if (plevel != null) {
                selected = baselines.get(0);
            } else {
                selected = baselines.get(baselines.size() - 1);
            }

            return (Baseline) RemoteUtil.loadEntity(workspace, selected, true);
        } else {
            return null;
        }
    }

    private void printParameters(PrintStream ps) {
        ps.println("[" + Config.nameShort + "] Getting baselines for :");
        ps.println("[" + Config.nameShort + "] * Stream:          " + stream);
        ps.println("[" + Config.nameShort + "] * Component:       " + component);

        if (plevel == null) {
            ps.println("[" + Config.nameShort + "] * Promotion level: " + "ANY");
        } else {
            ps.println("[" + Config.nameShort + "] * Promotion level: " + plevel);
        }


        ps.println("");
    }

    public void printBaselines(List<Baseline> baselines, PrintStream ps) {
        if (baselines != null) {
            ps.println("[" + Config.nameShort + "] Retrieved " + baselines.size() + " baseline" + (baselines.size() == 1 ? "" : "s") + ":");
            if (!(baselines.size() > 8)) {
                for (Baseline b : baselines) {
                    ps.println("[" + Config.nameShort + "] + " + b.getShortname() + "(" + b.getDate() + ")");
                }
            } else {
                int i = baselines.size();
                ps.println("[" + Config.nameShort + "] + " + baselines.get(0).getShortname() + "(" + baselines.get(0).getDate() + ")");
                ps.println("[" + Config.nameShort + "] + " + baselines.get(1).getShortname() + "(" + baselines.get(1).getDate() + ")");
                ps.println("[" + Config.nameShort + "] + " + baselines.get(2).getShortname() + "(" + baselines.get(2).getDate() + ")");
                ps.println("[" + Config.nameShort + "]   ...");
                ps.println("[" + Config.nameShort + "] + " + baselines.get(i - 3).getShortname() + "(" + baselines.get(i - 3).getDate() + ")");
                ps.println("[" + Config.nameShort + "] + " + baselines.get(i - 2).getShortname() + "(" + baselines.get(i - 2).getDate() + ")");
                ps.println("[" + Config.nameShort + "] + " + baselines.get(i - 1).getShortname() + "(" + baselines.get(i - 1).getDate() + ")");
            }
        }
    }

    /*
     * The following getters and booleans (six in all) are used to display saved
     * userdata in Hudsons gui
     */
    public String getLevelToPoll() {
        return levelToPoll;
    }

    public String getComponent() {
        return component;
    }

    public String getStream() {
        return stream;
    }

    public String getLoadModule() {
        return loadModule;
    }

    /*
     * getStreamObject() and getBaseline() are used by CCUCMNotifier to get the
     * Baseline and Stream in use, but does not work with concurrent builds!!!
     */
    public Stream getStreamObject() {
        return integrationstream;
    }

    @Exported
    public String getBaseline() {
        return bl;
    }

    public boolean getSlavePolling() {
        CCUCMScm.CCUCMScmDescriptor desc = (CCUCMScm.CCUCMScmDescriptor) this.getDescriptor();
        return desc.getSlavePolling();

    }

    public boolean getMultisitePolling() {
        if (this.multisitePolling != null) {
            return this.multisitePolling;
        } else {
            CCUCMScm.CCUCMScmDescriptor desc = (CCUCMScm.CCUCMScmDescriptor) this.getDescriptor();
            return desc.getMultisitePolling();
        }
    }

    @Exported
    public String getPolling() {
        return polling.toString();
    }

    @Exported
    public String getTreatUnstable() {
        return treatUnstable.toString();
    }

    public String getBuildProject() {
        return buildProject;
    }

    public boolean getForceDeliver() {
        return forceDeliver;
    }

    public boolean isTrimmedChangeSet() {
        return trimmedChangeSet;
    }

    public boolean isRemoveViewPrivateFiles() {
        return removeViewPrivateFiles;
    }

    public boolean isCreateBaseline() {
        return this.createBaseline;
    }

    public String getNameTemplate() {
        return this.nameTemplate;
    }

    public boolean isMakeTag() {
        return this.makeTag;
    }

    public boolean isSetDescription() {
        return this.setDescription;
    }

    public boolean isRecommend() {
        return this.recommend;
    }

    public void setMultisitePolling(boolean mp) {
        this.multisitePolling = mp;
    }
    
    public boolean isDiscard() {
        return discard;
    }
    
    
    public void setDiscard(boolean discard) {
        this.discard = discard;
    }

    /**
     * This class is used to describe the plugin to Hudson
     *
     * @author Troels Selch
     * @author Margit Bennetzen
     *
     */
    @Extension
    public static class CCUCMScmDescriptor extends SCMDescriptor<CCUCMScm> implements hudson.model.ModelObject {

        private boolean slavePolling;
        private boolean multisitePolling;
        private List<String> loadModules;

        public CCUCMScmDescriptor() {
            super(CCUCMScm.class, null);
            loadModules = getLoadModules();
            load();
        }

        /**
         * This method is called, when the user saves the global Hudson
         * configuration.
         */
        @Override
        public boolean configure(org.kohsuke.stapler.StaplerRequest req, JSONObject json) throws Descriptor.FormException {
            try {
                String s = json.getString("slavePolling");
                if (s != null) {
                    slavePolling = Boolean.parseBoolean(s);
                }
                s = json.getString("multisitePolling");
                if (s != null) {
                    multisitePolling = Boolean.parseBoolean(s);
                }
            } catch (Exception e) {
                e.getMessage();
            }

            save();

            return true;
        }

        public boolean getSlavePolling() {
            return slavePolling;
        }

        public boolean getMultisitePolling() {
            return multisitePolling;
        }

        /**
         * This is called by Hudson to discover the plugin name
         */
        @Override
        public String getDisplayName() {
            return "ClearCase UCM";
        }

        /**
         * This method is called by the scm/CCUCM/global.jelly to validate the
         * input without reloading the global configuration page
         *
         * @param value
         * @return
         */
        public FormValidation doExecutableCheck(@QueryParameter String value) {
            return FormValidation.validateExecutable(value);
        }

        public FormValidation doCheckTemplate(@QueryParameter String value) throws FormValidation {
            try {
                NameTemplate.testTemplate(NameTemplate.trim(value));
                return FormValidation.ok("The template seems ok");
            } catch (TemplateException e) {
                throw FormValidation.error("Does not appear to be a valid template: " + e.getMessage());
            }
        }

        public void doLevelCheck(@QueryParameter String polling, @QueryParameter String level) throws FormValidation {
            System.out.println("LEVEL CHECK: " + polling + " + " + level);
            if (level.equalsIgnoreCase("any") && !polling.equals("self")) {
                throw FormValidation.error("You can only combine self and any");
            }
        }

        @Override
        public CCUCMScm newInstance(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            try {
                String polling = formData.getString("polling");
                String level = formData.getString("levelToPoll");

                if (level.equalsIgnoreCase("any")) {
                    if (!polling.equalsIgnoreCase("self")) {
                        throw new Descriptor.FormException("You can only use any with self polling", "polling");
                    }
                }
            } catch (JSONException e) {
                throw new Descriptor.FormException("You missed some fields: " + e.getMessage(), "CCUCM.polling");
            }
            CCUCMScm instance = req.bindJSON(CCUCMScm.class, formData);
            /* TODO This is actually where the Notifier check should be!!! */
            return instance;
        }

        /**
         * Used by Hudson to display a list of valid promotion levels to build
         * from. The list of promotion levels is hard coded in
         * net.praqma.hudson.Config.java
         *
         * @return
         */
        public List<String> getLevels() {
            return Config.getLevels();
        }

        /**
         * Used by Hudson to display a list of loadModules (whether to poll all
         * or only modifiable elements
         *
         * @return
         */
        public List<String> getLoadModules() {
            loadModules = new ArrayList<String>();
            loadModules.add("All");
            loadModules.add("Modifiable");
            return loadModules;
        }

        public FormValidation doCheckMode(@QueryParameter String mode, @QueryParameter String checked) throws IOException {
            boolean isChecked = checked.equalsIgnoreCase("true");
            if (isChecked) {
                if (mode.equals("self")) {
                    return FormValidation.warning("You cannot create a baseline in self mode!");
                } else {
                    return FormValidation.ok();
                }
            } else {
                return FormValidation.ok();
            }
        }
    }
}
