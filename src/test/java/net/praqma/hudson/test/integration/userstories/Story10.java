package net.praqma.hudson.test.integration.userstories;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.scm.CCUCMScm;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.junit.DescriptionRule;
import net.praqma.junit.TestDescription;
import net.praqma.util.debug.Logger;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;

import static org.junit.Assert.*;

public class Story10 {

	@ClassRule
	public static CCUCMRule jenkins = new CCUCMRule();
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm-story10", "setup-story10.xml" );
	
	@Rule
	public static DescriptionRule desc = new DescriptionRule();

	private static Logger logger = Logger.getLogger();

	@Test
	@TestDescription( title = "Story 10", text = "New baseline, bl1, on dev stream, dev1, poll on child, create baselines, but wrong baseline template", 
	outcome = { "Build baseline is bl1 and BUILT",
				"Created baseline is null", 
				"Job is FAILED" } 
	)
	public void story10() throws Exception {
		
		AbstractBuild<?, ?> build = jenkins.initiateBuild( "story10", "child", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, false, false, "[what]-)(/&" );

		/* Build validation */
		assertTrue( build.getResult().isBetterOrEqualTo( Result.FAILURE ) );
		
		Baseline b = ccenv.context.baselines.get( "model-1" ).load();
		
		/* Expected build baseline */
		Baseline buildBaseline = jenkins.getBuildBaseline( build );
		assertEquals( b, buildBaseline );
		assertEquals( PromotionLevel.BUILT, buildBaseline.getPromotionLevel( true ) );
		
		/* Created baseline */
		Baseline createdBaseline = jenkins.getCreatedBaseline( build );
		assertNull( createdBaseline );
	}
	
}
