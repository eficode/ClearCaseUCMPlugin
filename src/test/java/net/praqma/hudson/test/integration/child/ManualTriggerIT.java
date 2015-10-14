/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.test.integration.child;

import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.hudson.scm.pollingmode.PollChildMode;
import net.praqma.hudson.test.BaseTestClass;
import static net.praqma.hudson.test.BaseTestClass.jenkins;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author Mads
 */
public class ManualTriggerIT extends BaseTestClass {
    
    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule("ccucm");
    
    @Rule
    public DescriptionRule desc = new DescriptionRule();
    
    @Test
    @ClearCaseUniqueVobName(name = "manual-trigger")
    @TestDescription(title = "Child polling, exhaust baseline build manually last build", text = "baseline available", configurations = {"Use newest = true"})
    public void testManual() throws Exception {        
        //Create a new baseline
        Baseline bl1 = ccenv.createNewDevStreamContents("one_dev").load();
        
        //Create another 
        Baseline bl2 = ccenv.createNewDevStreamContents("one_dev").load();
        
        PollChildMode modeChild = new PollChildMode("INITIAL");
        modeChild.setNewest(true);
        modeChild.setCreateBaseline(true);

        CCUCMRule.ProjectCreator creator = new CCUCMRule.ProjectCreator("manual-" + ccenv.getUniqueName(), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob())
        .setMode(modeChild);
        
        
        FreeStyleProject p = creator.getProject();         
        AbstractBuild<?,?> buildNewest = jenkins.getProjectBuilder(p).failBuild(true).build();
        
        //First build we fail. That means no created baseline on source
        SystemValidator validator = new SystemValidator(buildNewest)
                .validateBuild(Result.FAILURE)
                .validateBuiltBaseline(Project.PromotionLevel.REJECTED, bl2);
        validator.validate();
        
        //Reset promotion level
        bl2.setPromotionLevel(Project.PromotionLevel.INITIAL);
        
        //Next build. Since we trigger manually. The project should pick up the previous baseline.
        AbstractBuild<?,?> nothing = jenkins.getProjectBuilder(p).build(new Cause.UserIdCause());
        SystemValidator validator2 = new SystemValidator(nothing)
                .validateBuiltBaseline(Project.PromotionLevel.BUILT, bl2)
                .validateBuild(Result.SUCCESS);
        validator2.validate();
    }    
}
