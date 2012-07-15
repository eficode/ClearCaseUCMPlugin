package net.praqma.hudson.test.integration.userstories;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.junit.DescriptionRule;
import net.praqma.junit.TestDescription;

import net.praqma.clearcase.test.junit.ClearCaseRule;

public class JENKINS13944 {

	@ClassRule
	public static CCUCMRule jenkins = new CCUCMRule();
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm-story11" );
	
	@Rule
	public static DescriptionRule desc = new DescriptionRule();

	@Test
	@TestDescription( title = "JENKINS-13944", text = "A baseline ís built successfully, but the tagging is not done, because the tag tyoe is not installed", configurations = { "Tag = true" }	)
	public void jenkins13944() throws Exception {
		
		/* First build to create a view */
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, true, false, false, false, false );
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		new SystemValidator( build ).validateBuild( Result.UNSTABLE ).validateBaselineTag( baseline, false ).validate();
	}

	
}
