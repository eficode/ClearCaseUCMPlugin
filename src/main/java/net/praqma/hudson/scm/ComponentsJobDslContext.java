package net.praqma.hudson.scm;

import java.util.ArrayList;
import java.util.List;
import javaposse.jobdsl.dsl.Context;
import net.praqma.hudson.scm.pollingmode.ComponentSelectionCriteriaRequirement;

class ComponentsJobDslContext implements Context{
    List<ComponentSelectionCriteriaRequirement> components = new ArrayList<ComponentSelectionCriteriaRequirement>();

    public void component(String selection){
        components.add(new ComponentSelectionCriteriaRequirement(selection));
    }
}
