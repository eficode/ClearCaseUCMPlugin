package net.praqma.hudson.test.integration.userstories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import net.praqma.hudson.test.BaseTestClass;
import net.praqma.util.test.junit.LoggingRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.scm.ChangeLogEntryImpl;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.junit.DescriptionRule;
import net.praqma.junit.TestDescription;

import net.praqma.clearcase.test.junit.ClearCaseRule;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class JENKINS14436 extends BaseTestClass {
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-14436", "setup-JENKINS-14436.xml" );
	
	@Rule
	public static DescriptionRule desc = new DescriptionRule();

	@Test
	@TestDescription( title = "JENKINS-14436", text = "Not all versions, only the latest are shown" )
	public void jenkins14436() throws Exception {
		
		/* First build to create a view */
		AbstractBuild<?, ?> firstbuild = jenkins.initiateBuild( ccenv.getUniqueName(), "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, false, false );

		/* Validate first build */
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		new SystemValidator( firstbuild ).validateBuild( Result.SUCCESS ).validateBuiltBaseline( PromotionLevel.BUILT, baseline, false ).validate();
		
		AbstractBuild<?, ?> build = jenkins.buildProject( firstbuild.getProject(), false );
		
		Baseline baseline2 = ccenv.context.baselines.get( "model-2" );
		new SystemValidator( build ).validateBuild( Result.SUCCESS ).validateBuiltBaseline( PromotionLevel.BUILT, baseline2, false ).validate();
		
		ChangeLogSet<? extends Entry> ls = build.getChangeSet();
		
		Object[] items = ls.getItems();
		
		System.out.println( "ITEMS: " + items );
		
		assertThat( items.length, is( 1 ) );
		
		Collection<String> col = ((ChangeLogEntryImpl)items[0]).getAffectedPaths();
		assertThat( col.size(), is( 1 ) );

	}

	
}
