package net.praqma.hudson.test.integration.child;

import static org.junit.Assert.*;

import java.io.File;
import java.util.logging.Level;

import net.praqma.hudson.test.BaseTestClass;
import net.praqma.util.test.junit.LoggingRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;
import net.praqma.clearcase.Environment;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.UCMEntityNotFoundException;
import net.praqma.clearcase.exceptions.UnableToCreateEntityException;
import net.praqma.clearcase.exceptions.UnableToGetEntityException;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.junit.TestDescription;
import net.praqma.util.debug.Logger;

public class Polling extends BaseTestClass {
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm-child-polling" );

	private static Logger logger = Logger.getLogger();

	@Test
	@ClearCaseUniqueVobName( name = "changes-child" )
	@TestDescription( title = "Child polling, polling", text = "baseline available" )
	public void testPollingChildsWithChanges() throws Exception {
		FreeStyleProject project = jenkins.setupProject( "polling-test-with-baselines-" + ccenv.getUniqueName(), "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false );
		
		File path = setActivity();
		Baseline b1 = getNewBaseline( path, "file1.txt" );
		Baseline b2 = getNewBaseline( path, "file2.txt" );
		
		FreeStyleBuild build = null;
		try {
			build = project.scheduleBuild2( 0 ).get();
		} catch( Exception e ) {
			logger.info( "Build failed: " + e.getMessage() );
		}
		
		logger.info( "Build DONE, now polling" );
		
		PollingResult result = project.poll( jenkins.createTaskListener() );
		
		assertTrue( result.hasChanges() );
	}
	
	@Test
	@ClearCaseUniqueVobName( name = "nochanges-child" )
	@TestDescription( title = "Child polling, polling", text = "baseline available" )
	public void testPollingChildsWithNoChanges() throws Exception {
		FreeStyleProject project = jenkins.setupProject( "polling-test-with-baselines-" + ccenv.getUniqueName(), "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false );
		
		File path = setActivity();
		Baseline b1 = getNewBaseline( path, "file1.txt" );
		
		FreeStyleBuild build = null;
		try {
			build = project.scheduleBuild2( 0 ).get();
		} catch( Exception e ) {
			logger.info( "Build failed: " + e.getMessage() );
		}
		
		logger.info( "Build DONE, now polling" );
		
		PollingResult result = project.poll( jenkins.createTaskListener() );
		
		assertTrue( result.hasChanges() );
	}
	
	protected File setActivity() throws ClearCaseException {
		/**/
		String viewtag = ccenv.getUniqueName() + "_one_dev";
		System.out.println( "VIEW: " + ccenv.context.views.get( viewtag ) );
		File path = new File( ccenv.context.mvfs + "/" + viewtag + "/" + ccenv.getVobName() );
				
		System.out.println( "PATH: " + path );
		
		Stream stream = Stream.get( "one_dev", ccenv.getPVob() );
		Activity activity = Activity.create( "ccucm-activity", stream, ccenv.getPVob(), true, "ccucm activity", null, path );
		UCMView.setActivity( activity, path, null, null );
		
		return path;
	}

	protected Baseline getNewBaseline( File path, String filename ) throws ClearCaseException {
		
		try {
			ccenv.addNewElement( ccenv.context.components.get( "Model" ), path, filename );
		} catch( ClearCaseException e ) {
			ExceptionUtils.print( e, System.out, true );
		}
		return Baseline.create( "baseline-for-test", ccenv.context.components.get( "_System" ), path, LabelBehaviour.FULL, false );
	}
	
}
