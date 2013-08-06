package net.praqma.hudson.test;

import hudson.FilePath;
import net.praqma.util.test.junit.LoggingRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * User: cwolfgang
 */
public class BaseTestClass {

    private static Logger logger = Logger.getLogger( BaseTestClass.class.getName() );

    @ClassRule
    public static CCUCMRule jenkins = new CCUCMRule();

    @ClassRule
    public static LoggingRule lrule = new LoggingRule( "net.praqma" );


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
}
