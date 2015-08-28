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
 * @author Mads
 */
public class PollChildMode extends PollingMode implements BaselineCreationEnabled, NewestFeatureToggle {
    
    private boolean createBaseline = false;
    private boolean newest = false;
    
    @DataBoundConstructor
    public PollChildMode(String levelToPoll) {
        super(levelToPoll);
        polling = new Polling(Polling.PollingType.childs);        
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
    
    public boolean isNewest() {
        return newest;
    }
    
    @Extension
    public static final class PollChildDescriptor extends PollingModeDescriptor<PollingMode> {

        public PollChildDescriptor() { }
                
        @Override
        public String getDisplayName() {
            return "Poll child";
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
