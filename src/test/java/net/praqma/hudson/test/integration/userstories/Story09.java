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
import net.praqma.junit.TestDescription;
import net.praqma.util.debug.Logger;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;

import static org.junit.Assert.*;

public class Story09 {

	@ClassRule
	public static CCUCMRule jenkins = new CCUCMRule();
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm-story09" );

	private static Logger logger = Logger.getLogger();

	@Test
	@ClearCaseUniqueVobName( name = "story09" )
	@TestDescription( title = "Story 9", text = "New baseline, bl1, on dev stream, dev1, poll on child, don't create baselines", 
	outcome = { "Build baseline is bl1",
				"Created baseline is null", 
				"Job is SUCCESS" } 
	)
	public void story09() throws Exception {
		
		AbstractBuild<?, ?> build = jenkins.initiateBuild( "story09", "child", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, false );

		/* Build validation */
		assertTrue( build.getResult().isBetterOrEqualTo( Result.SUCCESS ) );
		
		Baseline b = ccenv.context.baselines.get( "model-1" ).load();
		
		/* Expected build baseline */
		Baseline buildBaseline = jenkins.getBuildBaseline( build );
		assertEquals( b, buildBaseline );
		assertEquals( PromotionLevel.REJECTED, buildBaseline.getPromotionLevel( true ) );
		
		/* Created baseline */
		Baseline createdBaseline = jenkins.getCreatedBaseline( build );
		assertNull( createdBaseline );
	}
	
}
