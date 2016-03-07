package net.praqma.hudson.scm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javaposse.jobdsl.dsl.Context;
import static javaposse.jobdsl.dsl.Preconditions.checkArgument;
import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;
import net.praqma.hudson.scm.pollingmode.ComponentSelectionCriteriaRequirement;
import net.praqma.hudson.scm.pollingmode.JobNameRequirement;

class PollingModeJobDslContext implements Context {

    PollingModeJobDslContext(String promotionLevel) {
        this.promotionLevel = promotionLevel;
    }

    //Used by: All
    Set<String> promotionLevels = new HashSet<String>() {
        {
            add("ANY");
            add("INITIAL");
            add("BUILT");
            add("TESTED");
            add("RELEASED");
            add("REJECTED");
        }
    };
    String promotionLevel;

    public void promotionLevel(String value) {
        promotionLevel = promotionLevel.toUpperCase();
        checkArgument(promotionLevels.contains(promotionLevel), "promotionLevel must be one of: " + promotionLevels.toString());
        promotionLevel = value;
    }

    //Used by: SUBSCRIBE
    List<ComponentSelectionCriteriaRequirement> components = new ArrayList<ComponentSelectionCriteriaRequirement>();

    public void components(Runnable closure) {
        ComponentsJobDslContext context = new ComponentsJobDslContext();
        executeInContext(closure, context);

        components = context.components;
    }

    //Used by: SUBSCRIBE
    List<JobNameRequirement> jobs = new ArrayList<JobNameRequirement>();

    public void jobs(Runnable closure) {
        JobsJobDslContext context = new JobsJobDslContext();
        executeInContext(closure, context);

        jobs = context.jobs;
    }

     // Used by: CHILD, REBASE, SIBLING
    boolean createBaseline = false;

    public void createBaseline() {
        createBaseline = true;
    }

    public void createBaseline(boolean value) {
        createBaseline = value;
    }

    //Used by: SIBLING
    boolean hyperlinkPolling = false;

    public void hyperlinkPolling() {
        hyperlinkPolling = true;
    }

    public void hyperlinkPolling(boolean value) {
        hyperlinkPolling = value;
    }

    //Used by: SUBSCRIBE
    boolean cascadePromotion = false;

    public void cascadePromotion() {
        cascadePromotion = true;
    }

    public void cascadePromotion(boolean value) {
        cascadePromotion = value;
    }

    //Used by: SUBSCRIBE
    boolean useNewest = false;

    public void useNewest() {
        useNewest = true;
    }

    public void useNewest(boolean value) {
        useNewest = value;
    }

    //Used by: REBASE
    String excludeList = "";
    
    public void excludeList(String value){
        excludeList = value;
    }
}
