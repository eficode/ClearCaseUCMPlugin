/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.scm.pollingmode;

import hudson.Extension;
import net.praqma.hudson.scm.Polling;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Mads
 */
public class PollChildMode extends PollingMode implements BaselineCreationEnabled{
    
    private boolean createBaseline;
    
    @DataBoundConstructor
    public PollChildMode(boolean createBaseline) {
        polling = new Polling(Polling.PollingType.childs);
        this.createBaseline = createBaseline;
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
    public void setCreateBaseline(boolean createBaseline) {
        this.createBaseline = createBaseline;
    }
    
    @Extension
    public static final class PollChildDescriptor extends PollingModeDescriptor<PollingMode> {

        public PollChildDescriptor() { }
                
        @Override
        public String getDisplayName() {
            return "Poll child";
        }
        
    }
}
