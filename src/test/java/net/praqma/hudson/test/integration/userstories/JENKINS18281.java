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
    public ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-18281", "setup-basic.xml" );

    @Rule
    public DescriptionRule desc = new DescriptionRule();

    @Test
    @TestDescription( title = "JENKINS-18281", text = "Possibility to remove the contributing activities from the change set of the build. The WASHED CHANGESET fix." )
    public void jenkins18281() throws Exception {

        Stream target = ccenv.context.streams.get( "one_int" );
        Stream stream = ccenv.context.streams.get( "one_dev" );
        Component component = ccenv.context.components.get( "_System" );

        /* Create first baseline on int and rebase dev */
        ClearCaseRule.ContentCreator ccint1_1 = ccenv.getContentCreator().setFilename( "model.h" ).setActivityName( "int-act1" ).setStreamName( "one_int" ).setPostFix( "_one_int" ).setNewElement( true ).create();
        ClearCaseRule.ContentCreator ccint1_2 = ccenv.getContentCreator().setFilename( "common.h" ).setStreamName( "one_int" ).setPostFix( "_one_int" ).setNewElement( true ).create();
        ClearCaseRule.ContentCreator ccint1_3 = ccenv.getContentCreator().setFilename( "algorithm.h" ).setStreamName( "one_int" ).setPostFix( "_one_int" ).setNewElement( true ).create();
        Baseline bl_int_1 = Baseline.create( "blint1", component, ccint1_1.getPath(), Baseline.LabelBehaviour.FULL, false );

        new Rebase( stream ).addBaseline( bl_int_1 ).rebase( true );

        /* Create baseline on dev */
        ClearCaseRule.ContentCreator cc1_1 = ccenv.getContentCreator().setFilename( "model.h" ).setActivityName( "dev-act1" ).create();
        ClearCaseRule.ContentCreator cc1_2 = ccenv.getContentCreator().setFilename( "common.h" ).create();
        ClearCaseRule.ContentCreator cc1_3 = ccenv.getContentCreator().setFilename( "algorithm.h" ).create();

        Baseline bl_dev_1 = Baseline.create( "bl1", component, cc1_1.getPath(), Baseline.LabelBehaviour.FULL, false );

        /* Create second baseline on int and rebase dev */
        ClearCaseRule.ContentCreator ccint2_1 = ccenv.getContentCreator().setFilename( "model.h" ).setActivityName( "int-act2" ).setStreamName( "one_int" ).setPostFix( "_one_int" ).create();
        ClearCaseRule.ContentCreator ccint2_2 = ccenv.getContentCreator().setFilename( "common.h" ).setStreamName( "one_int" ).setPostFix( "_one_int" ).create();
        ClearCaseRule.ContentCreator ccint2_3 = ccenv.getContentCreator().setFilename( "algorithm.h" ).setStreamName( "one_int" ).setPostFix( "_one_int" ).create();
        Baseline bl_int_2 = Baseline.create( "blint1", component, ccint1_1.getPath(), Baseline.LabelBehaviour.FULL, false );

        new Rebase( stream ).addBaseline( bl_int_2 ).rebase( true );

        /* Create baseline on dev */
        ClearCaseRule.ContentCreator cc2_1 = ccenv.getContentCreator().setFilename( "model.h" ).setActivityName( "dev-act2" ).create();
        ClearCaseRule.ContentCreator cc2_2 = ccenv.getContentCreator().setFilename( "common.h" ).create();
        ClearCaseRule.ContentCreator cc2_3 = ccenv.getContentCreator().setFilename( "algorithm.h" ).create();

        ClearCaseRule.ContentCreator cc3_1 = ccenv.getContentCreator().setFilename( "model.h" ).setActivityName( "dev-act3" ).create();
        ClearCaseRule.ContentCreator cc3_2 = ccenv.getContentCreator().setFilename( "common.h" ).create();
        ClearCaseRule.ContentCreator cc3_3 = ccenv.getContentCreator().setFilename( "algorithm.h" ).create();

        ClearCaseRule.ContentCreator cc4_1 = ccenv.getContentCreator().setFilename( "model.h" ).setActivityName( "dev-act4" ).create();
        ClearCaseRule.ContentCreator cc4_2 = ccenv.getContentCreator().setFilename( "common.h" ).create();

        Baseline bl_dev_2 = Baseline.create( "bl2", component, cc2_1.getPath(), Baseline.LabelBehaviour.FULL, false );

        /* Create the Jenkins project for the dev stream */
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-18281", "_System@" + ccenv.getPVob(), "one_dev@" + ccenv.getPVob() ).getProject();

        /* I need a first build */
        jenkins.getProjectBuilder( project ).build();

        /* Deliver bl1 */
        //Deliver deliver = new Deliver( bl1, bl1.getStream(), target, cc1.getPath(), cc1.getViewTag() );
        //deliver.deliver( true, true, false, false );
        //new Rebase( target ).addBaseline( bl1 ).rebase( true );

        /*
        printDiffs( bl_int_2, bl_dev_2, cc1_1.getPath() );
        printDiffs( bl_dev_1, bl_dev_2, cc1_1.getPath() );
        */

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
