/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.scm.pollingmode;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Descriptor;
import java.io.Serializable;
import jenkins.model.Jenkins;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.Util;
import net.praqma.hudson.notifier.RemotePostBuild;
import net.praqma.hudson.notifier.Status;
import net.praqma.hudson.scm.Polling;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
/**
 *
 * @author Mads
 */
public class PollingMode implements Describable<PollingMode>, ExtensionPoint, Serializable  {

    protected Polling polling;
    private String component;
    private String levelToPoll = "INITIAL";
    
    public PollingMode() { } 
    
    @DataBoundConstructor
    public PollingMode(String levelToPoll) { 
        this.levelToPoll = levelToPoll;
    }
    
    @Override
    public Descriptor<PollingMode> getDescriptor() {
        return (Descriptor<PollingMode>)Jenkins.getInstance().getDescriptorOrDie(getClass());
    }
    
    public static DescriptorExtensionList<PollingMode, PollingModeDescriptor<PollingMode>> all() {
        return Jenkins.getInstance().<PollingMode,PollingModeDescriptor<PollingMode>>getDescriptorList(PollingMode.class);        
    }

    /**
     * @return the polling
     */
    public Polling getPolling() {
        return polling;
    }

    /**
     * @param polling the polling to set
     */
    public void setPolling(Polling polling) {
        this.polling = polling;
    }
    
    public boolean createBaselineEnabled() {
        if(this instanceof PollSelfMode) {
            return false;
        } else {
            return ((BaselineCreationEnabled)this).isCreateBaseline();
        }
    }

    /**
     * @return the levelToPoll
     */
    public String getLevelToPoll() {
        return levelToPoll;
    }

    public void setLevelToPoll(String levelToPoll) {
        this.levelToPoll = levelToPoll;
    }
    
    /**
     * 
     * @return A converted enum from the value entered in the UI.
     */
    public Project.PromotionLevel getPromotionLevel() {
        return Util.getLevel(levelToPoll);
    }

    /**
     * @return the component
     */
    public String getComponent() {
        return component;
    }

    /**
     * @param component the component to set
     */
    @DataBoundSetter
    public void setComponent(String component) {
        this.component = component;
    }
    
    /**
     * Marker that let us know if the given mode should skip the promotion/relegtion
     * of the source baseline. Disabled by default for poll rebase and poll self when
     * 'ANY' promotion level is selected.
     * 
     * @return a boolean indicating if promotion should be skipped
     */
    public boolean isPromotionSkipped() {
        return false;
    }
    
    /**
     * At this point we should have all the information stored in our actions to perform the necessary 
     * actions after the build has completed. The return value is always the result of the operation.
     * 
     * The result can be used to set the build status of a Jenkins job
     * @param b
     * @param listener
     * @param buildStatus
     * @return The callable to perform on the slave. 
     **/
    public FileCallable<Status> postBuildFinalizer(AbstractBuild<?,?> b, BuildListener listener, Status buildStatus){
        
        CCUCMBuildAction a = b.getAction(CCUCMBuildAction.class);
        
        //Do not promote source when polling rebase. We also allow the promotion of the selected baseline in poll subscribe mode. 
        //boolean skipPromote = (a.getPromotionLevel() == null && !a.getPolling().isPollingSubscribe()) || a.getPolling().isPollingRebase();

        Stream targetstream = a.getBaseline().getStream();
        Stream sourcestream = targetstream;
        
        Baseline sourcebaseline = a.getBaseline();
        Baseline targetbaseline = a.getCreatedBaseline() != null ? a.getCreatedBaseline() : sourcebaseline;
        
        if( getPolling().isPollingOther() || getPolling().isPollingRebase() ) {
			targetstream = a.getStream();
		}

        return new RemotePostBuild(b.getResult(), 
                buildStatus, 
                listener, 
                a.doMakeTag(), 
                a.doRecommend(),
                a.getUnstable(), 
                isPromotionSkipped(), 
                sourcebaseline, 
                targetbaseline, sourcestream, targetstream, b.getParent().getDisplayName(), Integer.toString(b.number), a.getRebaseTargets());        
    }
}
