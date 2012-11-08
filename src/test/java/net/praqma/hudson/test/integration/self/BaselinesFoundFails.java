package net.praqma.hudson.test.integration.self;

import static org.junit.Assert.*;

import net.praqma.hudson.test.BaseTestClass;
import net.praqma.util.test.junit.LoggingRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import net.praqma.clearcase.Environment;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.junit.TestDescription;
import net.praqma.util.debug.Logger;

import java.util.logging.Level;

public class BaselinesFoundFails extends BaseTestClass {
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm" );
	
	private static Logger logger = Logger.getLogger();
	
	public AbstractBuild<?, ?> initiateBuild( String projectName, boolean recommend, boolean tag, boolean description, boolean fail ) throws Exception {
		return jenkins.initiateBuild( projectName, "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), recommend, tag, description, fail, false );
	}

	@Test
	@ClearCaseUniqueVobName( name = "self-failed-nop" )
	@TestDescription( title = "Self polling", text = "baseline available, build fails" )
	public void testNoOptions() throws Exception {
		AbstractBuild<?, ?> build = initiateBuild( "no-options-" + ccenv.getUniqueName(), false, false, false, true );
		
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.FAILURE )
		.validateBuiltBaseline( PromotionLevel.REJECTED, baseline, false )
		.validate();
	}
	
	@Test
	@ClearCaseUniqueVobName( name = "self-failed-recommend" )
	@TestDescription( title = "Self polling", text = "baseline available, build fails", configurations = { "Recommend = true" }	)
	public void testRecommended() throws Exception {
		AbstractBuild<?, ?> build = initiateBuild( "recommended-" + ccenv.getUniqueName(), true, false, false, true );
		
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.FAILURE )
		.validateBuiltBaseline( PromotionLevel.REJECTED, baseline, false )
		.validate();
	}
	
	@Test
	@ClearCaseUniqueVobName( name = "self-failed-tagged" )
	@TestDescription( title = "Self polling", text = "baseline available, build fails",	configurations = { "Set tag = true" } )
	public void testTagged() throws Exception {
		jenkins.makeTagType( ccenv.getPVob() );
		AbstractBuild<?, ?> build = initiateBuild( "tagged-" + ccenv.getUniqueName(), false, true, false, true );
		
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.FAILURE )
		.validateBuiltBaseline( PromotionLevel.REJECTED, baseline, false )
		.validateBaselineTag( baseline, true )
		.validate();
	}
}
