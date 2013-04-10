package net.praqma.hudson.test;

import net.praqma.util.test.junit.LoggingRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

/**
 * User: cwolfgang
 */
public class BaseTestClass {

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
}
