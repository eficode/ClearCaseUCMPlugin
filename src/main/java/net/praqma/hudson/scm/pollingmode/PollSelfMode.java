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
public class PollSelfMode extends PollingMode {
    
    @DataBoundConstructor
    public PollSelfMode() {
        polling = new Polling(Polling.PollingType.self);
    }
    
    @Extension
    public static final class PollSelfDescriptor extends PollingModeDescriptor<PollingMode> {

        public PollSelfDescriptor() {}
        
        @Override
        public String getDisplayName() {
            return "Poll self";
        }
    
    }
}
