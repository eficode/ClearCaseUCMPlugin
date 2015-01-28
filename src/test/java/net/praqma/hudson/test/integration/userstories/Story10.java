package net.praqma.hudson.test.integration.userstories;

import net.praqma.hudson.test.BaseTestClass;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;

import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollChildMode;


public class Story10 extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "ccucm-story10", "setup-story10.xml" );
	
	@Rule
	public DescriptionRule desc = new DescriptionRule();

	@Test
	@TestDescription( title = "Story 10", text = "New baseline, bl1, on dev stream, dev1, poll on child, create baselines, but wrong baseline template", configurations = { "Create baselines = true", "Name template = [fail]" } )
	public void story10() throws Exception {
		PollChildMode mode = new PollChildMode("INITIAL");
        mode.setCreateBaseline(true);
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), mode, "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, false, "[what]-)(/&" );

		Baseline b = ccenv.context.baselines.get( "model-1" ).load();
		
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.FAILURE )
		.validateCreatedBaseline( false )
		.validate();
	}
	
}
