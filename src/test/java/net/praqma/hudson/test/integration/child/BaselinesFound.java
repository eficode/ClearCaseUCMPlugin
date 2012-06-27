package net.praqma.hudson.test.integration.child;

import java.io.File;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.Environment;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.util.debug.Logger;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;

import static org.junit.Assert.*;

public class BaselinesFound {

	@ClassRule
	public static CCUCMRule jenkins = new CCUCMRule();
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm" );

	private static Logger logger = Logger.getLogger();
		
	public AbstractBuild<?, ?> initiateBuild( String projectName, boolean recommend, boolean tag, boolean description, boolean fail ) throws Exception {
		return jenkins.initiateBuild( projectName, "child", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), recommend, tag, description, fail, true );
	}

	@Test
	@ClearCaseUniqueVobName( name = "polling" )
	public void testNoOptions() throws Exception {
		
		/**/
		String viewtag = ccenv.getVobName() + "_one_dev";
		System.out.println( "VIEW: " + ccenv.context.views.get( viewtag ) );
		File path = new File( ccenv.context.mvfs + "/" + ccenv.getVobName() + "_one_dev/" + ccenv.getVobName() );
				
		System.out.println( "PATH: " + path );
		
		Stream stream = Stream.get( "one_dev", ccenv.getPVob() );
		Activity activity = Activity.create( "ccucm-activity", stream, ccenv.getPVob(), true, "ccucm activity", null, path );
		UCMView.setActivity( activity, path, null, null );
		
		Baseline s = ccenv.context.baselines.get( "_System_2.0" );
		
		/*
		String cmd = "rebase -baseline " + s + " -view " + viewtag + " -stream " + stream + " " + " -complete ";
		try {
			Cleartool.run( cmd, path );
		} catch( Exception e ) {
			logger.fatal( e );
		}
		*/
		
		
		try {
			//coolTest.addNewContent( CoolTestCase.context.components.get( "Model" ), path, "test2.txt" );
			ccenv.addNewElement( ccenv.context.components.get( "Model" ), path, "test2.txt" );
		} catch( ClearCaseException e ) {
			ExceptionUtils.print( e, System.out, true );
		}
		Baseline baseline = Baseline.create( "baseline-for-test", ccenv.context.components.get( "_System" ), path, LabelBehaviour.FULL, false );
		
		AbstractBuild<?, ?> build = initiateBuild( "no-options-" + ccenv.getVobName(), false, false, false, false );
		//interactiveBreak();
		/* Build validation */
		assertTrue( build.getResult().isBetterOrEqualTo( Result.SUCCESS ) );
		
		/* Expected build baseline */
		logger.info( "Build baseline: " + jenkins.getBuildBaseline( build ) );
		
		//Baseline baseline = CoolTestCase.context.baselines.get( "model-1" );
		
		jenkins.assertBuildBaseline( baseline, build );
		assertFalse( jenkins.isRecommended( baseline, build ) );
		assertNull( jenkins.getTag( baseline, build ) );
		jenkins.samePromotionLevel( baseline, PromotionLevel.BUILT );
		
		jenkins.testCreatedBaseline( build );
	}
	
}
