package net.praqma.hudson.test.integration.userstories;

import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.model.Result;
import hudson.triggers.SCMTrigger;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollSelfMode;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;


/**
 * @author cwolfgang
 *         Date: 04-02-13
 *         Time: 12:00
 */
public class JENKINS16636IT extends BaseTestClass {

    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-16636", "setup-JENKINS-16620.xml" );

    @Rule
    public DescriptionRule desc = new DescriptionRule();

    @Test
    @TestDescription( title = "JENKINS-16636", text = "No new baseline found, but can be build anyway" )
    @ClearCaseUniqueVobName( name = "NORMAL" )
    public void jenkins16636() throws Exception {
        PollSelfMode mode = new PollSelfMode("INITIAL");
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-16636", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() )
                .setMode(mode)
                .getProject();

        /* First build must be a success, because there is a valid baseline.
         * This build is done because we need a previous action object */
        AbstractBuild build1 = jenkins.getProjectBuilder( project ).build(new SCMTrigger.SCMTriggerCause("Trigger change for SCM test"));
        new SystemValidator( build1 ).validateBuild( Result.SUCCESS ).validate();

        /* Because there are no new baselines, the build must fail */
        AbstractBuild build2 = jenkins.getProjectBuilder( project ).build(new SCMTrigger.SCMTriggerCause("Trigger change for SCM test"));
        new SystemValidator( build2 ).validateBuild( Result.NOT_BUILT ).validate();
    }

    @Test
    @TestDescription( title = "JENKINS-16636", text = "No new baseline found, but can be build anyway. Correct behaviour for ANY" )
    @ClearCaseUniqueVobName( name = "ANY" )
    public void jenkins16636Any() throws Exception {
        PollSelfMode mode = new PollSelfMode("ANY");
        
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-16636-any", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() )
                .setMode(mode)
                .getProject();

        /* First build must be a success, because there is a valid baseline.
         * This build is done because we need a previous action object */
        AbstractBuild build1 = jenkins.getProjectBuilder( project ).build();
        new SystemValidator( build1 ).validateBuild( Result.SUCCESS ).validate();

        /* Because we have ANY promotion level, the build must NOT fail */
        AbstractBuild build2 = jenkins.getProjectBuilder( project ).build();
        new SystemValidator( build2 ).validateBuild( Result.SUCCESS ).validate();
    }

}
