package net.praqma.hudson.scm;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.Cool;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.utils.BaselineList;
import net.praqma.hudson.Config;
import net.praqma.hudson.exception.ScmException;
import net.praqma.hudson.scm.PucmState.State;
import net.praqma.hudson.scm.StoredBaselines.StoredBaseline;
import net.praqma.util.debug.PraqmaLogger;
import net.praqma.util.debug.PraqmaLogger.Logger;
import net.praqma.util.structure.Tuple;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;

/**
 * Pucm is responsible for everything regarding Hudsons connection to ClearCase
 * pre-build. This class defines all the files required by the user. The
 * information can be entered on the config page.
 *
 * @author Troels Selch
 * @author Margit Bennetzen
 *
 */
public class PucmScm extends SCM {

    private String levelToPoll;
    private String loadModule;
    private String component;
    private String stream;
    private boolean newest;
    private String bl;
    private boolean compRevCalled;
    private StringBuffer pollMsgs = new StringBuffer();
    private Stream integrationstream;
    private boolean doPostBuild = true;
    private String buildProject;
    private boolean multiSite = false;
    private String jobName = "";
    private Integer jobNumber;
    private String id = "";
    private Logger logger = null;
    public static PucmState pucm = new PucmState();

    /* Threshold in milliseconds */
    public static final long __PUCM_STORED_BASELINES_THRESHOLD = 5 * 60 * 1000;
    public static StoredBaselines storedBaselines = new StoredBaselines();
    public static final String PUCM_LOGGER_STRING = "include_classes";
    private boolean pollChild;

    /**
     * Default constructor, mainly used for unit tests.
     */
    public PucmScm() {
    }

    /**
     *
     * @param component
     * @param levelToPoll
     * @param loadModule
     * @param stream
     * @param newest
     * @param multiSite
     * @param testing
     * @param buildProject
     */
    @DataBoundConstructor
    public PucmScm(String buildProject, String component, String levelToPoll, String loadModule, boolean multiSite, boolean newest,
            boolean pollChild, String stream) {

        /* Preparing the logger */
        logger = PraqmaLogger.getLogger();
        logger.subscribeAll();
        File rdir = new File(logger.getPath());
        logger.setLocalLog(new File(rdir + System.getProperty("file.separator") + "log.log"));

        /* Make sure the cool library is also affected */
        Cool.setLogger(logger);
        this.bl = bl;
        this.logger = PraqmaLogger.getLogger();
        logger.trace_function();
        logger.debug("PucmSCM constructor");
        this.component = component;
        this.levelToPoll = levelToPoll;
        this.loadModule = loadModule;
        this.stream = stream;
        this.newest = newest;
        this.buildProject = buildProject;
        this.multiSite = multiSite;

        /* Poll child streams */
        this.pollChild = pollChild;

    }

    /**
     * This methond handles all functionallity about creating the rigth
     * workspace folder for the job..
     */
    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile)
            throws IOException, InterruptedException {
        /* Prepare job variables */

        jobName = build.getParent().getDisplayName().replace(' ', '_');
        jobNumber = build.getNumber();
        this.id = "[" + jobName + "::" + jobNumber + "]";


        /* Preparing the logger */
        logger = PraqmaLogger.getLogger();
        logger.subscribeAll();
        File rdir = build.getRootDir();
        logger.setLocalLog(new File(rdir + System.getProperty("file.separator") + "log.log"));

        /* Make sure the cool library is also affected */
        Cool.setLogger(logger);

        // logger.print(bl.getFullyQualifiedName());

        logger.info(id + "PucmSCM checkout v. " + net.praqma.hudson.Version.version);
        boolean result = true;

        PrintStream consoleOutput = listener.getLogger();
        consoleOutput.println("[PUCM] Praqmatic UCM v. " + net.praqma.hudson.Version.version + " - SCM section started");
        consoleOutput.println("[PUCM] Polling childs streams: " + pollChild);

        /* Recalculate the states */
        int count = pucm.recalculate(build.getProject());
        logger.info(id + "Removed " + count + " from states.");

        doPostBuild = true;

        /* If we polled, we should get the same object created at that point */
        State state = pucm.getState(jobName, jobNumber);
        state.setLoadModule(loadModule);

        state.setLogger(logger);

        if (this.multiSite) {
            /* Get the time in milli seconds and store it to the state */
            state.setMultiSiteFrquency(((PucmScmDescriptor) getDescriptor()).getMultiSiteFrequencyAsInt() * 60000);
            logger.info(id + "Multi site frequency: " + state.getMultiSiteFrquency());
        } else {
            state.setMultiSiteFrquency(0);
        }

        logger.debug(id + "The initial state:\n" + state.stringify());
        if (this.pollChild) {
            try {
                /* Make deliver view */
                state.setPollChild(pollChild);
                String baselinevalue = "";
                try {
                    /* Determining the pucm_baseline modifier */
                    if (build.getBuildVariables().get(baselinevalue) != null) {
                        String baselinename = (String) build.getBuildVariables().get(baselinevalue);
                        try {
                            state.setBaseline(UCMEntity.getBaseline(baselinename));
                            consoleOutput.println("[PUCM] Starting parameterized build with a pucm_baseline.\n[PUCM] Using baseline: " + baselinename
                                    + " from integrationstream " + state.getBaseline().getStream().getShortname());

                            /* The component could be used in the post build section */
                            state.setComponent(state.getBaseline().getComponent());
                            state.setStream(UCMEntity.getStream(stream));
                            logger.debug(id + "Saving the component for later use");
                        } catch (UCMException e) {
                            consoleOutput.println("[PUCM] Could not find baseline from parameter '" + baselinename + "'.");
                            state.setPostBuild(false);
                            result = false;
                            state.setBaseline(null);
                        }
                    } else {

                        printParameters(consoleOutput);
                        state.setStream(UCMEntity.getStream(stream));
                        if (!state.isAddedByPoller()) {
                            try {
                                List<Stream> streams = UCMEntity.getStream(stream).getChildStreams();
                                List<Baseline> baselines = null;
                                for (Stream s : streams) {
                                    try {
                                        baselines = getValidBaselines(build.getProject(), state, Project.getPlevelFromString(levelToPoll), s);
                                    } catch (ScmException e) {
                                        //We won't throw exceptions just because there are no baselines on this particulare stream
                                        //we are moving forward...
                                        ;
                                    }
                                    if (state.getBaselines() == null) {
                                        state.setBaselines(baselines);
                                    } else {
                                        for (Baseline b : baselines) {
                                            state.getBaselines().add(b);
                                        }
                                    }
                                }
                                state.setBaseline(selectBaseline(state.getBaselines(), newest));
                                state.setStream(UCMEntity.getStream(stream));
                            } catch (UCMException e) {
                                consoleOutput.println("[PUCM] " + e.getMessage());
                                result = false;
                            }

                            /* if we did not find any baselines we should return false */
                            if (state.getBaselines().size() < 1) {
                                result = false;
                            }

                            /* Print the baselines to jenkins out */
                            printBaselines(state.getBaselines(), consoleOutput);
                        }
                    }
                    RemoteDeliver rmDeliver = new RemoteDeliver(UCMEntity.getStream(stream).getFullyQualifiedName(), listener, component, loadModule, state.getBaseline().getFullyQualifiedName(), build.getParent().getDisplayName());

                    int i = workspace.act(rmDeliver);
                    /*Next line must be after the line above*/
                    state.setSnapView(rmDeliver.getSnapShotView());
                    build.setDescription("<small>" + state.getBaseline() + "</small>");
                } catch (IOException e) {
                    consoleOutput.println("[PUCM] " + e.getMessage());
                    result = false;
                } catch (UCMException e) {
                    consoleOutput.println("[PUCM] " + e.getMessage());
                    result = false;
                }
                state.setStream(UCMEntity.getStream(stream));
                return result;
            } catch (UCMException ex) {
                java.util.logging.Logger.getLogger(PucmScm.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        logger.debug(id + "The initial state:\n" + state.stringify());

        /* Determining the pucm_baseline modifier */
        String baselinevalue = "";
        Collection<?> c = build.getBuildVariables().keySet();
        Iterator<?> i = c.iterator();

        while (i.hasNext()) {
            String next = i.next().toString();
            if (next.equalsIgnoreCase("pucm_baseline")) {
                baselinevalue = next;
            }
        }

        /* The special pucm_baseline case */

        if (build.getBuildVariables().get(baselinevalue) != null) {
            String baselinename = (String) build.getBuildVariables().get(baselinevalue);
            try {
                state.setBaseline(UCMEntity.getBaseline(baselinename));
                state.setStream(state.getBaseline().getStream());
                consoleOutput.println("[PUCM] Starting parameterized build with a pucm_baseline.\n[PUCM] Using baseline: " + baselinename
                        + " from integrationstream " + state.getStream().getShortname());

                /* The component could be used in the post build section */
                state.setComponent(state.getBaseline().getComponent());
                state.setStream(state.getBaseline().getStream());
                logger.debug(id + "Saving the component for later use");
            } catch (UCMException e) {
                consoleOutput.println("[PUCM] Could not find baseline from parameter '" + baselinename + "'.");
                state.setPostBuild(false);
                result = false;
                state.setBaseline(null);
            }
        } /* Default stream + component case */ else {

            printParameters(consoleOutput);

            // compRevCalled tells whether we have polled for baselines to build
            // -
            // so if we haven't polled, we do it now
            // if( !compRevCalled )
            if (!state.isAddedByPoller()) {
                try {
                    List<Baseline> baselines = getValidBaselines(build.getProject(), state, Project.getPlevelFromString(levelToPoll), state.getBaseline().getStream());
                    state.setBaselines(baselines);
                    Baseline baseline = selectBaseline(baselines, newest);
                    logger.debug(id + "I chose " + baseline);
                    state.setBaseline(baseline);
                } catch (UCMException e) {
                    consoleOutput.print("[PUCM] " + e.getMessage());
                } catch (ScmException e) {
                    consoleOutput.println("[PUCM] " + e.getMessage());
                    result = false;
                }

                /* Print the baselines to jenkins out */
                printBaselines(state.getBaselines(), consoleOutput);
            }
        }

        /* If a baseline is found */
        if (state.getBaseline() != null) {
            consoleOutput.println("[PUCM] building baseline " + state.getBaseline());

            try {
                /* Force the Baseline to be loaded */
                try {
                    state.getBaseline().load();
                } catch (UCMException e) {
                    logger.debug(id + "Could not load Baseline");
                    consoleOutput.println("[PUCM] Could not load Baseline.");
                }

                /*
                 * Check parameters TODO This should be deleted....
                 */
                if (listener == null) {
                    consoleOutput.println("[PUCM] Listener is null");
                }

                if (jobName == null) {
                    consoleOutput.println("[PUCM] jobname is null");
                }

                if (build == null) {
                    consoleOutput.println("[PUCM] BUILD is null");
                }

                if (stream == null) {
                    consoleOutput.println("[PUCM] stream is null");
                }

                if (loadModule == null) {
                    consoleOutput.println("[PUCM] loadModule is null");
                }

                if (buildProject == null) {
                    consoleOutput.println("[PUCM] buildProject is null");
                }

                build.setDescription("<small>" + state.getBaseline() + "</small>");
                CheckoutTask ct = new CheckoutTask(listener, jobName, build.getNumber(), state.getStream().getFullyQualifiedName(), loadModule, state.getBaseline().getFullyQualifiedName(), buildProject, logger);

                Tuple<String, String> ctresult = workspace.act(ct);
                String changelog = ctresult.t1;
                logger.empty(ctresult.t2);

                /* Write change log */
                try {
                    FileOutputStream fos = new FileOutputStream(changelogFile);
                    fos.write(changelog.getBytes());
                    fos.close();
                } catch (IOException e) {
                    logger.debug(id + "Could not write change log file");
                    consoleOutput.println("[PUCM] Could not write change log file");
                }

            } catch (Exception e) {
                consoleOutput.println("[PUCM] An unknown error occured: " + e.getMessage());
                logger.warning(e);
                e.printStackTrace(consoleOutput);
                doPostBuild = false;
                state.setPostBuild(false);
                result = false;
            }
        }

        return result;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        logger.trace_function();
        return new ChangeLogParserImpl();
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, Map<String, String> env) {
        super.buildEnvVars(build, env);

        State state = pucm.getState(jobName, jobNumber);

        if (state.getBaseline() != null) {
            env.put("CC_BASELINE", state.getBaseline().getFullyQualifiedName());
        } else {
            env.put("CC_BASELINE", "");
        }
    }

    /**
     * This method polls the version control system to see if there are any
     * changes in the source code.
     *
     */
    @Override
    public PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener,
            SCMRevisionState rstate) throws IOException, InterruptedException {
        /* Preparing the logger */
        logger = PraqmaLogger.getLogger();

        this.id = "[" + project.getDisplayName() + "::" + project.getNextBuildNumber() + "]";

        /*
         * Make a state object, which is only temporary, only to determine if
         * there's baselines to build this object will be stored in checkout
         */
        jobName = project.getDisplayName().replace(' ', '_');
        jobNumber = project.getNextBuildNumber(); /*
         * This number is not the
         * final job number
         */

        State state = pucm.getState(jobName, jobNumber);
        state.setAddedByPoller(true);

        if (this.multiSite) {
            /* Get the time in milli seconds and store it to the state */
            state.setMultiSiteFrquency(((PucmScmDescriptor) getDescriptor()).getMultiSiteFrequencyAsInt() * 60000);
            logger.info(id + "Multi site frequency: " + state.getMultiSiteFrquency());
        } else {
            state.setMultiSiteFrquency(0);
        }

        PrintStream consoleOut = listener.getLogger();
        printParameters(consoleOut);

        PollingResult p = null;
        consoleOut.println("[PUCM] polling on child streams: " + pollChild);
        if (this.pollChild) {
            consoleOut.println("[PUCM] we wont poll on the integration stream insted we are looking for all new baselines on all stream that has the defined int stream as there default deliver target");
            try {
                // Finds all child stream this integration stream..
                List<Stream> cStreams = UCMEntity.getStream(this.stream).getChildStreams();

                for (Stream stream : cStreams) {
                    state.setStream(stream);
                    PollingResult tempP = getPossibleBaselines(project, state, listener);

                    if (p == null || p == PollingResult.NO_CHANGES) {
                        p = tempP;
                    } else {
                        p = tempP;
                    }
                }
                state.setStream(UCMEntity.getStream(this.stream));
            } catch (UCMException e) {
                consoleOut.println(e.getMessage());
            }

            logger.debug(id + "FINAL Polling result = " + p.change.toString());

            logger.unsubscribeAll();

            return p;
        }

        return getPossibleBaselines(project, state, listener);
    }

    private PollingResult getPossibleBaselines(AbstractProject<?, ?> project, State state, TaskListener listener) {

        List<Baseline> baselines = null;
        PrintStream consoleOut = listener.getLogger();
        printParameters(consoleOut);
        PollingResult p;

        try {
            baselines = getValidBaselines(project, state, Project.getPlevelFromString(levelToPoll), state.getStream());
            printBaselines(baselines, consoleOut);
            state.setBaselines(baselines);
            Baseline baseline = selectBaseline(baselines, newest);
            logger.info(id + "Using " + baseline);
            state.setBaseline(baseline);
            compRevCalled = true;

        } catch (ScmException e) {
            e.printStackTrace(consoleOut);
            consoleOut.println(pollMsgs + "\n[PUCM] " + e.getMessage());
            pollMsgs = new StringBuffer();
            logger.debug(id + "Removed job " + state.getJobNumber() + " from list");
            state.remove();
        }

        if (baselines.size() > 0) {
            p = PollingResult.BUILD_NOW;
        } else {
            p = PollingResult.NO_CHANGES;
        }

        logger.debug(id + "FINAL Polling result = " + p.change.toString());

        logger.unsubscribeAll();

        return p;

    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener) throws IOException,
            InterruptedException {
        SCMRevisionState scmRS = null;

        if (bl != null) {
            scmRS = new SCMRevisionStateImpl();
        }
        return scmRS;
    }

    private Baseline selectBaseline(List<Baseline> baselines, boolean newest) {
        if (baselines.size() > 0) {
            if (newest) {
                return baselines.get(baselines.size() - 1);
            } else {
                return baselines.get(0);
            }
        } else {
            return null;
        }
    }

    private List<Baseline> getValidBaselines(AbstractProject<?, ?> project, State state, Project.Plevel plevel, Stream stream) throws ScmException {
        logger.debug(id + "Retrieving valid baselines.");

        /* Store the component to the state */
        try {
            state.setComponent(UCMEntity.getComponent(component, false));
        } catch (UCMException e) {
            throw new ScmException("Could not get component. " + e.getMessage());
        }

        /* Store the stream to the state */
        //state.setStream(stream);

        state.setPlevel(plevel);
        logger.debug(id + "GetBaseline state:\n" + state.stringify());

        /* The baseline list */
        BaselineList baselines = null;

        try {
            baselines = state.getComponent().getBaselines(stream, plevel);
        } catch (UCMException e) {
            System.exit(-1);
            throw new ScmException("Could not retrieve baselines from repository. " + e.getMessage());
        }
        logger.debug(id + "GetBaseline state:\n" + state.stringify());
        List<Baseline> validBaselines = new ArrayList<Baseline>();
        if (baselines.size() >= 1) {
            logger.debug(id + "PUCM=" + pucm.stringify());

            if (state.isMultiSite()) {
                /* Prune the stored baselines */
                int pruned = PucmScm.storedBaselines.prune(state.getMultiSiteFrquency());
                logger.info(id + "I pruned " + pruned + " baselines from cache with threshold "
                        + StoredBaselines.milliToMinute(state.getMultiSiteFrquency()) + "m");
                logger.debug(id + "My stored baselines:\n" + PucmScm.storedBaselines.toString());
            }

            try {
                /* For each baseline in the list */
                for (Baseline b : baselines) {
                    /* Get the state for the current baseline */
                    State cstate = pucm.getStateByBaseline(jobName, b.getFullyQualifiedName());

                    /* Find the stored baseline if multi site, null if not */
                    StoredBaseline sbl = null;
                    if (state.isMultiSite()) {
                        /* Find the baseline if stored */
                        sbl = PucmScm.storedBaselines.getBaseline(b.getFullyQualifiedName());
                        logger.debug(id + "The found stored baseline: " + sbl);
                    }

                    /*
                     * The baseline is in progress, determine if the job is
                     * still running
                     */
                    if (cstate != null) {
                        Integer bnum = cstate.getJobNumber();
                        Object o = project.getBuildByNumber(bnum);
                        Build bld = (Build) o;

                        /* The job is not running */
                        if (!bld.isLogUpdated()) {
                            logger.debug(id + "Job " + bld.getNumber() + " is not building");

                            /*
                             * Verify that the found baseline has the same
                             * promotion as the stored(if stored)
                             */
                            if (sbl == null || sbl.plevel == b.getPromotionLevel(true)) {
                                logger.debug(id + b + " was added to selected list");
                                validBaselines.add(b);
                            }
                        } else {
                            logger.debug(id + "Job " + bld.getNumber() + " is building " + cstate.getBaseline().getFullyQualifiedName());
                        }
                    } /* The baseline is available */ else {
                        /*
                         * Verify that the found baseline has the same promotion
                         * as the stored(if stored)
                         */
                        if (sbl == null || sbl.plevel == b.getPromotionLevel(true)) {
                            logger.debug(id + b + " was added to selected list");
                            validBaselines.add(b);
                        }
                    }
                }

                if (validBaselines.size() == 0) {
                    logger.log(id + "No baselines available on chosen parameters.");
                    throw new ScmException("No baselines available on chosen parameters.");
                }

            } catch (UCMException e) {
                throw new ScmException("Could not get recommended baselines. " + e.getMessage());
            }
        } else {
            throw new ScmException("No baselines on chosen parameters.");
        }

        return validBaselines;
    }

    private void printParameters(PrintStream ps) {
        ps.println("[PUCM] Getting baselines for :");
        ps.println("[PUCM] * Stream:          " + stream);
        ps.println("[PUCM] * Component:       " + component);
        ps.println("[PUCM] * Promotion level: " + levelToPoll);
        ps.println("");
    }

    public void printBaselines(List<Baseline> baselines, PrintStream ps) {
        if (baselines != null) {
            ps.println("[PUCM] Retrieved baselines:");
            if (!(baselines.size() > 20)) {
                for (Baseline b : baselines) {
                    ps.println("[PUCM] + " + b.getShortname());
                }
            } else {
                int i = baselines.size();
                ps.println("[PUCM] + " + baselines.get(0).getShortname());
                ps.println("[PUCM] + " + baselines.get(1).getShortname());
                ps.println("[PUCM] + " + baselines.get(2).getShortname());
                ps.println("[PUCM]   ...(" + (i - 6) + " baselines not shown)...");
                ps.println("[PUCM] + " + baselines.get(i - 3).getShortname());
                ps.println("[PUCM] + " + baselines.get(i - 2).getShortname());
                ps.println("[PUCM] + " + baselines.get(i - 1).getShortname());
            }
        }
    }

    /*
     * The following getters and booleans (six in all) are used to display saved
     * userdata in Hudsons gui
     */
    public boolean getMultiSite() {
        return this.multiSite;
    }

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

    public boolean isNewest() {
        return newest;
    }

    /*
     * getStreamObject() and getBaseline() are used by PucmNotifier to get the
     * Baseline and Stream in use, but does not work with concurrent builds!!!
     */
    public Stream getStreamObject() {
        return integrationstream;
    }

    @Exported
    public String getBaseline() {
        return bl;
    }

    @Exported
    public boolean getPollChild() {
        return pollChild;
    }

    @Exported
    public String getBl() {
        return bl;
    }

    @Exported
    public boolean doPostbuild() {
        return doPostBuild;
    }

    public String getBuildProject() {
        return buildProject;
    }

    /**
     * This class is used to describe the plugin to Hudson
     *
     * @author Troels Selch
     * @author Margit Bennetzen
     *
     */
    @Extension
    public static class PucmScmDescriptor extends SCMDescriptor<PucmScm> implements hudson.model.ModelObject {

        private String cleartool;
        private String multiSiteFrequency;
        private List<String> loadModules;

        public PucmScmDescriptor() {
            super(PucmScm.class, null);
            loadModules = getLoadModules();
            load();
            Config.setContext();
        }

        /**
         * This method is called, when the user saves the global Hudson
         * configuration.
         */
        @Override
        public boolean configure(org.kohsuke.stapler.StaplerRequest req, JSONObject json) throws FormException {
            /* For backwards compatibility, check if parameters are null */

            cleartool = json.getString("PUCM.cleartool");
            if (cleartool != null) {
                cleartool = cleartool.trim();
            }

            multiSiteFrequency = json.getString("PUCM.multiSiteFrequency");
            if (multiSiteFrequency != null) {
                multiSiteFrequency = multiSiteFrequency.trim();
            }

            save();
            return true;
        }

        /**
         * This is called by Hudson to discover the plugin name
         */
        @Override
        public String getDisplayName() {
            return "Praqmatic UCM";
        }

        /**
         * This method is called by the scm/Pucm/global.jelly to validate the
         * input without reloading the global configuration page
         *
         * @param value
         * @return
         */
        public FormValidation doExecutableCheck(@QueryParameter String value) {
            return FormValidation.validateExecutable(value);
        }

        /**
         * Called by Hudson. If the user does not input a command for Hudson to
         * use when polling, default value is returned
         *
         * @return
         */
        public String getCleartool() {
            if (cleartool == null || cleartool.equals("")) {
                return "cleartool";
            }
            return cleartool;
        }

        public String getMultiSiteFrequency() {
            return multiSiteFrequency;
        }

        public int getMultiSiteFrequencyAsInt() {
            try {
                return Integer.parseInt(multiSiteFrequency);
            } catch (Exception e) {
                return 0;
            }
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
    }
}
