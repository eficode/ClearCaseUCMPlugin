package net.praqma.hudson.test.integration.userstories;

import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.model.Result;
import hudson.scm.PollingResult;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.junit.DescriptionRule;
import net.praqma.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

import static net.praqma.hudson.test.CCUCMRule.ProjectCreator.Type.child;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author cwolfgang
 *         Date: 04-02-13
 *         Time: 12:00
 */
public class JENKINS16636 extends BaseTestClass {

    @Rule
    public static ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-16636", "setup-basic.xml" );

    @Rule
    public static DescriptionRule desc = new DescriptionRule();

    @Test
    @TestDescription( title = "JENKINS-16636", text = "No new baseline found --> But the job builds anyway" )
    public void jenkins16636() throws Exception {
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-16636", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() ).setType( child ).getProject();

        PollingResult result = project.poll( jenkins.createTaskListener() );

        assertFalse( result.hasChanges() );
    }



}
