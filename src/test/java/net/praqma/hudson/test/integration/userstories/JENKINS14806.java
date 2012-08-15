package net.praqma.hudson.test.integration.userstories;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.scm.CCUCMScm;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.junit.DescriptionRule;
import net.praqma.junit.TestDescription;

import net.praqma.clearcase.test.junit.ClearCaseRule;

@RunWith( PowerMockRunner.class )
public class JENKINS14806 {

	@ClassRule
	public static CCUCMRule jenkins = new CCUCMRule();
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-14806", "setup-JENKINS-14806.xml" );
	
	@Rule
	public static DescriptionRule desc = new DescriptionRule();

	@Test
	@TestDescription( title = "JENKINS-14806", text = "Multisite polling finds the same baseline times", configurations = { "ClearCase multisite = true" }	)
	public void jenkins13944() throws Exception {
	
		CCUCMScm ccucm = jenkins.getCCUCM( "child", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), "INITIAL", false, false, false, false, true, "[project]_[date]_[time]" );
		CCUCMScm ccucmspy = Mockito.spy( ccucm );
		
		/* Behaviour */		
		PowerMockito.doReturn( true ).when( ccucmspy ).getMultisitePolling();
		
		FreeStyleProject project = jenkins.createProject( ccenv.getUniqueName(), ccucmspy );
		
		/* First build to create a view */
		AbstractBuild<?, ?> build = jenkins.buildProject( project, false );
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		new SystemValidator( build ).validateBuild( Result.SUCCESS ).validateBuiltBaseline( PromotionLevel.BUILT, baseline, false ).validate();
	}

	
}
