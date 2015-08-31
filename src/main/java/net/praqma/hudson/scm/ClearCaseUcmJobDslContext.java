package net.praqma.hudson.scm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javaposse.jobdsl.dsl.Context;
import static javaposse.jobdsl.dsl.Preconditions.checkArgument;
import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;
import net.praqma.hudson.scm.pollingmode.PollChildMode;
import net.praqma.hudson.scm.pollingmode.PollRebaseMode;
import net.praqma.hudson.scm.pollingmode.PollSelfMode;
import net.praqma.hudson.scm.pollingmode.PollSiblingMode;
import net.praqma.hudson.scm.pollingmode.PollSubscribeMode;
import net.praqma.hudson.scm.pollingmode.PollingMode;

public class ClearCaseUcmJobDslContext implements Context {

    String loadModules = "All";
    Map<String, String> loadModulesOptions = new HashMap<String, String>() {
        {
            put("ALL", "All");
            put("MODIFIABLE", "Modifiable");
        }
    };

    public void loadModules(String value) {
        value = value.toUpperCase();
        checkArgument(loadModulesOptions.containsKey(value), "loadModules must be one of: " + loadModulesOptions.keySet().toString());
        loadModules = loadModulesOptions.get(value);
    }

    boolean ignoreUnmodifiableChanges = false;

    public void ignoreUnmodifiableChanges() {
        ignoreUnmodifiableChanges = true;
    }

    public void ignoreUnmodifiableChanges(boolean value) {
        ignoreUnmodifiableChanges = value;
    }

    boolean trimmedChangeset = false;

    public void trimmedChangeset() {
        trimmedChangeset = true;
    }

    public void trimmedChangeset(boolean value) {
        trimmedChangeset = value;
    }

    boolean removeViewPrivateFiles = true;

    public void removeViewPrivateFiles() {
        removeViewPrivateFiles = true;
    }

    public void removeViewPrivateFiles(boolean value) {
        removeViewPrivateFiles = value;
    }

    String buildProject = null;

    public void buildProject(String value) {
        buildProject = value;
    }

    boolean setDescription = true;

    public void setDescription() {
        setDescription = true;
    }

    public void setDescription(boolean value) {
        setDescription = value;
    }

    boolean makeTag = false;

    public void makeTag() {
        makeTag = true;
    }

    public void makeTag(boolean value) {
        makeTag = value;
    }

    boolean recommendBaseline = false;

    public void recommendBaseline() {
        recommendBaseline = true;
    }

    public void recommendBaseline(boolean value) {
        recommendBaseline = value;
    }

    boolean forceDeliver = false;

    public void forceDeliver() {
        forceDeliver = true;
    }

    public void forceDeliver(boolean value) {
        forceDeliver = value;
    }

    String nameTemplate = "[project]_[date]_[time]";

    public void nameTemplate(String value) {
        nameTemplate = value;
    }

    String treatUnstableAsSuccessful = "successful";

    public void treatUnstableAsSuccessful() {
        treatUnstableAsSuccessful = "successful";
    }

    public void treatUnstableAsSuccessful(boolean value) {
        if (value) {
            treatUnstableAsSuccessful = "successful";
        } else {
            treatUnstableAsSuccessful = "failed";
        }
    }

    PollingMode pollingMode = new PollChildMode("INITIAL");
    Set<String> pollingModes = new HashSet<String>() {
        {
            add("CHILD");
            add("REBASE");
            add("SELF");
            add("SIBLING");
            add("SUBSCRIBE");
        }
    };
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

    public void pollingMode(String mode, String component, Runnable closure) {
        mode = mode.toUpperCase();
        checkArgument(pollingModes.contains(mode), "pollingMode must be one of: " + pollingModes.toString());

        String promotionLevel;
        if (mode.equals("SELF")) {
            promotionLevel = "ANY";
        } else {
            promotionLevel = "INITIAL";
        }

        pollingMode(mode, component, promotionLevel, closure);
    }

    public void pollingMode(String mode, String component, String promotionLevel, Runnable closure) {
        mode = mode.toUpperCase();
        checkArgument(pollingModes.contains(mode), "pollingMode must be one of: " + pollingModes.toString());

        promotionLevel = promotionLevel.toUpperCase();
        checkArgument(promotionLevels.contains(promotionLevel), "promotionLevel must be one of: " + promotionLevels.toString());

        PollingModeJobDslContext context = new PollingModeJobDslContext(promotionLevel);
        executeInContext(closure, context);

        if (mode.equals("CHILD")) {
            PollChildMode pollChildMode = new PollChildMode(context.promotionLevel);
            pollChildMode.setComponent(component);
            pollChildMode.setCreateBaseline(context.createBaseline);
            pollChildMode.setNewest(context.useNewest);
            pollingMode = pollChildMode;
        } else if (mode.equals("REBASE")) {
            PollRebaseMode pollRebaseMode = new PollRebaseMode(context.promotionLevel);
            pollRebaseMode.setComponent(component);
            pollRebaseMode.setCreateBaseline(context.createBaseline);
            pollRebaseMode.setExcludeList(context.excludeList);
            pollingMode = pollRebaseMode;
        } else if (mode.equals("SELF")) {
            PollSelfMode pollSelfMode = new PollSelfMode(context.promotionLevel);
            pollSelfMode.setComponent(component);
            pollSelfMode.setNewest(context.useNewest);
            pollingMode = pollSelfMode;
        } else if (mode.equals("SIBLING")) {
            PollSiblingMode pollSiblingMode = new PollSiblingMode(context.promotionLevel);
            pollSiblingMode.setComponent(component);
            pollSiblingMode.setCreateBaseline(context.createBaseline);
            pollSiblingMode.setUseHyperLinkForPolling(context.hyperlinkPolling);
            pollSiblingMode.setNewest(context.useNewest);
            pollingMode = pollSiblingMode;
        } else if (mode.equals("SUBSCRIBE")) {
            PollSubscribeMode pollSubscribeMode = new PollSubscribeMode(context.promotionLevel, context.components, context.jobs);
            pollSubscribeMode.setComponent(component);
            pollSubscribeMode.setCascadePromotion(context.cascadePromotion);
            pollSubscribeMode.setNewest(context.useNewest);
            pollingMode = pollSubscribeMode;
        }
    }
}
