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

/**
 * @author cwolfgang
 */
public class JENKINS18280 extends BaseTestClass {

    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-18280" );

    @Rule
    public DescriptionRule desc = new DescriptionRule();

    @Test
    @TestDescription( title = "JENKINS-18280", text = "Testing the swipe function. Off and should leave the view private files as they were." )
    @ClearCaseUniqueVobName( name = "Remain" )
    public void jenkins18280() throws Exception {
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-18280", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() ).setSwipe( false ).getProject();

        /* I need a first build */
        jenkins.getProjectBuilder( project ).build();

        FilePath path = new FilePath( project.getLastBuiltOn().getWorkspaceFor( (FreeStyleProject)project ), "view/" + ccenv.getUniqueName() );

        FilePath file = new FilePath( path, "test.txt" );
        file.write( "hello", "utf-8" );

        AbstractBuild build1 = jenkins.getProjectBuilder( project ).build();

        new SystemValidator( build1 ).
                validateBuild( Result.SUCCESS ).
                addElementToPathCheck( path, new SystemValidator.Element( "test.txt", true ) ).
                validate();
    }

    @Test
    @TestDescription( title = "JENKINS-18280", text = "Testing the swipe function. On and should remove the view private files." )
    @ClearCaseUniqueVobName( name = "Swiped" )
    public void jenkins18280Swiped() throws Exception {
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-18280-swiped", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() ).setSwipe( true ).getProject();

        /* I need a first build */
        jenkins.getProjectBuilder( project ).build();

        FilePath path = new FilePath( project.getLastBuiltOn().getWorkspaceFor( (FreeStyleProject)project ), "view/" + ccenv.getUniqueName() );

        FilePath file = new FilePath( path, "test.txt" );
        file.write( "hello", "utf-8" );

        AbstractBuild build1 = jenkins.getProjectBuilder( project ).build();

        new SystemValidator( build1 ).
                validateBuild( Result.SUCCESS ).
                addElementToPathCheck( path, new SystemValidator.Element( "test.txt", false ) ).
                validate();
    }



}
