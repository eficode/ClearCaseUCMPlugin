package net.praqma.hudson.test;

import hudson.FilePath;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.exceptions.UCMEntityNotFoundException;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.exceptions.UnableToLoadEntityException;
import net.praqma.clearcase.interfaces.Diffable;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Version;
import org.junit.Before;
import org.junit.ClassRule;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import net.praqma.util.test.junit.LoggingRule;

public class BaseTestClass {

    private static final Logger logger = Logger.getLogger( BaseTestClass.class.getName() );

    @ClassRule
    public static CCUCMRule jenkins = new CCUCMRule();

    @ClassRule
    public static LoggingRule lrule = new LoggingRule( "net.praqma" );
    	
	@ClassRule
	public static LoggerRule loggerRule = new LoggerRule();
	

    @Before
    public void before() {
        jenkins.setOutputDir( getName() );
    }

    public String getName() {
        return this.getClass().getName();
    }


    protected void listPath( FilePath path ) throws IOException, InterruptedException {
        logger.info( "Listing " + path + "(" + path.exists() + ")" );
        for( FilePath f : path.list() ) {
            logger.info( " * " + f );
        }
    }

    protected void printDiffs( Diffable d1, Diffable d2, File path ) throws UnableToLoadEntityException, UnableToInitializeEntityException, CleartoolException, UCMEntityNotFoundException {
        List<Activity> activities = Version.getBaselineDiff( d1, d2, true, path );
        for( int i = 0 ; i < activities.size() ; ++i ) {
            System.out.println( "Activity #" + (i+1) );
            Activity a = activities.get( i );
            printActivity( a );
        }
    }

    protected void printActivity( Activity activity ) {
        System.out.println( "Activity: " + activity.getFullyQualifiedName() );
        System.out.println( "Headline: " + activity.getHeadline() );
        System.out.println( "Versions: " );
        for( Version v : activity.changeset.versions ) {
            System.out.println( " * " + v );
        }
    }
}
