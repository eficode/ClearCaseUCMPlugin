/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.test.integration.child;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.hudson.scm.pollingmode.PollChildMode;
import net.praqma.hudson.test.BaseTestClass;
import static net.praqma.hudson.test.BaseTestClass.jenkins;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Mads
 * 
 * Split this one out in new class.
 * 
 */
public class BaselinesFoundRecommend extends BaseTestClass {
    
    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule("ccucm");
    
    @Rule
    public DescriptionRule desc = new DescriptionRule();    

    @Test
    @ClearCaseUniqueVobName(name = "rec-child")
    @TestDescription(title = "Child polling", text = "baseline available", configurations = {"Recommended = true"})
    public void testRecommended() throws Exception {
        Baseline baseline = ccenv.createNewDevStreamContents("one_dev");

        AbstractBuild<?, ?> build = initiateBuild("rec-" + ccenv.getUniqueName(), true, false, false, false);
        
        SystemValidator validator = new SystemValidator(build)
                .validateBuild(Result.SUCCESS)
                .validateBuildView()
                .validateBuiltBaseline(Project.PromotionLevel.BUILT, baseline, false)
                .validateCreatedBaseline(true, true)
                .validate();
    }
    
    public AbstractBuild<?, ?> initiateBuild(String projectName, boolean recommend, boolean tag, boolean description, boolean fail) throws Exception {
        PollChildMode mode = new PollChildMode("INITIAL");
        mode.setCreateBaseline(true);
        return jenkins.initiateBuild(projectName, mode, "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), recommend, tag, description, fail, true);
    }
    
}
