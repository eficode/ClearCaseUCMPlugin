/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.scm.pollingmode;

import hudson.model.Descriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Mads
 * @param <T>
 */
public abstract class PollingModeDescriptor<T extends PollingMode> extends Descriptor<PollingMode> {

    @Override
    public PollingMode newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        return req.bindJSON(PollingMode.class, formData);
    }
    
}
