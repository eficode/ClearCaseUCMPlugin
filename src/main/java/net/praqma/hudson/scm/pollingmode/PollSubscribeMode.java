/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.scm.pollingmode;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jenkins.model.GlobalConfiguration;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.Config;
import net.praqma.hudson.PromotionListAction;
import net.praqma.hudson.notifier.PollSubscribeRemotePostBuild;
import net.praqma.hudson.notifier.Status;
import net.praqma.hudson.remoting.GetConsideredBaselinesForSubscribe;
import net.praqma.hudson.remoting.GetDetermineResultForSubscribe;
import net.praqma.hudson.scm.Polling;
import net.praqma.hudson.scm.pollingmode.ComponentSelectionCriteriaRequirement.ComponentSelectionCriteriaRequirementDescriptorImpl;
import net.praqma.hudson.scm.pollingmode.JobNameRequirement.JobNameRequirementDescriptorImpl;
import net.praqma.hudson.scm.pollingmode.Requirement.RequirementDescriptor;
import org.jenkinsci.plugins.compatibilityaction.CompatibilityDataPlugin;
import org.jenkinsci.plugins.compatibilityaction.CompatibilityDataProvider;
import org.jenkinsci.plugins.compatibilityaction.MongoProviderImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author Mads
 */
public class PollSubscribeMode extends PollingMode implements BaselineCreationEnabled, NewestFeatureToggle {
    
    private List<ComponentSelectionCriteriaRequirement> componentsToMonitor = new ArrayList<>();
    private List<JobNameRequirement> jobsToMonitor = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(PollSubscribeMode.class.getName());
    private boolean newest = false;
    private boolean cascadePromotion = true;
 
    @DataBoundConstructor
    public PollSubscribeMode(String levelToPoll, List<ComponentSelectionCriteriaRequirement> componentsToMonitor, List<JobNameRequirement> jobsToMonitor) {
        super(levelToPoll);
        this.polling = new Polling(Polling.PollingType.subscribe);
        this.componentsToMonitor = componentsToMonitor;
        this.jobsToMonitor = jobsToMonitor;
    }
    
    @DataBoundSetter
    public void setNewest(boolean newest) {
        this.newest = newest;
    }
    
    @Override
    public boolean isNewest() {
        return newest;
    }
    
    @DataBoundSetter
    public void setCascadePromotion(boolean cascadePromotion) {
        this.cascadePromotion = cascadePromotion;
    }
    
    public boolean isCascadePromotion() {
        return cascadePromotion;
    }
    /**
     * @return the componentsToMonitor
     */
    public List<ComponentSelectionCriteriaRequirement> getComponentsToMonitor() {
        return componentsToMonitor;
    }

    /**
     * @param componentsToMonitor the componentsToMonitor to set
     */
    public void setComponentsToMonitor(List<ComponentSelectionCriteriaRequirement> componentsToMonitor) {
        this.componentsToMonitor = componentsToMonitor;
    }

    /**
     * @return the jobsToMonitor
     */
    public List<JobNameRequirement> getJobsToMonitor() {
        return jobsToMonitor;
    }

    /**
     * @param jobsToMonitor the jobsToMonitor to set
     */
    public void setJobsToMonitor(List<JobNameRequirement> jobsToMonitor) {
        this.jobsToMonitor = jobsToMonitor;
    }

    /**
     * Determine the result of this subscribe action that triggered the build. We need to take action
     * on incompatible configurations.
     * @param workspace Workspace
     * @param bls Baseline
     * @param slavePolling Slave polling enabled. (Allow polling on slaves)
     * @return The desired build status. Null if one of the baselines specified was not found in all the required jobs. 
     * @throws java.io.IOException Generic system error
     * @throws java.lang.InterruptedException Generic system error 
     */
    public Result determineResult(FilePath workspace, List<Baseline> bls, boolean slavePolling) throws IOException, InterruptedException {        
        CompatibilityDataProvider pr = GlobalConfiguration.all().get(CompatibilityDataPlugin.class).getProvider(MongoProviderImpl.class);
        if(slavePolling) {
            return workspace.act(new GetDetermineResultForSubscribe(jobsToMonitor, bls, pr));
        }
        return new GetDetermineResultForSubscribe(jobsToMonitor, bls, pr).invoke(null, null);
    }
    
    /**
     * First filter. We take out the composite baselines of this baseline we might not be interested in. This needs to be filtered 
     * further.
     * @param bl Baseline
     * @param workspace Workspace
     * @param slavePolling Allow polling on slaves
     * @return A list of {@link Baseline}s under the chosen components
     * @throws UnableToInitializeEntityException Cleartool error
     * @throws CleartoolException Cleartool error
     * @throws java.io.IOException System errors 
     * @throws java.lang.InterruptedException System errors  
     */
    public List<Baseline> getBaselinesToConsider(final Baseline bl, FilePath workspace, boolean slavePolling) throws UnableToInitializeEntityException, CleartoolException, IOException, InterruptedException {        
        logger.fine("Preparing to list baselines on remote for poll subscribe");
        List<Baseline> blzzz = new ArrayList<>();
        if(slavePolling) {
            blzzz = workspace.act(new GetConsideredBaselinesForSubscribe(bl, componentsToMonitor));            
        } else {
            blzzz =  new GetConsideredBaselinesForSubscribe(bl, componentsToMonitor).invoke(null, null);            
        }
        return blzzz;
    }

    @Override
    public boolean isCreateBaseline() {
        return false;
    }
    
    @Extension
    public static final class PollSubscribeDescriptor extends PollingModeDescriptor<PollingMode> {

        @Override
        public String getDisplayName() {
            return "Poll subscribe";
        }
        
        public ListBoxModel doFillLevelToPollItems() {
            ListBoxModel model = new ListBoxModel();            
            model.add("ANY");
            for(String s : Config.getLevels()) {
                model.add(s);
            }            
            return model;
        }
        
        public ExtensionList<JobNameRequirementDescriptorImpl> getAllRequirements() {
            return RequirementDescriptor.allOfSubtype(JobNameRequirementDescriptorImpl.class);
        }
        
        public ExtensionList<ComponentSelectionCriteriaRequirementDescriptorImpl> getCListReqs() {
            return RequirementDescriptor.allOfSubtype(ComponentSelectionCriteriaRequirementDescriptorImpl.class);
        }

    }

    @Override
    public FilePath.FileCallable<Status> postBuildFinalizer(AbstractBuild<?, ?> b, BuildListener listener, Status buildStatus) {
        
        CCUCMBuildAction a = b.getAction(CCUCMBuildAction.class);
        PromotionListAction plist = b.getAction(PromotionListAction.class);
        
        //Do not promote source when polling rebase. We also allow the promotion of the selected baseline in poll subscribe mode. 
        //boolean skipPromote = (a.getPromotionLevel() == null && !a.getPolling().isPollingSubscribe()) || a.getPolling().isPollingRebase();

        Stream targetstream = a.getBaseline().getStream();
        Stream sourcestream = targetstream;
        
        Baseline sourcebaseline = a.getBaseline();
        Baseline targetbaseline = a.getCreatedBaseline() != null ? a.getCreatedBaseline() : sourcebaseline;
        
        if( getPolling().isPollingOther() || getPolling().isPollingRebase() ) {
			targetstream = a.getStream();
		}
        
        PollSubscribeRemotePostBuild pob = new PollSubscribeRemotePostBuild(b.getResult(), 
            buildStatus, 
            listener, 
            sourcebaseline, 
            targetbaseline, sourcestream, targetstream, b.getParent().getDisplayName(), Integer.toString(b.number), plist.baselines).
       
        setDoMakeTag(a.doMakeTag()).
        setDoRecommend(a.doRecommend()).
        setMode(this).
        setSkipPromotion(isPromotionSkipped()).
        setUnstable(a.getUnstable());
        
        return pob;
    }
    
    
    
}
