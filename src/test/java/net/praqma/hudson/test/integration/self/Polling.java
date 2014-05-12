package net.praqma.hudson.test.integration.self;

import hudson.model.FreeStyleProject;
import hudson.scm.PollingResult;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.util.debug.Logger;
import net.praqma.util.test.junit.TestDescription;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;

public class Polling extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "ccucm" );

	private static Logger logger = Logger.getLogger();
    
    @Test
    @TestDescription(title = "Self polling, create baseline", text="This is a test that should return no-changes, if a baseline is specified")
    @ClearCaseUniqueVobName( name = "self-create-baseline")
    public void testPollingNoChangesWithCreateBaselines() throws Exception {
        FreeStyleProject project = jenkins.setupProjectWithASlave( "polling-test-with-baselines-" + ccenv.getUniqueName(), "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, true );
        
        
        /** Initial build **/
		try {
			project.scheduleBuild2( 0 ).get();
		} catch( Exception e ) {
			logger.info( "Build failed: " + e.getMessage() );
		}
        
        PollingResult result = project.poll(jenkins.createTaskListener());
        
        System.out.println("Changes: "+result.hasChanges());
        assertFalse("We expect no changes. According to the fix i made for JENKINS-18107", result.hasChanges());
    }

	@Test
	@ClearCaseUniqueVobName( name = "self-changes" )
	public void testPollingSelfWithBaselines() throws Exception {
		FreeStyleProject project = jenkins.setupProjectWithASlave( "polling-test-with-baselines-" + ccenv.getUniqueName(), "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false );
		
		/* BUILD 1 */
		try {
			project.scheduleBuild2( 0 ).get();
		} catch( Exception e ) {
			logger.info( "Build failed: " + e.getMessage() );
		}
		
		PollingResult result = project.poll( jenkins.createTaskListener() );
		
		assertTrue( result.hasChanges() );
	}
    
	public void testPollingSelfWithNoBaselines() throws Exception {
		FreeStyleProject project = jenkins.setupProjectWithASlave( "polling-test-with-baselines-" + ccenv.getUniqueName(), "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false );
		
		/* BUILD 1 */
		try {
			project.scheduleBuild2( 0 ).get();
		} catch( Exception e ) {
			logger.info( "Build failed: " + e.getMessage() );
		}		
		/* BUILD 2 */
		try {
			project.scheduleBuild2( 0 ).get();
		} catch( Exception e ) {
			logger.info( "Build failed: " + e.getMessage() );
		}
		
		/* BUILD 3 */
		try {
			project.scheduleBuild2( 0 ).get();
		} catch( Exception e ) {
			logger.info( "Build failed: " + e.getMessage() );
		}
		
		/* BUILD 4 */
		try {
			project.scheduleBuild2( 0 ).get();
		} catch( Exception e ) {
			logger.info( "Build failed: " + e.getMessage() );
		}
		
		/* BUILD 5 */
		try {
			project.scheduleBuild2( 0 ).get();
		} catch( Exception e ) {
			logger.info( "Build failed: " + e.getMessage() );
		}
		
		/* BUILD 6 */
		try {
			project.scheduleBuild2( 0 ).get();
		} catch( Exception e ) {
			logger.info( "Build failed: " + e.getMessage() );
		}
		
		PollingResult result = project.poll( jenkins.createTaskListener() );
		
		assertTrue( !result.hasChanges() );
	}
	
}
