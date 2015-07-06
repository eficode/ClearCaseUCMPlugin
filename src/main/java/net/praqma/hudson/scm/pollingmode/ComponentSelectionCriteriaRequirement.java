/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.scm.pollingmode;

import hudson.Extension;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.ucm.entities.Component;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author Mads
 */
public class ComponentSelectionCriteriaRequirement extends Requirement {
    
    private String componentSelection;
    
    @DataBoundConstructor
    public ComponentSelectionCriteriaRequirement(String componentSelection) {
        this.componentSelection = componentSelection;
    }

    /**
     * @return the componentSelection
     */
    public String getComponentSelection() {
        return componentSelection;
    }
    
    public Component toComponent() throws UnableToInitializeEntityException {
        return Component.get(componentSelection);
    }

    /**
     * @param componentSelection the componentSelection to set
     */
    public void setComponentSelection(String componentSelection) {
        this.componentSelection = componentSelection;
    }
    
    @Extension
    public static class ComponentSelectionCriteriaRequirementDescriptorImpl extends RequirementDescriptor {

        @Override
        public String getDisplayName() {
            return "ClearCase UCM Component";
        }
        
        public ComponentSelectionCriteriaRequirementDescriptorImpl() {
            load();
        }
        
    }
}
