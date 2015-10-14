package net.praqma.hudson.test.integration.userstories;

import java.io.IOException;
import java.util.concurrent.Future;

import net.praqma.hudson.test.BaseTestClass;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.TestBuilder;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.scm.PollingResult;
import hudson.triggers.SCMTrigger;
import net.praqma.hudson.scm.CCUCMScm;

import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollChildMode;

import static org.junit.Assert.*;

import static org.hamcrest.CoreMatchers.*;

public class JENKINS14806 extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-14806", "setup-JENKINS-14806.xml" );
	
	@Rule
	public DescriptionRule desc = new DescriptionRule();

	@Test
	@TestDescription( title = "JENKINS-14806", text = "Multisite polling finds the same baseline multiple times", configurations = { "ClearCase multisite = true" }	)
	public void jenkins14806() throws Exception {        
        PollChildMode mode = new PollChildMode("INITIAL");
        mode.setCreateBaseline(true);
        
        CCUCMScm ccucm = new CCUCMScm("_System@"+ccenv.getPVob(), "ALL", false, mode, "one_int@"+ccenv.getPVob(), "successful", "[project]_[date]_[time]", true, false, false, false, "", true, false, false);
		
		ccucm.setMultisitePolling( true );
		System.out.println( "MP: " + ccucm.getMultisitePolling() );
		FreeStyleProject project = jenkins.createProject( ccenv.getUniqueName(), ccucm );
       		
		/* First build to create a view */
		System.out.println( "First build" );
		jenkins.buildProject( project, false );
        
		/* Add builder to sleep */
		System.out.println( "Adding waiter" );
		project.getBuildersList().add( new WaitBuilder() );
		
		System.out.println( "Async build" );
		Future<FreeStyleBuild> futureBuild = project.scheduleBuild2( 0, new SCMTrigger.SCMTriggerCause("Triggered for test") );
		
		/* Remove the builder again */
		System.out.println( "Clear builders" );
		project.getBuildersList().clear();
		
		/* And then poll */
        System.out.println("Executors on master = "+ jenkins.jenkins.getNumExecutors());
		System.out.println( "Poll ~ is building = " + project.isBuilding() + " is queued = " + project.isInQueue() );
		PollingResult result = project.poll( jenkins.createTaskListener() );

        doWait = false;
		
		System.out.println( "Assert" );
        assertThat( "The previous build was finished and there were changes!", result, is( PollingResult.NO_CHANGES ) );
		
		System.out.println( "Waiting for waiter" );
		futureBuild.get();
	}

    public volatile boolean doWait = true;

	public class WaitBuilder extends TestBuilder {

		@Override
		public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener ) throws InterruptedException, IOException {
			System.out.println( "Sleeping...." );
            while( doWait ) {}
			System.out.println( "Awaken...." );
			return true;
		}
		
	}
    
}
