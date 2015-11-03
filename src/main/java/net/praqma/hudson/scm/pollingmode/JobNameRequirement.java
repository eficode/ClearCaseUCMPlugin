/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.scm.pollingmode;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import hudson.Extension;
import hudson.model.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.hudson.scm.pollingmode.PollSubscribeMode.PollSubscribeDescriptor;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.compatibilityaction.CompatibilityDataException;
import org.jenkinsci.plugins.compatibilityaction.MongoProviderImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author Mads
 */
public class JobNameRequirement extends Requirement {

    private static final Logger logger = Logger.getLogger(JobNameRequirement.class.getName());
    
    private String jobName;
    private String ignores;
    
    @DataBoundConstructor
    public JobNameRequirement(String jobname, String ignores) {
        this.jobName = jobname;
        this.ignores = ignores;
    }
    
    /**
     * @return the jobName
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * @param jobName the jobName to set
     */
    @DataBoundSetter
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
    
    public String getSyntax() {
        return String.format("jobName:%s",jobName);
    }

    /**
     * @return the ignores
     */
    public String getIgnores() {
        return ignores;
    }

    /**
     * @param ignores the ignores to set
     */
    public void setIgnores(String ignores) {
        this.ignores = ignores;
    }
    
    /**
     * Split the component section for this requirement into a list of ClearCase component objects.
     * @return A list of {@link Component}s 
     * @throws UnableToInitializeEntityException Thrown when cleartool reports errors
     */
    public List<Component> extractComponents() throws UnableToInitializeEntityException {
        List<Component> comps = new ArrayList<>();
        if(StringUtils.isBlank(ignores)) {
            return comps;
        }
        
        String trim = ignores.replace(" ", "");
        
        for(String s : trim.split(",")) {
            if(!StringUtils.isBlank(s)) {
                Component c = Component.get(s);
                comps.add(c);
            }
        }
        
        return comps;
    }
    
    /**
     * 
     * @param bl {@link Baseline}
     * @param impl The data provider
     * @return The result of the execution for the given job. Null if the baseline is not in our database of
     * tested configurations
     * @throws CompatibilityDataException Thrown on database error
     * @throws UnableToInitializeEntityException Thrown on cleartool error
     */
    public Result getJobResult(Baseline bl, MongoProviderImpl impl) throws CompatibilityDataException, UnableToInitializeEntityException {
        Result r = null;
              
        logger.fine("Preparing query for subscribe");
        QueryBuilder qb2 = QueryBuilder.start("jobName")
        .is(jobName)
        .and("schemaType").is("ccucm")
        .and("schemaRevision").is(1);
        
        DBObject q = qb2.get();
        
        logger.fine(String.format("Using query: %s", q));
        
        List<DBObject> objects = impl.listAndSort(q, new BasicDBObject("registractionDate", -1));
        
        logger.fine("Done listing for subscribe");
        
        if(!objects.isEmpty()) {
            for(DBObject entry : objects) {
                for(BasicDBObject confComponent : (List<BasicDBObject>)entry.get("configuration")) {
                    String confBaseline = (String)confComponent.get("baseline");
                    if(bl.getFullyQualifiedName().equals(confBaseline)) {
                        r = (Boolean)entry.get("compatible") ? Result.SUCCESS : Result.FAILURE;
                        return r;
                    } 
                }                
            }                
        }
        
        return r;
    }
    
    
    @Extension
    public static class JobNameRequirementDescriptorImpl extends RequirementDescriptor {

        @Override
        public String getDisplayName() {
            return "Job name";
        }
        
        public JobNameRequirementDescriptorImpl() {
            load();
        }
        
        public String getDescriptorForAc() {
            return Jenkins.getInstance().getDescriptorByType(PollSubscribeDescriptor.class).getDescriptorUrl()+"/findCandidates";
        }
        
    }
    
}
