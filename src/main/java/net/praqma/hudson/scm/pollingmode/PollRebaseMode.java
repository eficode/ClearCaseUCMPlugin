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
public class PollRebaseMode extends PollingMode implements BaselineCreationEnabled {
    
    private String excludeList = "";
    private boolean createBaseline = false;
    
    @DataBoundConstructor
    public PollRebaseMode(String levelToPoll) {
        super(levelToPoll);
        polling = new Polling(Polling.PollingType.rebase);
    }

    /**
     * @return the excludeList
     */
    public String getExcludeList() {
        return excludeList;
    }

    /**
     * @param excludeList the excludeList to set
     */
    @DataBoundSetter
    public void setExcludeList(String excludeList) {
        this.excludeList = excludeList;
    }
    
    @DataBoundSetter
    public void setCreateBaseline(boolean createBaseline) {
        this.createBaseline = createBaseline;
    }

    @Override
    public boolean isCreateBaseline() {
        return createBaseline;
    }
    
    @Extension
    public static final class PollRebaseModeDescriptor extends PollingModeDescriptor<PollingMode> {

        public PollRebaseModeDescriptor() { }
        
        @Override
        public String getDisplayName() {
            return "Poll rebase";
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
