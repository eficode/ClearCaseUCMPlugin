package net.praqma.hudson.test.integration.userstories;

import net.praqma.hudson.test.BaseTestClass;
import net.praqma.util.test.junit.LoggingRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.junit.DescriptionRule;
import net.praqma.junit.TestDescription;

import net.praqma.clearcase.test.junit.ClearCaseRule;

import java.util.logging.Level;

public class JENKINS14702 extends BaseTestClass {
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-14702", "setup-JENKINS-14702.xml" );
	
	@Rule
	public static DescriptionRule desc = new DescriptionRule();

	@Test
	@TestDescription( title = "JENKINS-14702", text = "Use the current streams project, if the jenkins build project is not found", configurations = { "Jenkins project = not created" }	)
	public void jenkins13944() throws Exception {
	
		/* First build to create a view */
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, false, false );
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		new SystemValidator( build ).validateBuild( Result.SUCCESS ).validateBuiltBaseline( PromotionLevel.BUILT, baseline, false ).validate();
	}

	
}
