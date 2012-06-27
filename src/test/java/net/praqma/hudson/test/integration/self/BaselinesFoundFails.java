package net.praqma.hudson.test.integration.self;

import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import net.praqma.clearcase.Environment;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.util.debug.Logger;

public class BaselinesFoundFails {

	@ClassRule
	public static CCUCMRule jenkins = new CCUCMRule();
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm" );
	
	private static Logger logger = Logger.getLogger();
	
	public AbstractBuild<?, ?> initiateBuild( String projectName, boolean recommend, boolean tag, boolean description, boolean fail ) throws Exception {
		return jenkins.initiateBuild( projectName, "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), recommend, tag, description, fail, false );
	}

	@Test
	@ClearCaseUniqueVobName( name = "" )
	public void testNoOptions() throws Exception {
		AbstractBuild<?, ?> build = initiateBuild( "no-options-" + ccenv.getVobName(), false, false, false, true );
		
		/* Build validation */
		logger.info( "Checking result" );
		assertTrue( build.getResult().isBetterOrEqualTo( Result.FAILURE ) );
		
		/* Expected build baseline */
		logger.info( "Checking baseline" );
		logger.info( "Build baseline: " + jenkins.getBuildBaseline( build ) );
		
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		
		logger.info( "Assertions" );
		jenkins.assertBuildBaseline( baseline, build );
		assertFalse( jenkins.isRecommended( baseline, build ) );
		assertNull( jenkins.getTag( baseline, build ) );
		jenkins.samePromotionLevel( baseline, PromotionLevel.REJECTED );
	}
	
	@Test
	@ClearCaseUniqueVobName( name = "" )
	public void testRecommended() throws Exception {
		AbstractBuild<?, ?> build = initiateBuild( "recommended-" + ccenv.getVobName(), true, false, false, true );
		
		/* Build validation */
		assertTrue( build.getResult().isBetterOrEqualTo( Result.FAILURE ) );
		
		/* Expected build baseline */
		logger.info( "Build baseline: " + jenkins.getBuildBaseline( build ) );
		
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		
		jenkins.assertBuildBaseline( baseline, build );
		assertFalse( jenkins.isRecommended( baseline, build ) );
		assertNull( jenkins.getTag( baseline, build ) );
		jenkins.samePromotionLevel( baseline, PromotionLevel.REJECTED );
	}
	
	@Test
	@ClearCaseUniqueVobName( name = "" )
	public void testTagged() throws Exception {
		jenkins.makeTagType( ccenv.getPVob() );
		AbstractBuild<?, ?> build = initiateBuild( "tagged-" + ccenv.getVobName(), false, true, false, true );
		
		/* Build validation */
		assertTrue( build.getResult().isBetterOrEqualTo( Result.FAILURE ) );
		
		/* Expected build baseline */
		logger.info( "Build baseline: " + jenkins.getBuildBaseline( build ) );
		
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		
		jenkins.assertBuildBaseline( baseline, build );
		assertFalse( jenkins.isRecommended( baseline, build ) );
		assertNotNull( jenkins.getTag( baseline, build ) );
		jenkins.samePromotionLevel( baseline, PromotionLevel.REJECTED );
	}
}
