/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.test.integration.subscribe;

import hudson.model.Result;
import java.util.Date;
import net.praqma.clearcase.ucm.entities.Baseline;

/**
 *
 * @author Mads
 */
public class ExternalData {
    
    public static CompatabilityCompatible createResult(String jobName, Result r, Baseline bl) throws Exception {                
        
        ClearCaseUCMConfigurationDTO change = new ClearCaseUCMConfigurationDTO();
        String compname = bl.getComponent().load().getFullyQualifiedName();
        String streamname = bl.getStream() != null ? bl.getStream().load().getFullyQualifiedName() : "unknown";
        String blfqname = bl.getFullyQualifiedName();
        
        ClearCaseUCMConfigurationComponentDTO clearCaseUCMConfigurationComponentDTO = new ClearCaseUCMConfigurationComponentDTO(
                compname,
                streamname,
                blfqname, "INITIAL");
        
        change.add(clearCaseUCMConfigurationComponentDTO);
        
        ClearCaseUCMConfigurationDTO config = new ClearCaseUCMConfigurationDTO();
        config.add(clearCaseUCMConfigurationComponentDTO);
        
        ClearcaseUCMCompatability c = new ClearcaseUCMCompatability(change, new Date() , jobName, r == Result.SUCCESS, config);
        
        
        return c;
    }
}
