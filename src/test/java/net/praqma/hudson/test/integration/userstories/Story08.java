package net.praqma.hudson.test.integration.userstories;

import net.praqma.hudson.test.BaseTestClass;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;
import net.praqma.util.debug.Logger;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.hudson.scm.pollingmode.PollChildMode;


import static org.junit.Assert.*;

public class Story08 extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "ccucm-story08", "setup-story10.xml" );
	
	@Rule
	public DescriptionRule desc = new DescriptionRule();

	@Test
	@ClearCaseUniqueVobName( name = "story08a" )
	@TestDescription( title = "Story 08 a", text = "No new baseline on dev stream, poll on child" )
	public void story08a() throws Exception {
		/* Flip promotion level */
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		baseline.setPromotionLevel( PromotionLevel.REJECTED );
		
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), new PollChildMode("INITIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, false );
		
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.NOT_BUILT )
		.validateBuiltBaselineNotFound()
		.validateCreatedBaseline( false )
		.validate();
	}
	
	@Test
	@ClearCaseUniqueVobName( name = "story08b" )
	@TestDescription( title = "Story 08 b, polling", text = "No new baseline on dev stream, poll on child" )
	public void story08b() throws Exception {
		/* Flip promotion level */
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		baseline.setPromotionLevel( PromotionLevel.INITIAL );
		
		/* First build must succeed to get a workspace */
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), new PollChildMode("INITIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, false );
		AbstractProject<?, ?> project = build.getProject();
		assertTrue( build.getResult().isBetterOrEqualTo( Result.SUCCESS ) );		
				
		TaskListener listener = jenkins.createTaskListener();
		PollingResult result = project.poll( listener );

		/* Polling result validation */
		assertEquals( PollingResult.NO_CHANGES, result );
		
	}
		
}
