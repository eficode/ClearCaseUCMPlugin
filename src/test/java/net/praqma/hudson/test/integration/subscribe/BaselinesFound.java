/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.test.integration.subscribe;

import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jenkins.model.Jenkins;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.hudson.scm.pollingmode.ComponentSelectionCriteriaRequirement;
import net.praqma.hudson.scm.pollingmode.JobNameRequirement;
import net.praqma.hudson.scm.pollingmode.PollSubscribeMode;
import net.praqma.hudson.test.BaseTestClass;
import static net.praqma.hudson.test.BaseTestClass.jenkins;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.TestDescription;
import org.jenkinsci.plugins.compatibilityaction.CompatibilityDataPlugin;
import org.jenkinsci.plugins.compatibilityaction.MongoProviderImpl;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author Mads
 */
public class BaselinesFound extends BaseTestClass {
	
    @Rule
    public MongoExternalDataSourceRule mongo = new MongoExternalDataSourceRule();
    
    MongoProviderImpl impl = (MongoProviderImpl)mongo.getProvider();
    
    @Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "ccucm" );
    
    @Test
    @ClearCaseUniqueVobName( name = "subscribe-has-data" )
    @TestDescription( title = "Poll subscribe, success in database", text = "baseline available, in database")
    public void newBaselinesFoundTestedInDatabase() throws Exception {    
        jenkins.getInstance().getDescriptorByType(CompatibilityDataPlugin.class).setProvider(impl);
        //Timestamp and view name
        String uname = ccenv.getUniqueName();
        
        
        //Job name is prefixed with CCUCM
        String resultingJobName = "ccucm-"+uname;
        
        //Load the first existing baseline
		Baseline baseline = ccenv.context.baselines.get( "model-1" ).load();
        
        //Assert that the baseline is a composite
        List<Baseline> composites = baseline.getCompositeDependantBaselines();
        Assert.assertFalse(composites.isEmpty());
        
        //Create a result in the database
        
        for(Baseline bls : composites) {
            impl.create(ExternalData.createResult(resultingJobName, Result.SUCCESS, bls));
        }
        
        //Build 1 should be ok. 
        AbstractBuild<?, ?> build1 = initiateBuild(uname, false, false, false, Arrays.asList(resultingJobName), composites );        
        SystemValidator validator = new SystemValidator( build1 )
		.validateBuild( Result.SUCCESS )
		.validateBuiltBaseline( Project.PromotionLevel.BUILT, baseline, false )
		.validate();
    }
    
    @Test
    @ClearCaseUniqueVobName( name = "subscribe-has-data-fail" )
    @TestDescription( title = "Poll subscribe, failed in database", text = "baseline available, in database" )
    public void newBaselinesFoundTestedFailedInDatabase() throws Exception {    
        jenkins.getInstance().getDescriptorByType(CompatibilityDataPlugin.class).setProvider(impl);
        //Timestamp and view name
        String uname = ccenv.getUniqueName();        
        
        //Job name is prefixed with CCUCM
        String resultingJobName = "ccucm-"+uname;
        
        //Load the first existing baseline
		Baseline baseline = ccenv.context.baselines.get( "model-1" ).load();
        
        //Assert that the baseline is a composite
        List<Baseline> composites = baseline.getCompositeDependantBaselines();
        Assert.assertFalse(composites.isEmpty());
        
        //Create a result in the database        
        for(Baseline bls : composites) {
            impl.create(ExternalData.createResult(resultingJobName, Result.FAILURE, bls));
        }
        
        //Build 1 should be ok. 
        AbstractBuild<?, ?> build1 = initiateBuild(uname, false, false, false, Arrays.asList(resultingJobName), composites );        
        SystemValidator validator = new SystemValidator( build1 )
		.validateBuild( Result.FAILURE )
		.validateBuiltBaseline( Project.PromotionLevel.REJECTED, baseline, false )
		.validate();
    }
    
    @Test
    @ClearCaseUniqueVobName( name = "subscribe-has-data-njob" )
    @TestDescription( title = "Poll subscribe, failed in database", text = "baseline available, in database" )
    public void newBaselinesFoundTestedInDatabaseJobNotTested() throws Exception {    
        jenkins.getInstance().getDescriptorByType(CompatibilityDataPlugin.class).setProvider(impl);
        //Timestamp and view name
        String uname = ccenv.getUniqueName();        
        
        //Job name is prefixed with CCUCM
        String resultingJobName = "ccucm-"+uname;
        
        //Load the first existing baseline
		Baseline baseline = ccenv.context.baselines.get( "model-1" ).load();
        
        //Assert that the baseline is a composite
        List<Baseline> composites = baseline.getCompositeDependantBaselines();
        Assert.assertFalse(composites.isEmpty());
        
        //Create a result in the database
        
        for(Baseline bls : composites) {
            impl.create(ExternalData.createResult(resultingJobName, Result.SUCCESS, bls));
        }
        
        //Build 1 should be ok. But it should be NOT BUILT since the job 'not-there' doesn't have any results in the database. 
        AbstractBuild<?, ?> build1 = initiateBuild(uname, false, false, false, Arrays.asList(resultingJobName,"not-there"), composites );        
        SystemValidator validator = new SystemValidator( build1 )
		.validateBuild( Result.NOT_BUILT )	
		.validate();
    } 

    /*
    @Test
    @ClearCaseUniqueVobName( name = "subscribe-has-data" )
    @TestDescription( title = "Poll subscribe", text = "baseline available, in database, not part of job config")
    public void newBaselineNotInConfigTestedForcedBuild() throws Exception {
        //Timestamp and view
        String uname = ccenv.getUniqueName();
        
        //Job name is prefixed with ccucm
        String resultingJobName = "ccucm-"+uname;
        
        //
		Baseline baseline = ccenv.context.baselines.get( "model-1" ).load();
        
        MongoProviderImpl impl = (MongoProviderImpl)mongo.getProvider();
        impl.create(ExternalData.createResult(resultingJobName, Result.SUCCESS, baseline));
        
        List<DBObject> list = impl.listAndSort(new BasicDBObject("jobName", resultingJobName), new BasicDBObject("jobName", -1));
        Assert.assertEquals(1, list.size());       
        Assert.assertEquals(resultingJobName, ""+list.get(0).get("jobName"));
        Assert.assertEquals(baseline.getFullyQualifiedName(), ((List<BasicDBObject>)list.get(0).get("configuration")).get(0).get("baseline"));
        System.out.println(list.get(0));
        
        //Build 1 should be ok.
        AbstractBuild<?, ?> build1 = initiateBuild(uname, false, false, false, Arrays.asList(resultingJobName) );        
        SystemValidator validator = new SystemValidator( build1 )
		.validateBuild( Result.SUCCESS )
		.validateBuiltBaseline( Project.PromotionLevel.BUILT, baseline, false )
		.validate();
    }
    */
    public AbstractBuild<?, ?> initiateBuild( String projectName, boolean recommend, boolean tag, boolean description, List<String> jobNames, List<Baseline> others ) throws Exception {
        Baseline baseline = ccenv.context.baselines.get( "model-1" ).load();        
        List<ComponentSelectionCriteriaRequirement> crit = new ArrayList<ComponentSelectionCriteriaRequirement>();
        crit.add(new ComponentSelectionCriteriaRequirement(others.get(0).load().getComponent().load().getFullyQualifiedName()));
        
        List<JobNameRequirement> jobs = new ArrayList<JobNameRequirement>();
        for(String jobname : jobNames) {
            jobs.add(new JobNameRequirement(jobname, null));
        }
        
        PollSubscribeMode mode = new PollSubscribeMode("INITIAL", crit, jobs);
        
		return jenkins.initiateBuild( projectName, mode, "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), recommend, tag, description, false );
	}
    
    
}
