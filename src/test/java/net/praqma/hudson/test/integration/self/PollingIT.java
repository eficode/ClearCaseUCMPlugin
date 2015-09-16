package net.praqma.hudson.test.integration.self;

import hudson.model.FreeStyleProject;
import hudson.scm.PollingResult;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollSelfMode;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.util.debug.Logger;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;

public class PollingIT extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "ccucm" );

	private static Logger logger = Logger.getLogger();

	@Test
	@ClearCaseUniqueVobName( name = "self-changes" )
	public void testPollingSelfWithBaselines() throws Exception {
		FreeStyleProject project = jenkins.setupProjectWithASlave( "polling-test-with-baselines-" + ccenv.getUniqueName(), new PollSelfMode("INITIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false );
		
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
		FreeStyleProject project = jenkins.setupProjectWithASlave( "polling-test-with-baselines-" + ccenv.getUniqueName(), new PollSelfMode("INITIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false );
		
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
