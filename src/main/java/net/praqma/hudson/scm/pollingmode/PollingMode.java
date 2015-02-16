/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.scm.pollingmode;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.hudson.Util;
import net.praqma.hudson.scm.Polling;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Mads
 */
public class PollingMode implements Describable<PollingMode>, ExtensionPoint  {

    protected Polling polling;
    private String levelToPoll = "INITIAL";
    
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
}
