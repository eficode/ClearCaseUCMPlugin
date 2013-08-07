package net.praqma.hudson.test.integration.userstories;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Result;
import net.praqma.clearcase.Deliver;
import net.praqma.clearcase.Rebase;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.exceptions.UCMEntityNotFoundException;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.exceptions.UnableToLoadEntityException;
import net.praqma.clearcase.interfaces.Diffable;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.*;
import net.praqma.hudson.scm.ChangeLogEntryImpl;
import net.praqma.hudson.scm.ChangeLogSetImpl;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author cwolfgang
 */
public class JENKINS18281 extends BaseTestClass {

    private static Logger logger = Logger.getLogger( JENKINS18281.class.getName() );

    @Rule
    public static ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-18281", "setup-basic.xml" );

    @Rule
    public static DescriptionRule desc = new DescriptionRule();

    @Test
    @TestDescription( title = "JENKINS-18281", text = "Possibility to remove the contributing activities from the change set of the build" )
    public void jenkins18281() throws Exception {

        Stream target = ccenv.context.streams.get( "one_int" );
        Stream stream = ccenv.context.streams.get( "one_dev" );
        Component component = ccenv.context.components.get( "_System" );

        /* Create first baseline on int and rebase dev */
        ClearCaseRule.ContentCreator ccint1 = ccenv.getContentCreator().setBaselineName( "blint-1" ).setFilename( "foo.bar" ).setActivityName( "int-act1" ).setStreamName( "one_int" ).setPostFix( "_one_int" ).setNewElement( true ).create();
        new Rebase( stream ).addBaseline( ccint1.getBaseline() ).rebase( true );

        /* Create baselines on dev and deliver them to int */
        ClearCaseRule.ContentCreator cc1 = ccenv.getContentCreator().setBaselineName( "bl-1" ).setFilename( "foo.bar" ).setActivityName( "dev-act1" ).create();
        ClearCaseRule.ContentCreator cc2 = ccenv.getContentCreator().setBaselineName( "bl-2" ).setFilename( "foo.bar" ).setActivityName( "dev-act2" ).create();
        ClearCaseRule.ContentCreator cc3 = ccenv.getContentCreator().setBaselineName( "bl-3" ).setFilename( "foo.bar" ).setActivityName( "dev-act3" ).create();

        /* First */
        Deliver deliver = new Deliver( cc1.getBaseline(), stream, target, ccint1.getPath(), ccint1.getViewTag() );
        deliver.deliver( true, true, false, false );

        /* second */
        Deliver deliver2 = new Deliver( cc2.getBaseline(), stream, target, ccint1.getPath(), ccint1.getViewTag() );
        deliver2.deliver( true, true, false, false );

        /* Third */
        Deliver deliver3 = new Deliver( cc3.getBaseline(), stream, target, ccint1.getPath(), ccint1.getViewTag() );
        deliver3.deliver( true, true, false, false );

        /* Create the last baseline on dev */
        Baseline blint2 = Baseline.create( "blint-2", component,ccint1.getPath(), Baseline.LabelBehaviour.FULL, false );

        /* Create the Jenkins project for the dev stream */
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-18281", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() ).getProject();

        /* I need a first build */
        jenkins.getProjectBuilder( project ).build();

        /* Deliver bl1 */
        //Deliver deliver = new Deliver( bl1, bl1.getStream(), target, cc1.getPath(), cc1.getViewTag() );
        //deliver.deliver( true, true, false, false );
        //new Rebase( target ).addBaseline( bl1 ).rebase( true );

        printDiffs( blint2, ccint1.getBaseline(), ccint1.getPath() );
        printDiffs( blint2, cc2.getBaseline(), ccint1.getPath() );

        AbstractBuild build2 = jenkins.getProjectBuilder( project ).build();
        printChangeLog( build2 );

        FilePath path = new FilePath( project.getLastBuiltOn().getWorkspaceFor( (FreeStyleProject)project ), "view/" + ccenv.getUniqueName() + "/Model" );
        listPath( path );

        new SystemValidator( build2 ).validateBuild( Result.SUCCESS ).validate();
    }

    public void printChangeLog( AbstractBuild build ) {
        ChangeLogSetImpl cls = (ChangeLogSetImpl) build.getChangeSet();
        for( ChangeLogEntryImpl e : cls.getEntries() ) {
            System.out.println( "Author           : " + e.getAuthor() );
            System.out.println( "Activity headline: " + e.getActHeadline() );
            System.out.println( "Affected paths   : " + e.getAffectedPaths() );
            System.out.println( "Message          : " + e.getMsg() );
        }
    }

    public void printDiffs( Diffable d1, Diffable d2, File path ) throws UnableToLoadEntityException, UnableToInitializeEntityException, CleartoolException, UCMEntityNotFoundException {
        List<Activity> activities = Version.getBaselineDiff( d1, d2, true, path );
        for( int i = 0 ; i < activities.size() ; ++i ) {
            System.out.println( "Activity #" + (i+1) );
            Activity a = activities.get( i );
            printActivity( a );
        }
    }

    public void printActivity( Activity activity ) {
        System.out.println( "Activity: " + activity.getFullyQualifiedName() );
        System.out.println( "Headline: " + activity.getHeadline() );
        System.out.println( "Versions: " );
        for( Version v : activity.changeset.versions ) {
            System.out.println( " * " + v );
        }
    }

}
