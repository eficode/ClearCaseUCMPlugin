package net.praqma.hudson.scm;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.Cause.UserIdCause;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import net.praqma.clearcase.exceptions.CleartoolException;

import net.praqma.clearcase.exceptions.DeliverException;
import net.praqma.clearcase.exceptions.DeliverException.Type;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.ucm.entities.*;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.Config;
import net.praqma.hudson.PromotionListAction;
import net.praqma.hudson.Util;
import net.praqma.hudson.exception.CCUCMException;
import net.praqma.hudson.exception.DeliverNotCancelledException;
import net.praqma.hudson.exception.TemplateException;
import net.praqma.hudson.nametemplates.FileFoundable;
import net.praqma.hudson.nametemplates.NameTemplate;
import net.praqma.hudson.notifier.CCUCMNotifier;
import net.praqma.hudson.remoting.*;
import net.praqma.hudson.remoting.deliver.GetChanges;
import net.praqma.hudson.remoting.deliver.MakeDeliverView;
import net.praqma.hudson.remoting.deliver.StartDeliver;
import static net.praqma.hudson.scm.CCUCMScm.getLastAction;
import net.praqma.hudson.scm.Polling.PollingType;
import net.praqma.hudson.scm.pollingmode.BaselineCreationEnabled;
import net.praqma.hudson.scm.pollingmode.NewestFeatureToggle;
import net.praqma.hudson.scm.pollingmode.PollChildMode;
import net.praqma.hudson.scm.pollingmode.PollRebaseMode;
import net.praqma.hudson.scm.pollingmode.PollSelfMode;
import net.praqma.hudson.scm.pollingmode.PollSiblingMode;
import net.praqma.hudson.scm.pollingmode.PollSubscribeMode;
import net.praqma.hudson.scm.pollingmode.PollingMode;
import net.praqma.util.execute.AbnormalProcessTerminationException;
import net.praqma.util.structure.Tuple;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;

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

    public static final String HLINK_DEFAULT = "AlternateDeliverTarget";
    private static final Logger logger = Logger.getLogger(CCUCMScm.class.getName());
    
    private Boolean multisitePolling;
    private String loadModule;
    
    @Deprecated
    private String component;
    private String stream;
    private String bl;
    
    private Stream integrationstream;
    private String buildProject;
    private String jobName = "";    
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

    private String nameTemplate;
    
    private String viewtag = "";    
    private boolean addPostBuild = true;    
    private PollingMode mode;

    @Deprecated
    private Project.PromotionLevel plevel;
    @Deprecated
    private Polling polling = null;    
    @Deprecated
    private boolean createBaseline;    
    @Deprecated
    private String levelToPoll;
    @Deprecated
    private transient StringBuffer pollMsgs = null;
    @Deprecated
    private transient Integer jobNumber = null;    
    @Deprecated
    private Baseline lastBaseline = null;
    
    
    /**
     * Default constructor, mainly used for unit tests.
     */
    public CCUCMScm() {
        discard = false;
    }    
    
    @DataBoundConstructor
    public CCUCMScm(String loadModule, boolean newest, PollingMode mode, String stream, String treatUnstable, String nameTemplate, boolean forceDeliver, boolean recommend, boolean makeTag, boolean setDescription, String buildProject, boolean removeViewPrivateFiles, boolean trimmedChangeSet, boolean discard) {
        this.mode = mode;
        this.loadModule = loadModule;
        this.stream = stream;
        this.buildProject = buildProject;
        this.treatUnstable = new Unstable(treatUnstable);
        this.nameTemplate = nameTemplate;
        this.forceDeliver = forceDeliver;
        this.removeViewPrivateFiles = removeViewPrivateFiles;
        this.trimmedChangeSet = trimmedChangeSet;
        this.recommend = recommend;
        this.makeTag = makeTag;
        this.setDescription = setDescription;        
        this.discard = discard;
    }
    
    @Deprecated
    public CCUCMScm(String component, String loadModule, boolean newest, PollingMode mode, String stream, String treatUnstable, String nameTemplate, boolean forceDeliver, boolean recommend, boolean makeTag, boolean setDescription, String buildProject, boolean removeViewPrivateFiles, boolean trimmedChangeSet, boolean discard) {
        this.mode = mode;
        this.component = component;
        this.loadModule = loadModule;
        this.stream = stream;
        this.buildProject = buildProject;
        this.treatUnstable = new Unstable(treatUnstable);
        this.nameTemplate = nameTemplate;
        this.forceDeliver = forceDeliver;
        this.removeViewPrivateFiles = removeViewPrivateFiles;
        this.trimmedChangeSet = trimmedChangeSet;
        this.recommend = recommend;
        this.makeTag = makeTag;
        this.setDescription = setDescription;        
        this.discard = discard;
    }    
    
    
    public Object readResolve() {
        if(polling != null) {
            if(polling.isPollingChilds()) {
                mode = new PollChildMode(levelToPoll);
                ((PollChildMode)mode).setCreateBaseline(createBaseline);                
            } else if(polling.isPollingSelf()) {
                mode = new PollSelfMode(levelToPoll);
            } else {
                mode = new PollSiblingMode(levelToPoll);
                ((PollSiblingMode)mode).setCreateBaseline(createBaseline);
                ((PollSiblingMode)mode).setUseHyperLinkForPolling(false);
            }
        }
        
        if(levelToPoll != null) {
            if(mode != null ) {
                mode.setLevelToPoll(levelToPoll);
            } 
       }
        
        if(component != null) {
            mode.setComponent(component);
        }
        
        return this;
    }
    
    private Project.PromotionLevel _getPlevel() {
        if(mode == null && plevel != null) {
            //Get old value
            return plevel;
        }
        return mode.getPromotionLevel();
    }
    
    private String _getComponent() {
        if(mode != null && mode.getComponent() != null) {
            return mode.getComponent();
        }
        return component;
    } 
    
    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {
        /* Prepare job variables */
        jobName = build.getParent().getDisplayName().replace(' ', '_');

        PrintStream out = listener.getLogger();
        
        /* Printing short description of build */
        String version = Jenkins.getInstance().getPlugin("clearcase-ucm-plugin").getWrapper().getVersion();
        out.println("[" + Config.nameShort + "] ClearCase UCM Plugin version " + version);
        out.println("[" + Config.nameShort + "] Allow for slave polling: " + this.getSlavePolling());
        out.println("[" + Config.nameShort + "] Poll for posted deliveries: " + this.getMultisitePolling());
        out.println( String.format( "%s Trim changeset: %s", "[" + Config.nameShort + "]", trimmedChangeSet) );

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
        
        if (addPostBuild) {
            ensurePublisher(build);
        }

        /* The special Baseline parameter case */
        if (build.getBuildVariables().get(baselineInput) != null) {
            logger.fine("Baseline parameter: " + baselineInput);
            action.setPolling(new Polling(PollingType.none));            
            try {
                resolveBaselineInput(build, baselineInput, action, listener);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Resolving baseline input failed", e);
                Util.println(out, "No Baselines found");
            }
        } else {
            out.println("[" + Config.nameShort + "] Polling streams: " + _getPolling().toString());
            try {                
                //Set the result
                Result r = resolveBaseline(build, build.getProject(), action, listener);
                if(r != null) {
                    build.setResult(r);
                }
            } catch (CCUCMException e) {

                logger.warning(e.getMessage());
                /* If the promotion level is not set, ANY, use the last found Baseline, or the manual build was triggered */
                if (mode.getPromotionLevel() == null || build.getCause(UserIdCause.class) != null) {
                    logger.fine("Configured to use the latest always.");
                    CCUCMBuildAction last = getLastAction(build.getProject());
                    if (last != null) {
                        action.setBaseline(last.getBaseline());
                    } else {
                        build.setDescription("No valid baselines found");
                        throw new AbortException("No valid baselines found");
                    }
                } else {
                    build.setDescription("No valid baselines found");
                    throw new AbortException("No valid baselines found");
                }
            } catch (AbortException abex) {
                throw abex;
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
            if (_getPolling().isPollingSelf() || !_getPolling().isPolling() || _getPolling().isPollingSubscribe()) {
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
    
    /**
     * Compatability method to return the correct method of polling.
     * 
     * @return 
     */
    private Polling _getPolling() {
        if(mode != null) {
            return mode.getPolling();
        }
        return polling;
    }
    
    /**
     * Compatability method to return the correct method of polling.
     * 
     * @return 
     */
    private boolean _getCreateBaseline() {
        if(mode != null) {
            if(mode instanceof BaselineCreationEnabled) {
                return ((BaselineCreationEnabled)mode).isCreateBaseline();
            }
        }
        return createBaseline;
    }

    @Override
    public void postCheckout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener) throws IOException, InterruptedException {        
        logger.fine("BEGINNING POSTCHECKOUT");
        
        CCUCMBuildAction state = build.getAction(CCUCMBuildAction.class);
        PrintStream consoleOutput = listener.getLogger();

        /* This is really only interesting if child or sibling polling */
        if (_getPolling().isPollingOther()) {
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
        } else if(_getPolling().isPollingRebase()) {
            try {
                logger.fine(String.format( "Starting rebase operation in view %s", state.getViewTag() ));
                consoleOutput.println(String.format("%s Starting rebase operation in view %s", "[" + Config.nameShort + "]", state.getViewTag()));
                if(!state.getRebaseTargets().isEmpty()) {
                    logger.fine(String.format("%s Rebasing to the following baselines:","[" + Config.nameShort + "]"));
                    consoleOutput.println(String.format("%s Rebasing to the following baselines:","[" + Config.nameShort + "]"));
                    for(Baseline bline : state.getRebaseTargets()) {
                        logger.fine(String.format("%s + %s","[" + Config.nameShort + "]", bline));
                        consoleOutput.println(String.format("%s + %s","[" + Config.nameShort + "]", bline));
                    }
                }
                
                consoleOutput.println( String.format( "%s The new struture encompases the following baselines:","[" + Config.nameShort + "]") );
                for(Baseline blstruct : state.getNewFoundationStructure()) {
                    consoleOutput.println(String.format("%s * %s","[" + Config.nameShort + "]", blstruct.getNormalizedName()));
                }
                
                build.getWorkspace().act(new RebaseTask(state.getStream(), state.getRebaseTargets(), listener, state.getViewTag(), false));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Unable to begin rebase on stream"+state.getStream(), ex);
                throw new AbortException("Unable to begin rebase on stream "+state.getStream());
            } 
        //With poll subscribe we can already now determine the build status.
        } else if(_getPolling().isPollingSubscribe()) {
            
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

    /**
     * This method validates the naming template that has been selected for when a new baseline is created by the plugin
     * Furthermore there is a check to seel
     * @param listener
     * @return 
     */
    private boolean checkInput(TaskListener listener) {
        PrintStream out = listener.getLogger();
        out.println("[" + Config.nameShort + "] Verifying input");
        out.println("[" + Config.nameShort + "] Polling using "+_getPolling().getType().toString());
        
        /* Check baseline template */
        if (_getCreateBaseline()) {
                
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
            
        }
        return true;
    }

    private boolean initializeWorkspace(AbstractBuild<?, ?> build, FilePath workspace, File changelogFile, BuildListener listener, CCUCMBuildAction action) throws IOException, InterruptedException {
        PrintStream consoleOutput = listener.getLogger();
        CheckoutTask ct = new CheckoutTask(listener, jobName, build.getNumber(), action.getStream(), loadModule, action.getBaseline(), buildProject, (_getPlevel() == null), action.doRemoveViewPrivateFiles());
        EstablishResult er = workspace.act(ct);

        String changelog = Util.createChangelog(build, er.getActivities(), action.getBaseline(), trimmedChangeSet, er.getView().getViewRoot(), er.getView().getReadOnlyLoadLines(), discard);
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
    
    private List<String> parseExclusionList(FilePath workspace, String exclude) throws IOException, InterruptedException {
        
        List<String> excludes = new ArrayList<String>();
        Pattern p_file = Pattern.compile("\\[file=(.*)\\]");
        
        for(String s : exclude.split("[\\r\\n]+")) {
            logger.finest(String.format("Excluding %s", s));
            Matcher m = p_file.matcher(s);            
            if(m.matches()) {
                logger.finest("Found match on file template, getting file contents");
                String fileName = m.group(1);
                String contentsOfFile = workspace.act(new FileFoundable(fileName));
                logger.finest(String.format("Found the follwing content in ignore file:%n%s", contentsOfFile));                    
                excludes.addAll(Arrays.asList(contentsOfFile.split(System.lineSeparator())));                
            } else {
                excludes.add(s);
            }             
        }
        
        logger.finest("Done excluding");        
        return excludes;
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
    private Result resolveBaseline(AbstractBuild<?,?> build, AbstractProject<?, ?> project, CCUCMBuildAction action, BuildListener listener) throws IOException, InterruptedException, CCUCMException {
        Result r = null;
        logger.fine("Resolving Baseline from the Stream " + action.getStream().getNormalizedName());
        PrintStream out = listener.getLogger();

        printParameters(out);

        /* The Stream must be loaded */
        action.setStream((Stream) RemoteUtil.loadEntity(build.getWorkspace(), action.getStream(), getSlavePolling()));

        List<Baseline> baselines = null;

        /* We need to discriminate on promotion level, JENKINS-16620 */
        Date date = null;
        if (isUseLatestAlways(mode)) {
            CCUCMBuildAction lastAction = getLastAction(project);
            if (lastAction != null) {
                date = lastAction.getBaseline().getDate();
            }
        }

        /* Find the Baselines and store them, none of the methods returns null! At least an empty list */
        /* Old skool self polling */        

        if (_getPolling().isPollingSelf()) {
            baselines = getValidBaselinesFromStream(build.getWorkspace(), _getPlevel(), action.getStream(), action.getComponent(), date);
        } else if(_getPolling().isPollingOther()) {
            baselines = getBaselinesFromStreams(build.getWorkspace(), listener, out, action.getStream(), action.getComponent(), _getPolling(), date);
        } else if(_getPolling().isPollingSubscribe()) {            
            try {
                //Poll self method of finding baselines
                List<Baseline> currentBls = RemoteUtil.getRemoteBaselinesFromStream(build.getWorkspace(), action.getComponent(), action.getStream(), _getPlevel(), this.getSlavePolling(), this.getMultisitePolling(), date);
                
                //Get our candidate baseline
                Baseline blCandidate = selectBaseline(currentBls, mode, build.getWorkspace());
                if(blCandidate != null) {
                    logger.fine( String.format( "Examining baseline candidate %s", blCandidate ) );
                }
                
                //Get baselines on the currently selected components
                PollSubscribeMode subMode = (PollSubscribeMode)mode;
                List<Baseline> consideredBaselines = subMode.getBaselinesToConsider(blCandidate, build.getWorkspace(), getSlavePolling());
                
                //We only promote those baselines that was created as a consequence of the parent. That means that any labelled baselines should get matched.
                //This is done by convention.
                ArrayList<Baseline> selections = new ArrayList<Baseline>();                
                if(blCandidate != null) {
                    for(Baseline blcforprom : consideredBaselines) {
                         if(blcforprom.getShortname().startsWith(blCandidate.getShortname())) {
                             selections.add(blcforprom);
                         }
                    }                    
                }
                
                //Add these baselines to an action.
                build.addAction(new PromotionListAction(selections));
                

                Tuple<Result,List<Baseline>> rez = getValidBaselinesFromStreamWithSubscribe(currentBls, consideredBaselines, build.getWorkspace(), getSlavePolling());
                baselines = rez.t2;
                r = rez.t1;
                
            } catch (UnableToInitializeEntityException ex) {
                logger.log(Level.SEVERE, "Error in resolveBaseline, unable to initialize entity", ex);
                throw new IOException("Error in resolveBaseline, unable to initialize entity", ex);
            } catch (CleartoolException ex) {
                logger.log(Level.SEVERE, "Error in resolveBaseline, cleartool exception", ex);
                throw new IOException("Error in resolveBaseline, cleartool exception", ex);
            }
        } else {            
            PollRebaseMode md = (PollRebaseMode)mode;
            List<String> parsedList = parseExclusionList(build.getWorkspace(), md.getExcludeList());
            Tuple<List<Baseline>,List<Baseline>> results = getBaselinesForPollRebase(build.getWorkspace(), listener, action.getStream(), parsedList);
            baselines = results.t1;
            action.setRebaseTargets(baselines);
            action.setNewFoundationStructure(results.t2);            
        }

        /* if we did not find any baselines we should return false */
        if (baselines.size() < 1) {
            throw new CCUCMException("No valid Baselines found");
        }        

        /* Select and load baseline */
        Baseline blSelected = selectBaseline(baselines, mode, build.getWorkspace());
        action.setBaseline(blSelected);
        
        
        /* Print the baselines to jenkins out */
        printBaselines(baselines, out);
        out.println("");
        
        if(_getPolling().isPollingRebase()) {
            PollRebaseMode md = (PollRebaseMode)mode;
            List<String> parsedList = parseExclusionList(build.getWorkspace(), md.getExcludeList());
            out.println("[" + Config.nameShort + "] Excluding the following components: ");
            
            for(String exclude : parsedList) {
                if(!StringUtils.isBlank(exclude)) {
                    out.println(String.format("%s * %s", "[" + Config.nameShort + "]", exclude));
                }
            }
            checkExclusionList(listener, parsedList, build.getWorkspace(), action.getNewFoundationStructure());
        }
        
        return r;
    }
    
    private void checkExclusionList(BuildListener listener, List<String> exludeComponents, FilePath workspace, List<Baseline> baselines) throws AbortException {
        
        List<String> missing = new ArrayList<String>();
        for(String excl : exludeComponents) {
            boolean wasThere = false;
            for(Baseline foundabl : baselines) {
                if(foundabl.getComponent().getNormalizedName().equals(excl)) {
                    wasThere = true;
                }
            }
            
            if(!wasThere) {
                missing.add(excl);
            }
            
        }
        
        if(!missing.isEmpty()) {
            listener.getLogger().println(String.format("%s Warning: Excluded component(s) %s not found.", "[" + Config.nameShort + "]", missing));
        }
        
        for(String s : exludeComponents) {
            if(!StringUtils.isBlank(s)) {
                if(!s.startsWith("component")) {
                    try {
                        RemoteUtil.loadEntity(workspace, Component.get(s), getSlavePolling());
                    } catch (Exception ex) {
                        throw new AbortException(String.format("Unable to load component %s", s));
                    }
                }
            }
        }
        
    }

    /**
     * Initialize the deliver view
     * @param build
     * @param state
     * @param listener
     * @return 
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public SnapshotView initializeDeliverView(AbstractBuild<?, ?> build, CCUCMBuildAction state, BuildListener listener) throws IOException, InterruptedException {
        logger.fine("Initializing deliver view");
        FilePath workspace = build.getWorkspace();
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
     * @param state
     * @param listener
     * @param changelogFile
     * @param snapshotView
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public void generateChangeLog(AbstractBuild<?, ?> build, CCUCMBuildAction state, BuildListener listener, File changelogFile, SnapshotView snapshotView) throws IOException, InterruptedException {
        FilePath workspace = build.getWorkspace();
        PrintStream consoleOutput = listener.getLogger();

        logger.fine("Generating change log");
        logger.fine(String.format( "Trim changeset = %s", trimmedChangeSet ) );

        GetChanges gc = new GetChanges(listener, state.getStream(), state.getBaseline(), snapshotView.getPath());
        List<Activity> activities = workspace.act(gc);

        String changelog = Util.createChangelog(build, activities, state.getBaseline(), trimmedChangeSet, new File(snapshotView.getPath()), snapshotView.getReadOnlyLoadLines(), discard, getSlavePolling());
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
        
        try {
            CCUCMBuildAction action = build.getAction(CCUCMBuildAction.class);
            CC_BASELINE = action.getBaseline().getFullyQualifiedName();
        } catch (Exception e1) {            
            logger.log(Level.WARNING, "Exception caught in buildEnvVars method", e1);
        }

        /* View path */
        String workspace = env.get("WORKSPACE");
        if (workspace != null) {
            CC_VIEWPATH = workspace + File.separator + "view";
        } else {
            CC_VIEWPATH = "";
        }

        env.put("CC_BASELINE", CC_BASELINE);
        env.put("CC_VIEWTAG", viewtag);
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
        Component loadedComponent = null;
        try {
            stream = Stream.get(this.stream);
            if(!StringUtils.isBlank(_getComponent())) {
                loadedComponent = Component.get(_getComponent());
            } 
        } catch (UnableToInitializeEntityException e) {
            Util.println(out, e);
            throw new AbortException("Unable initialize ClearCase entities");
        }

        logger.fine("Let's go!");

        /* Check input */        
        if (checkInput(listener)) {

            printParameters(out);

            List<Baseline> baselines = null;

            Date date = null;
            if (isUseLatestAlways(mode)) {
                CCUCMBuildAction lastAction = getLastAction(project);
                if (lastAction != null) {
                    date = lastAction.getBaseline().getDate();
                }
            }

            /* Old skool self polling */
            if (_getPolling().isPollingSelf()) {
                baselines = getValidBaselinesFromStream(workspace, _getPlevel(), stream, loadedComponent, date);
            } else if(_getPolling().isPollingOther()) {
                /* Find the Baselines and store them */
                baselines = getBaselinesFromStreams(workspace, listener, out, stream, loadedComponent, _getPolling(), date);
            } else if(_getPolling().isPollingSubscribe()) {
                try {
                    //Poll self method of finding baselines
                    List<Baseline> currentBls = RemoteUtil.getRemoteBaselinesFromStream(workspace, loadedComponent, stream, _getPlevel(), this.getSlavePolling(), this.getMultisitePolling(), date);
                    logger.fine("Baseline candidates");
                    for(Baseline blz : currentBls) {
                        logger.fine(blz.getShortname());
                    }
                    //Get our candidate baseline
                    Baseline blCandidate = selectBaseline(currentBls, mode, workspace);
                    
                    if(blCandidate != null) {
                        logger.fine(String.format( "Current candidate is: %s",blCandidate.getShortname() ));                    
                    } else {
                        logger.fine("No candidate available!");
                    }
                    
                    //Get baselines on the currently selected components
                    PollSubscribeMode subMode = (PollSubscribeMode)mode;
                    List<Baseline> consideredBaselines = subMode.getBaselinesToConsider(blCandidate, workspace, getSlavePolling());                                    
                    baselines = getValidBaselinesFromStreamWithSubscribe(currentBls, consideredBaselines, workspace, getSlavePolling()).t2;
                } catch (UnableToInitializeEntityException ex) {
                    logger.log(Level.SEVERE, "Error in getValidBaselinesFromStreamWithSubscribe, unable to initialize entity", ex);
                    throw new IOException("Error in getValidBaselinesFromStreamWithSubscribe, unable to initialize entity", ex);
                } catch (CleartoolException ex) {
                    logger.log(Level.SEVERE, "Error in getValidBaselinesFromStreamWithSubscribe, cleartool exception", ex);
                    throw new IOException("Error in getValidBaselinesFromStreamWithSubscribe, cleartool exception", ex);
                }
            } else {
                PollRebaseMode md = (PollRebaseMode)mode;
                baselines = getBaselinesForPollRebase(workspace, listener, stream, parseExclusionList(workspace, md.getExcludeList())).t1;
                logger.fine("Baseline list retrieved...");
            }

            if (baselines.size() > 0) {                
                p = PollingResult.BUILD_NOW;
            }

        }
        return p;
    }
    
    private Tuple<List<Baseline>,List<Baseline>> getBaselinesForPollRebase(FilePath workspace, final TaskListener listener, final Stream stream, final List<String> excludeComponents) throws IOException, InterruptedException {
        return RemoteUtil.getRemoteRebaseCandidatesFromStream(workspace, stream, excludeComponents, _getPlevel());
    }
    
 
    /**
     * Get the {@link Baseline}s from a {@link Stream}s related Streams.
     *
     * @return A list of {@link Baseline}'s
     */
    private List<Baseline> getBaselinesFromStreams(FilePath workspace, TaskListener listener, PrintStream consoleOutput, Stream stream, Component component, Polling polling, Date date) {

        List<Stream> streams = null;
        List<Baseline> baselines = new ArrayList<Baseline>();

        try {
            streams = RemoteUtil.getRelatedStreams(workspace, listener, stream, polling, this.getSlavePolling(), this.getMultisitePolling(), this.getHLinkFeedFrom());
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
                List<Baseline> found = RemoteUtil.getRemoteBaselinesFromStream(workspace, component, s, _getPlevel(), this.getSlavePolling(), this.getMultisitePolling(), date);
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
    
    private Tuple<Result,List<Baseline>> getValidBaselinesFromStreamWithSubscribe(List<Baseline> currentBaselines, List<Baseline> considerBaselines, FilePath workspace, boolean slavePolling) throws IOException, InterruptedException {
        logger.fine("Retrieving baselines for poll subscribe");
        Tuple<Result,List<Baseline>> t = new Tuple<Result, List<Baseline>>();

        if(currentBaselines.isEmpty()) {
            t.t1 = null;
            t.t2 = currentBaselines;
            return t;
        } else {
          
            PollSubscribeMode psm = (PollSubscribeMode)mode;

            Result r = psm.determineResult(workspace, considerBaselines, slavePolling);
            t.t1 = r;
            //If the oldest baseline has not been tested yet. Wait a bit
            if(r == null) {
                t.t2 = new ArrayList<Baseline>();                    
            } else {
                t.t2 = currentBaselines;                    
            }
            return t;                
             
        }
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
        Component cmp =  StringUtils.isBlank(_getComponent()) ? null : Component.get(_getComponent());
        CCUCMBuildAction action = new CCUCMBuildAction(Stream.get(stream), cmp);

        action.setMode(mode);
        action.setDescription(setDescription);
        action.setMakeTag(makeTag);
        action.setRecommend(recommend);
        action.setForceDeliver(forceDeliver);
        action.setPromotionLevel(_getPlevel());
        action.setUnstable(treatUnstable);
        action.setLoadModule(loadModule);
        action.setRemoveViewPrivateFiles(removeViewPrivateFiles);
        action.setTrimmedChangeSet(trimmedChangeSet);

        /* Deliver and template */
        action.setCreateBaseline(_getCreateBaseline());

        /* Trim template, strip out quotes */
        if (nameTemplate.matches("^\".+\"$")) {
            nameTemplate = nameTemplate.substring(1, nameTemplate.length() - 1);
        }
        action.setNameTemplate(nameTemplate);

        action.setPolling(_getPolling());

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
    
    private boolean isUseLatestAlways(PollingMode mode) {
        if(mode instanceof NewestFeatureToggle) {
            return ((NewestFeatureToggle)mode).isNewest();
        }        
        return false;
    }
    
    private Baseline selectBaseline(List<Baseline> baselines, PollingMode mode, FilePath workspace) throws IOException, InterruptedException {        
        Baseline selected = null;       
        if (baselines.size() > 0) {
            boolean isULatest = isUseLatestAlways(mode);
            logger.fine("Skip to latest is: "+isULatest);
            if (isULatest) {
                selected = baselines.get(baselines.size() - 1);                
            } else {
                selected = baselines.get(0);
            }
            return (Baseline) RemoteUtil.loadEntity(workspace, selected, true);
        } else {
            return null;
        }
    }    

    private void printParameters(PrintStream ps) {
        ps.println("[" + Config.nameShort + "] Getting baselines for :");
        ps.println("[" + Config.nameShort + "] * Stream:          " + stream);
        if(!StringUtils.isBlank(_getComponent())) { 
            ps.println("[" + Config.nameShort + "] * Component:       " + _getComponent());
        }
        if (_getPlevel() == null) {
            ps.println("[" + Config.nameShort + "] * Promotion level: " + "ANY");
        } else {
            ps.println("[" + Config.nameShort + "] * Promotion level: " + _getPlevel());
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
    
    public String getHLinkFeedFrom() {
        CCUCMScm.CCUCMScmDescriptor desc = (CCUCMScm.CCUCMScmDescriptor) this.getDescriptor();
        return desc.gethLinkFeedFrom();
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
        return _getPolling().toString();
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
     * @return the mode
     */
    public PollingMode getMode() {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(PollingMode mode) {
        this.mode = mode;
    }

    /**
     * @param stream the stream to set
     */
    public void setStream(String stream) {
        this.stream = stream;
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

        private String hLinkFeedFrom;
        private boolean slavePolling;
        private boolean multisitePolling;
        public CCUCMScmDescriptor() {
            super(CCUCMScm.class, null);
            load();
        }

        /**
         * This method is called, when the user saves the global Hudson
         * configuration.
         * @param req
         * @param json
         * @return 
         * @throws hudson.model.Descriptor.FormException
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            try {
                String s = json.getString("slavePolling");
                if (s != null) {
                    slavePolling = Boolean.parseBoolean(s);                    
                }
                s = json.getString("multisitePolling");
                if (s != null) {
                    multisitePolling = Boolean.parseBoolean(s);
                }
                String hlink = json.getString("hLinkFeedFrom");
                if(!StringUtils.isBlank(hlink)) {
                    hLinkFeedFrom = hlink;
                } else {
                    hLinkFeedFrom = HLINK_DEFAULT;
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
         * @return The name to be displayed when the user selects the SCM
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
        
        public FormValidation doCheckStream(@QueryParameter String stream) {
            if(StringUtils.isBlank(stream)) {
                return FormValidation.error("Stream field cannot be empty");
            } else {    
                if(!stream.contains("@\\")) {
                    return FormValidation.errorWithMarkup("Streams must be defined with the correct syntax. <em>Syntax: [stream]@[vob]</em>");
                } 
            }        
            return FormValidation.ok();
        }
        
        public ListBoxModel doFillLoadModuleItems() {
            ListBoxModel model = new ListBoxModel();
            model.add("All", "ALL");
            model.add("Modifiable","MODIFIABLE");
            return model;
        }
    
        @Override
        public CCUCMScm newInstance(StaplerRequest req, JSONObject formData) {
            return req.bindJSON(CCUCMScm.class, formData);
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
         * @return the hLinkFeedFrom
         */
        public String gethLinkFeedFrom() {
            if(StringUtils.isBlank(hLinkFeedFrom)) {
                return HLINK_DEFAULT;
            }
            return hLinkFeedFrom;
        }

        /**
         * @param hLinkFeedFrom the hLinkFeedFrom to set
         */
        public void sethLinkFeedFrom(String hLinkFeedFrom) {
            this.hLinkFeedFrom = hLinkFeedFrom;
        }
    }
}
