package net.praqma.hudson.test.integration.userstories;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.junit.DescriptionRule;
import net.praqma.junit.TestDescription;

import net.praqma.clearcase.test.junit.ClearCaseRule;

public class Story12 {

	@ClassRule
	public static CCUCMRule jenkins = new CCUCMRule();
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm-story12", "setup-story12.xml" );
	
	@Rule
	public static DescriptionRule desc = new DescriptionRule();

	@Test
	@TestDescription( title = "Story 12, JENKINS-14436", text = "Not all versions, only the latest are shown" )
	public void story12() throws Exception {
		
		/* First build to create a view */
		AbstractBuild<?, ?> firstbuild = jenkins.initiateBuild( ccenv.getUniqueName(), "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, false, false );

		/* Validate first build */
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		new SystemValidator( firstbuild ).validateBuild( Result.SUCCESS ).validateBuiltBaseline( PromotionLevel.BUILT, baseline, false ).validate();
		
		AbstractBuild<?, ?> build = jenkins.buildProject( firstbuild.getProject(), false );
		
		Baseline baseline2 = ccenv.context.baselines.get( "model-2" );
		new SystemValidator( build ).validateBuild( Result.SUCCESS ).validateBuiltBaseline( PromotionLevel.BUILT, baseline2, false ).validate();
		
		ChangeLogSet<? extends Entry> chlog = build.getChangeSet();
	}

	
}
