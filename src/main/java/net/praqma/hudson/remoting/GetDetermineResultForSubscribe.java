/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.remoting;

import hudson.FilePath;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.hudson.scm.pollingmode.JobNameRequirement;
import org.jenkinsci.plugins.compatibilityaction.CompatibilityDataProvider;
import org.jenkinsci.plugins.compatibilityaction.MongoProviderImpl;

/**
 *
 * @author Mads
 */
public class GetDetermineResultForSubscribe implements FilePath.FileCallable<Result> {
    
    private static final Logger logger = Logger.getLogger(GetConsideredBaselinesForSubscribe.class.getName());
    
    public final List<JobNameRequirement> jobsToMonitor;
    public final List<Baseline> bls;
    public final CompatibilityDataProvider provider;
    
    public GetDetermineResultForSubscribe(List<JobNameRequirement> jobsToMonitor, List<Baseline> bls, CompatibilityDataProvider provider) {
        this.jobsToMonitor = jobsToMonitor;
        this.bls = bls;
        this.provider = provider;
    }

    @Override
    public Result invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        if(bls.isEmpty()) {
            return null;
        }
        
        Result r = Result.SUCCESS;
        
        logger.fine( String.format( "Starting to compare baselines with compatability data for subscribe. We have %s baselines", bls.size() ) );        
        logger.fine( String.format( "We need to check the following jobs:"));
        
        for(JobNameRequirement jreq : jobsToMonitor) {
            logger.fine(String.format(" * %s",jreq.getJobName()));
        }
        
        for(Baseline baseline : bls) {            
            for(JobNameRequirement req : jobsToMonitor ) {                
                try {
                    //If the configuration is set to ignore this one. Skip it
                    if(req.extractComponents().contains(baseline.getComponent())) {
                        logger.fine(String.format("Ignoring baseline %s for job %s", baseline.getFullyQualifiedName(), req.getJobName()));
                        continue;
                    }
                    
                    //Get the aggregated result
                    logger.fine( String.format( "Calculating intermediate for job name %s", req.getJobName()) );
                    Result intermediate = req.getJobResult(baseline, (MongoProviderImpl)provider);
                    logger.fine( String.format( "Done calculating intermediate for job name %s", req.getJobName()) );
                    if(intermediate != null) {
                        logger.fine(String.format("Baseline %s found to be %s for job name %s", baseline.getFullyQualifiedName(), intermediate, req.getJobName()));
                        r = r.combine(intermediate);
                    } else {
                        logger.fine(String.format("Baseline %s not found for job name %s", baseline.getFullyQualifiedName(), req.getJobName()));
                        return null;                
                    }
                } catch (Exception ex) {
                    logger.fine("Unable to correctly determine the result of the subscrbe polling");
                    logger.log(Level.SEVERE, "Error in invoke()#"+this.getClass().getName(), ex);
                    throw new IOException(ex);
                }
            }           
        }
        return r;
    }
    
}
