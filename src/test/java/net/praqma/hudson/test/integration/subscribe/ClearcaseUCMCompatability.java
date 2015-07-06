/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.test.integration.subscribe;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import org.mongojack.ObjectId;

/**
 *
 * @author Mads
 */
public class ClearcaseUCMCompatability implements CompatabilityCompatible {
   
    private String id; 
    
    private String jobName;
    private Date registrationDate;
    private int schemaRevision = 1;
    
    public final String schemaType = "ccucm";
    private boolean compatible;
    
    /**
     * The current configuration. And the components(s) that was flipped on run
     */
    private ClearCaseUCMConfigurationDTO component;
    private ClearCaseUCMConfigurationDTO configuration;
    
    public ClearcaseUCMCompatability() { }
    
    public ClearcaseUCMCompatability(ClearCaseUCMConfigurationDTO component, Date registrationDate, String jobName, boolean compatible, ClearCaseUCMConfigurationDTO configuration) {
        this.registrationDate = registrationDate;
        this.jobName = jobName;
        this.compatible = compatible;
        this.configuration = configuration;
        this.component = component;
    }
    
    @ObjectId
    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    @ObjectId
    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
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
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    /**
     * @return the version
     */
    public ClearCaseUCMConfigurationDTO getComponent() {
        return component;
    }

    /**
     * @param component the version to set
     */
    public void setComponent(ClearCaseUCMConfigurationDTO component) {
        this.component = component;
    }

    /**
     * @return the compatible
     */
    public boolean isCompatible() {
        return compatible;
    }

    /**
     * @param compatible the compatible to set
     */
    public void setCompatible(boolean compatible) {
        this.compatible = compatible;
    }

    /**
     * @return the schemaId
     */
    public int getSchemaRevision() {
        return schemaRevision;
    }

    /**
     * @param schemaRevision the schemaId to set
     */
    public void setSchemaRevision(int schemaRevision) {
        this.schemaRevision = schemaRevision;
    }

    /**
     * @return the configuration
     */
    public ClearCaseUCMConfigurationDTO getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration the configuration to set
     */
    public void setConfiguration(ClearCaseUCMConfigurationDTO configuration) {
        this.configuration = configuration;
    }

    /**
     * @return the schemaType
     */
    public String getSchemaType() {
        return schemaType;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public Date getRegistrationDate() {
        return registrationDate;
    }

    @Override
    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }
    
}
