package net.praqma.hudson.test.integration.userstories;

import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.hudson.scm.pollingmode.PollSelfMode;
import net.praqma.hudson.test.SystemValidator;

public class JENKINS13944 extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-13944" );
	
	@Rule
	public DescriptionRule desc = new DescriptionRule();

	@Test
	@TestDescription( title = "JENKINS-13944", text = "A baseline ís built successfully, but the tagging is not done, because the tag tyoe is not installed", configurations = { "Tag = true" }	)
	public void jenkins13944() throws Exception {
		
		/* First build to create a view */
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), new PollSelfMode("INTIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, true, false, false, false );
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		new SystemValidator( build ).validateBuild( Result.UNSTABLE ).validateBaselineTag( baseline, false ).validate();
	}

	
}
