package net.praqma.hudson.test.integration.userstories;

import hudson.model.AbstractBuild;
import hudson.model.Project;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollSelfMode;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.CCUCMRule;
import org.junit.Rule;
import org.junit.Test;


/**
 * @author cwolfgang
 */
public class Story11IT extends BaseTestClass {

    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule( "Story11" );

    @Test
    public void test() throws Exception {
        /* First build to create a view */
        PollSelfMode mode = new PollSelfMode("INITIAL");
        
        Project project = new CCUCMRule.ProjectCreator( ccenv.getUniqueName(), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() )
                .setMode(mode)
                .getProject();

        AbstractBuild<?, ?> firstbuild = jenkins.buildProject( project, false );

        project.renameTo( project.getName() + "_renamed" );

        AbstractBuild<?, ?> secondbuild = jenkins.buildProject( project, false );
    }
}
