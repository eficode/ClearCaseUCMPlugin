package net.praqma.hudson.test.integration.child;

import java.io.File;

import net.praqma.hudson.test.BaseTestClass;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.TestDescription;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollChildMode;
import static net.praqma.hudson.test.BaseTestClass.jenkins;
import net.praqma.hudson.test.CCUCMRule;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BaselinesFoundFailedIT extends BaseTestClass {

    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule("ccucm");

    public AbstractBuild<?, ?> initiateBuild(String projectName, boolean recommend, boolean tag, boolean description, boolean fail) throws Exception {
        PollChildMode mode = new PollChildMode("INITIAL");
        mode.setCreateBaseline(false);
        return jenkins.initiateBuild(projectName, mode, "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), recommend, tag, description, fail, true);
    }

    @Test
    @ClearCaseUniqueVobName(name = "nop-child-failed")
    @TestDescription(title = "Child polling", text = "baseline available, build fails")
    public void testNoOptions() throws Exception {

        Baseline baseline = getNewBaseline();

        PollChildMode mode = new PollChildMode("INITIAL");
        mode.setCreateBaseline(false);
        
        CCUCMRule.ProjectCreator c = new CCUCMRule.ProjectCreator("no-options-" + ccenv.getUniqueName(), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob())
                .setForceDeliver(true)
                .setRecommend(false)
                .setMode(mode)
                .setTagged(false);
        
        FreeStyleProject fsb = c.getProject();        
        AbstractBuild<?, ?> build = jenkins.getProjectBuilder(fsb).failBuild(true).build();

        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.FAILURE)
                .validateBuiltBaseline(PromotionLevel.REJECTED, baseline, false)
                .validateCreatedBaseline(false)
                .validate();
    }

    @Test
    @ClearCaseUniqueVobName(name = "recommended-child-failed")
    @TestDescription(title = "Child polling", text = "baseline available, build fails", configurations = {"Recommend = true"})
    public void testRecommended() throws Exception {

        Baseline baseline = getNewBaseline();
        
        PollChildMode mode = new PollChildMode("INITIAL");
        mode.setCreateBaseline(true);
        
        CCUCMRule.ProjectCreator c = new CCUCMRule.ProjectCreator("recommended-" + ccenv.getUniqueName(), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob())
                .setForceDeliver(true)
                .setRecommend(true)
                .setMode(mode)
                .setTagged(false);
        
        FreeStyleProject fsb = c.getProject();        
        AbstractBuild<?, ?> build = jenkins.getProjectBuilder(fsb).failBuild(true).build();
        
        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.FAILURE)
                .validateBuiltBaseline(PromotionLevel.REJECTED, baseline, false)
                .validateCreatedBaseline(false)
                .validate();
    }

    @Test
    @ClearCaseUniqueVobName(name = "description-child-failed")
    @TestDescription(title = "Child polling", text = "baseline available, build fails", configurations = {"Set description = true"})
    public void testDescription() throws Exception {

        Baseline baseline = getNewBaseline();
        
        PollChildMode mode = new PollChildMode("INITIAL");
        mode.setCreateBaseline(false);
        
        CCUCMRule.ProjectCreator c = new CCUCMRule.ProjectCreator("description-" + ccenv.getUniqueName(), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob())
                .setForceDeliver(true)
                .setRecommend(false)
                .setMode(mode)
                .setDescribed(true)
                .setTagged(false);

        FreeStyleProject fsb = c.getProject();
        
        AbstractBuild<?, ?> build = jenkins.getProjectBuilder(fsb).failBuild(true).build();

        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.FAILURE)
                .validateBuiltBaseline(PromotionLevel.REJECTED, baseline, false)
                .validateCreatedBaseline(false)
                .validate();
    }

    @Test
    @ClearCaseUniqueVobName(name = "tagged-child-failed")
    @TestDescription(title = "Child polling", text = "baseline available, build fails", configurations = {"Set tag = true"})
    public void testTagged() throws Exception {
        jenkins.makeTagType(ccenv.getPVob());
        Baseline baseline = getNewBaseline();
        
        PollChildMode mode = new PollChildMode("INITIAL");
        mode.setCreateBaseline(false);
        
        CCUCMRule.ProjectCreator c = new CCUCMRule.ProjectCreator("no-options-" + ccenv.getUniqueName(), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob())
                .setForceDeliver(true)
                .setRecommend(false)
                .setMode(mode)
                .setTagged(true);
        
        FreeStyleProject fsb = c.getProject();
        AbstractBuild<?, ?> build = jenkins.getProjectBuilder(fsb).failBuild(true).build();
        
        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.FAILURE)
                .validateBuiltBaseline(PromotionLevel.REJECTED, baseline, false)
                .validateBaselineTag(baseline, true)
                .validateCreatedBaseline(false)
                .validate();
    }

    protected Baseline getNewBaseline() throws ClearCaseException {
        /**/
        String viewtag = ccenv.getUniqueName() + "_one_dev";
        System.out.println("VIEW: " + ccenv.context.views.get(viewtag));
        File path = new File(ccenv.context.mvfs + "/" + viewtag + "/" + ccenv.getVobName());

        System.out.println("PATH: " + path);

        Stream stream = Stream.get("one_dev", ccenv.getPVob());
        Activity activity = Activity.create("ccucm-activity", stream, ccenv.getPVob(), true, "ccucm activity", null, path);
        UCMView.setActivity(activity, path, null, null);

        try {
            ccenv.addNewElement(ccenv.context.components.get("Model"), path, "test2.txt");
        } catch (ClearCaseException e) {
            ExceptionUtils.print(e, System.out, true);
        }
        return Baseline.create("baseline-for-test", ccenv.context.components.get("_System"), path, LabelBehaviour.FULL, false);
    }
}
