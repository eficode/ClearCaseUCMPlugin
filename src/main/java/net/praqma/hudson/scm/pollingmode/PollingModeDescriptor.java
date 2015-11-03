/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.scm.pollingmode;

import hudson.model.Descriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author Mads
 * @param <T> The {@link Descriptor}'s base class
 */
public abstract class PollingModeDescriptor<T extends PollingMode> extends Descriptor<PollingMode> {

    @Override
    public PollingMode newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        return req.bindJSON(PollingMode.class, formData);
    }
    
    public FormValidation doCheckComponent(@QueryParameter String component) {
        if(StringUtils.isBlank(component)) {
            return FormValidation.error("Component field cannot be empty");
        } else {    
            if(!component.contains("@\\")) {
                return FormValidation.errorWithMarkup("Components must be entered with the correct syntax. <em>Syntax: [component]@[PVOB]</em>");
            } 
        }        
        return FormValidation.ok();
    }       
}
