package net.praqma.hudson.test.integration.userstories;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Result;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author cwolfgang
 */
public class JENKINS18279 extends BaseTestClass {

    private static Logger logger = Logger.getLogger( JENKINS18279.class.getName() );

    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-18279" );

    @Rule
    public DescriptionRule desc = new DescriptionRule();

    @Test
    @TestDescription( title = "JENKINS-18279", text = "Testing the swipe function. UPDT's must remain as non-view private files." )
    public void jenkins18279() throws Exception {
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-18279", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() ).setSwipe( true ).getProject();

        /* I need a first build */
        jenkins.getProjectBuilder( project ).build();

        FilePath path = new FilePath( project.getLastBuiltOn().getWorkspaceFor( (FreeStyleProject)project ), "view/" );
        FilePath[] names = path.list( "*.updt" );

        listPath( path );

        AbstractBuild build1 = jenkins.getProjectBuilder( project ).build();

        SystemValidator validator = new SystemValidator( build1 ).validateBuild( Result.SUCCESS );

        for( FilePath n : names ) {
                validator.addElementToPathCheck( path, new SystemValidator.Element( n.getName(), true ) );
        }

         validator.validate();
    }


}
