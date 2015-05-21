/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.scm.pollingmode;

import hudson.ExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import java.io.Serializable;
import jenkins.model.Jenkins;

/**
 *
 * @author Mads
 */
public class Requirement extends AbstractDescribableImpl<Requirement> implements Serializable {
    
    public Requirement() { }
    
    public abstract static class RequirementDescriptor extends Descriptor<Requirement> {

        public static ExtensionList<RequirementDescriptor> all() {
            return Jenkins.getInstance().getExtensionList(RequirementDescriptor.class);
        }
        
        public static <T extends RequirementDescriptor> ExtensionList<T> allOfSubtype(Class<T> t) {
            return Jenkins.getInstance().getExtensionList(t);
        }
    }
}
