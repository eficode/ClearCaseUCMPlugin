package net.praqma.hudson.test.integration.self;

import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;
import net.praqma.clearcase.Environment;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.util.debug.Logger;

public class Polling {
	
	@ClassRule
	public static CCUCMRule jenkins = new CCUCMRule();
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm" );

	private static Logger logger = Logger.getLogger();

	@Test
	@ClearCaseUniqueVobName( name = "" )
	public void testPollingSelfWithBaselines() throws Exception {
		FreeStyleProject project = jenkins.setupProject( "polling-test-with-baselines-" + ccenv.getVobName(), "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false );
		
		PollingResult result = project.poll( jenkins.createTaskListener() );
		
		assertTrue( result.hasChanges() );
		//testCCUCMPolling( project );
	}

	/*
	public void testPollingSelfWithNoBaselines() throws Exception {
		FreeStyleProject project = setupProject( "polling-test-with-baselines-" + ccenv.getVobName(), "self", "one_dev@" + ccenv.getPVob(), false, false, false, false );
		
		PollingResult result = project.poll( createTaskListener() );
		
		assertFalse( result.hasChanges() );
	}
	*/
	
}
