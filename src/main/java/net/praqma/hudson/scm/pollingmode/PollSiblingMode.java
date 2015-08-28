/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.scm.pollingmode;

import hudson.Extension;
import hudson.util.ListBoxModel;
import net.praqma.hudson.Config;
import net.praqma.hudson.scm.Polling;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author Mads
 */
public class PollSiblingMode extends PollingMode implements BaselineCreationEnabled, NewestFeatureToggle {
    
    private boolean useHyperLinkForPolling = false;
    private boolean createBaseline = false;
    private boolean newest = false;
    
    
    @DataBoundConstructor
    public PollSiblingMode(String levelToPoll) {
        super(levelToPoll);
    }

    /**
     * @return the useHyperLinkForPolling
     */
    public boolean isUseHyperLinkForPolling() {
        return useHyperLinkForPolling;
    }

    /**
     * @param useHyperLinkForPolling the useHyperLinkForPolling to set
     */
    @DataBoundSetter
    public void setUseHyperLinkForPolling(boolean useHyperLinkForPolling) {
        this.useHyperLinkForPolling = useHyperLinkForPolling;
        if(this.useHyperLinkForPolling) {
            polling = new Polling(Polling.PollingType.siblingshlink);
        } else {
            polling = new Polling(Polling.PollingType.siblings);
        }
    }

    /**
     * @return the createBaseline
     */
    @Override
    public boolean isCreateBaseline() {
        return createBaseline;
    }

    /**
     * @param createBaseline the createBaseline to set
     */
    @DataBoundSetter
    public void setCreateBaseline(boolean createBaseline) {
        this.createBaseline = createBaseline;
    }
    
    @DataBoundSetter
    public void setNewest(boolean newest) {
        this.newest = newest;
    }
    
    @Override
    public boolean isNewest() {
        return newest;
    }
    
    @Extension    
    public static class PollSiblingDescriptor extends PollingModeDescriptor<PollingMode> {

        public PollSiblingDescriptor() { }
        
        @Override
        public String getDisplayName() {
            return "Poll sibling";
        }
        
        public ListBoxModel doFillLevelToPollItems() {
            ListBoxModel model = new ListBoxModel();
            for(String s : Config.getLevels()) {
                model.add(s);
            }
            return model;
        }
        
    }
}
