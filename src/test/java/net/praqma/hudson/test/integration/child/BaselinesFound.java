package net.praqma.hudson.test.integration.child;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.test.junit.CoolTestCase;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.CCUCMTestCase;
import net.praqma.util.debug.Logger;

public class BaselinesFound extends CCUCMTestCase {

	private static Logger logger = Logger.getLogger();
	
	public AbstractBuild<?, ?> initiateBuild( String projectName, String uniqueTestVobName, boolean recommend, boolean tag, boolean description, boolean fail ) throws Exception {
		return initiateBuild( projectName, uniqueTestVobName, "child", recommend, tag, description, fail );
	}

	public void testNoOptions() throws Exception {
		String un = setupCC( false );
		AbstractBuild<?, ?> build = initiateBuild( "no-options-" + un, un, false, false, false, false );
		
		/* Build validation */
		assertTrue( build.getResult().isBetterOrEqualTo( Result.SUCCESS ) );
		
		/* Expected build baseline */
		logger.info( "Build baseline: " + getBuildBaseline( build ) );
		
		Baseline baseline = CoolTestCase.context.baselines.get( "model-1" );
		
		assertBuildBaseline( baseline, build );
		assertFalse( isRecommended( baseline, build ) );
		assertNull( getTag( baseline, build ) );
		samePromotionLevel( baseline, PromotionLevel.BUILT );
		
		testCreatedBaseline( build );
	}
	
}
