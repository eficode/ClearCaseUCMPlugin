package net.praqma.hudson.test;

import net.praqma.util.test.junit.LoggingRule;
import org.junit.ClassRule;

/**
 * User: cwolfgang
 * Date: 08-11-12
 * Time: 11:11
 */
public class BaseTestClass {
    @ClassRule
    public static CCUCMRule jenkins = new CCUCMRule();

    @ClassRule
    public static LoggingRule lrule = new LoggingRule( "net.praqma" );

}
