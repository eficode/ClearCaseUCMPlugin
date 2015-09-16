package net.praqma.hudson.test.integration.child;

import net.praqma.hudson.test.BaseTestClass;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollChildMode;
import net.praqma.hudson.test.CCUCMRule;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BaselinesFoundIT extends BaseTestClass {

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
        Baseline baseline = ccenv.createNewDevStreamContents("one_dev");
        AbstractBuild<?, ?> build = initiateBuild("no-options-" + ccenv.getUniqueName(), false, false, false, false);
        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.SUCCESS)
                .validateBuildView()
                .validateBuiltBaseline(PromotionLevel.BUILT, baseline, false)
                .validateCreatedBaseline(true)
                .validate();
    }

    @Test
    @ClearCaseUniqueVobName(name = "rec-child")
    @TestDescription(title = "Child polling", text = "baseline available", configurations = {"Recommended = true"})
    public void testRecommended() throws Exception {
        Baseline baseline = ccenv.createNewDevStreamContents("one_dev");

        AbstractBuild<?, ?> build = initiateBuild("rec-" + ccenv.getUniqueName(), true, false, false, false);
        
        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.SUCCESS)
                .validateBuildView()
                .validateBuiltBaseline(PromotionLevel.BUILT, baseline, false)
                .validateCreatedBaseline(true, true)
                .validate();
    }

    @Test
    @ClearCaseUniqueVobName(name = "description-child")
    @TestDescription(title = "Child polling", text = "baseline available", configurations = {"Set description = true"})
    public void testDescription() throws Exception {
        Baseline baseline = ccenv.createNewDevStreamContents("one_dev");

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
        jenkins.makeTagType(ccenv.getPVob());
        Baseline baseline = ccenv.createNewDevStreamContents("one_dev");

        AbstractBuild<?, ?> build = initiateBuild("tagged-" + ccenv.getUniqueName(), false, true, false, false);

        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.SUCCESS)
                .validateBuildView()
                .validateBuiltBaseline(PromotionLevel.BUILT, baseline, false)
                .validateBaselineTag(baseline, true)
                .validateCreatedBaseline(true)
                .validate();
    }
    
    @Test
    @ClearCaseUniqueVobName(name = "newest-child")
    @TestDescription(title = "Child polling", text = "baseline available", configurations = {"Use newest = true"})
    public void testUseNewest() throws Exception {        
        //Create a new baseline
        Baseline bl1 = ccenv.createNewDevStreamContents("one_dev").load();
        
        //Create another 
        Baseline bl2 = ccenv.createNewDevStreamContents("one_dev").load();
        
        PollChildMode modeChild = new PollChildMode("INITIAL");
        modeChild.setNewest(true);
        modeChild.setCreateBaseline(true);

        CCUCMRule.ProjectCreator creator = new CCUCMRule.ProjectCreator("newest-" + ccenv.getUniqueName(), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob())
        .setMode(modeChild);
        
        FreeStyleProject p = creator.getProject();         
        AbstractBuild<?,?> buildNewest = jenkins.getProjectBuilder(p).build();
        
        //First build should be succes
        SystemValidator validator = new SystemValidator(buildNewest)
                .validateBuild(Result.SUCCESS)
                .validateBuiltBaseline(PromotionLevel.BUILT, bl2)
                .validateCreatedBaseline(true);
        validator.validate();
        
        //Next build. Nothing to do
        AbstractBuild<?,?> nothing = jenkins.getProjectBuilder(p).build();
        SystemValidator validator2 = new SystemValidator(nothing)
                .validateBuild(Result.NOT_BUILT);
        validator2.validate();
    }
}
