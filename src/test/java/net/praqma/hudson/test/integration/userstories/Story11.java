package net.praqma.hudson.test.integration.userstories;

import hudson.model.AbstractBuild;
import hudson.model.Project;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.CCUCMRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

/**
 * @author cwolfgang
 */
public class Story11 extends BaseTestClass {

    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule( "Story11" );

    @Test
    public void test() throws IOException {
        /* First build to create a view */
        Project project = new CCUCMRule.ProjectCreator( ccenv.getUniqueName(), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() )
                .setType( CCUCMRule.ProjectCreator.Type.self )
                .getProject();

        AbstractBuild<?, ?> firstbuild = jenkins.buildProject( project, false, null );

        project.renameTo( project.getName() + "_renamed" );

        AbstractBuild<?, ?> secondbuild = jenkins.buildProject( project, false, null );
    }
}
