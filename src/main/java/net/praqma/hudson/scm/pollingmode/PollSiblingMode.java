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
public class PollSiblingMode extends PollingMode implements BaselineCreationEnabled {
    
    private boolean useHyperLinkForPolling;
    private boolean createBaseline;
    
    @DataBoundConstructor
    public PollSiblingMode(boolean useHyperLinkForPolling, boolean createBaseline) {
        if(useHyperLinkForPolling) {
            polling = new Polling(Polling.PollingType.siblingshlink);
        } else {
            polling = new Polling(Polling.PollingType.siblings);
        }
        this.useHyperLinkForPolling = useHyperLinkForPolling;
        this.createBaseline = createBaseline;
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
    public void setUseHyperLinkForPolling(boolean useHyperLinkForPolling) {
        this.useHyperLinkForPolling = useHyperLinkForPolling;
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
    public static class PollSiblingDescriptor extends PollingModeDescriptor<PollingMode> {

        public PollSiblingDescriptor() { }
        
        @Override
        public String getDisplayName() {
            return "Poll sibling";
        }
        
    }
}
