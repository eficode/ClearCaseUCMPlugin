package net.praqma.hudson.test.integration.child;

import java.io.File;

import net.praqma.hudson.test.BaseTestClass;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
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
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollChildMode;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BaselinesFound extends BaseTestClass {

    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule("ccucm");
    @Rule
    public DescriptionRule desc = new DescriptionRule();

    public AbstractBuild<?, ?> initiateBuild(String projectName, boolean recommend, boolean tag, boolean description, boolean fail) throws Exception {
        PollChildMode mode = new PollChildMode("INITIAL");
        mode.setCreateBaseline(true);
        return jenkins.initiateBuild(projectName, mode, "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), recommend, tag, description, fail, true);
    }

    @Test
    @ClearCaseUniqueVobName(name = "nop-child")
    @TestDescription(title = "Child polling", text = "baseline available")
    public void testNoOptions() throws Exception {
        Baseline baseline = getNewBaseline();
        AbstractBuild<?, ?> build = initiateBuild("no-options-" + ccenv.getUniqueName(), false, false, false, false);
        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.SUCCESS)
                .validateBuildView()
                .validateBuiltBaseline(PromotionLevel.BUILT, baseline, false)
                .validateCreatedBaseline(true)
                .validate();
    }

    @Test
    @ClearCaseUniqueVobName(name = "recommended-child")
    @TestDescription(title = "Child polling", text = "baseline available", configurations = {"Recommended = true"})
    public void testRecommended() throws Exception {
        System.out.println("2");
        Baseline baseline = getNewBaseline();

        AbstractBuild<?, ?> build = initiateBuild("recommended-" + ccenv.getUniqueName(), true, false, false, false);
        
        //Question: Why do we not validate that the BUILT baseline in promoted?? 
        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.SUCCESS)
                .validateBuildView()
                .validateBuiltBaseline(PromotionLevel.BUILT, baseline, false)
                .validateCreatedBaseline(true)
                .validate();
    }

    @Test
    @ClearCaseUniqueVobName(name = "description-child")
    @TestDescription(title = "Child polling", text = "baseline available", configurations = {"Set description = true"})
    public void testDescription() throws Exception {
        System.out.println("3");
        Baseline baseline = getNewBaseline();

        AbstractBuild<?, ?> build = initiateBuild("description-" + ccenv.getUniqueName(), false, false, true, false);

        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.SUCCESS)
                .validateBuildView()
                .validateBuiltBaseline(PromotionLevel.BUILT, baseline, false)
                .validateCreatedBaseline(true)
                .validate();
    }

    @Test
    @ClearCaseUniqueVobName(name = "tagged-child")
    @TestDescription(title = "Child polling", text = "baseline available", configurations = {"Set tag = true"})
    public void testTagged() throws Exception {
        System.out.println("4");
        jenkins.makeTagType(ccenv.getPVob());
        Baseline baseline = getNewBaseline();

        AbstractBuild<?, ?> build = initiateBuild("tagged-" + ccenv.getUniqueName(), false, true, false, false);

        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.SUCCESS)
                .validateBuildView()
                .validateBuiltBaseline(PromotionLevel.BUILT, baseline, false)
                .validateBaselineTag(baseline, true)
                .validateCreatedBaseline(true)
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
