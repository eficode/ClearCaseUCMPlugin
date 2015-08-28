package net.praqma.hudson.test.integration.self;


import net.praqma.hudson.test.BaseTestClass;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.TestDescription;

import net.praqma.hudson.scm.pollingmode.PollSelfMode;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BaselinesFound extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "ccucm" );
	
	@Test
	@ClearCaseUniqueVobName( name = "self-nop" )
	@TestDescription( title = "Self polling", text = "baseline available" )
	public void testNoOptions() throws Exception {
        AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), new PollSelfMode("INITIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false );
	
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.SUCCESS )
		.validateBuiltBaseline( PromotionLevel.BUILT, baseline, false )
		.validate();
	}
	
	@Test
	@ClearCaseUniqueVobName( name = "self-recommended" )
	@TestDescription( title = "Self polling", text = "baseline available", configurations = { "Recommend = true" } )
	public void testRecommended() throws Exception {
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), new PollSelfMode("INITIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), true, false, false, false );
		
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.SUCCESS )
		.validateBuiltBaseline( PromotionLevel.BUILT, baseline, true )
		.validate();
	}
	
	@ClearCaseUniqueVobName( name = "self-description" )
	@TestDescription( title = "Self polling", text = "baseline available", configurations = { "Set description = true" } )
	public void testDescription() throws Exception {
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), new PollSelfMode("INITIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, true, false );		
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.SUCCESS )
		.validateBuiltBaseline( PromotionLevel.BUILT, baseline, false )
		.validate();
	}
	
	@Test
	@ClearCaseUniqueVobName( name = "self-tagged" )
	@TestDescription( title = "Self polling", text = "baseline available", configurations = { "Set tag = true" } )
	public void testTagged() throws Exception {
		jenkins.makeTagType( ccenv.getPVob() );
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), new PollSelfMode("INITIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, true, false, false );		
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.SUCCESS )
		.validateBuiltBaseline( PromotionLevel.BUILT, baseline, false )
		.validate();
	}
 
       
    @Test
    @ClearCaseUniqueVobName(name="self-newest-initial")
    @TestDescription(title = "Self polling", text="baseline available using newest") 
    public void testNewest() throws Exception {	
        PollSelfMode mode = new PollSelfMode("INITIAL");
        mode.setNewest(true);        
        
		AbstractBuild<?, ?> build = jenkins.initiateBuild( ccenv.getUniqueName(), mode, "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false );				
		Baseline baseline = ccenv.context.baselines.get( "client-3" );
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.SUCCESS )
		.validateBuiltBaseline( PromotionLevel.BUILT, baseline, false )
		.validate();        
    }
}
