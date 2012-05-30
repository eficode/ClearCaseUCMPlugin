package net.praqma.hudson.test.integration.self;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.scm.CCUCMScm;
import net.praqma.jenkins.utils.test.ClearCaseJenkinsTestCase;
import net.praqma.util.debug.Logger;

public class BaselinesFound extends ClearCaseJenkinsTestCase {

	private static Logger logger = Logger.getLogger();
	
	public FreeStyleProject setupProject( boolean recommend, boolean tag, boolean description ) throws Exception {
		String uniqueTestVobName = "ccucm" + coolTest.uniqueTimeStamp;
		coolTest.variables.put( "vobname", uniqueTestVobName );
		coolTest.variables.put( "pvobname", uniqueTestVobName + "_PVOB" );
		
		coolTest.bootStrap();
		
		logger.info( "Setting up build for self polling, recommend:" + recommend + ", tag:" + tag + ", description:" + description );
		
		FreeStyleProject project = createFreeStyleProject( "ccucm-project-" + uniqueTestVobName );
		
		// boolean createBaseline, String nameTemplate, boolean forceDeliver, boolean recommend, boolean makeTag, boolean setDescription
		CCUCMScm scm = new CCUCMScm( "Model@" + coolTest.getPVob(), "INITIAL", "ALL", false, "self", uniqueTestVobName + "_one_int@" + coolTest.getPVob(), "successful", false, "", true, recommend, tag, description, "jenkins" );
		
		project.setScm( scm );
		
		return project;
	}
	
	public CCUCMBuildAction getBuildAction( AbstractBuild<?, ?> build ) {
		/* Check the build baseline */
		logger.info( "Getting ccucm build action from " + build );
		CCUCMBuildAction action = build.getAction( CCUCMBuildAction.class );
		assertNotNull( action.getBaseline() );
		return action;
	}
	
	public Baseline getBuildBaseline( AbstractBuild<?, ?> build ) {
		CCUCMBuildAction action = getBuildAction( build );
		assertNotNull( action.getBaseline() );
		return action.getBaseline();
	}
	
	
	public void testRecommend() throws Exception {
		FreeStyleProject project = setupProject( true, false, false );
		
		FreeStyleBuild b = project.scheduleBuild2( 0 ).get();
		
		logger.info( "Workspace: " + b.getWorkspace() );
		
		logger.info( "Logfile: " + b.getLogFile() );
		
		logger.info( "JENKINS::: " + getLog( b ) );
		
		/* Build validation */
		assertTrue( b.getResult().isBetterOrEqualTo( Result.SUCCESS ) );
		
		logger.info( "Build baseline: " + getBuildBaseline( b ) );
	}
}
