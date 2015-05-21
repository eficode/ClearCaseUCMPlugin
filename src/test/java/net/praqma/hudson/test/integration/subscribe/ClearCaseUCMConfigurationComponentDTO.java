/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.test.integration.subscribe;

import java.io.Serializable;

/**
 * TODO: This cold be put into some sort of library as i basically copied from the config rotator plugin
 * which contribues the information.
 * @author Mads
 */
public class ClearCaseUCMConfigurationComponentDTO implements Serializable {
    
    private String component;
    private String stream;
    private String baseline;
    private String plevel;
    
    public ClearCaseUCMConfigurationComponentDTO() { }
    
    public ClearCaseUCMConfigurationComponentDTO(String component, String stream, String baseline, String plevel)  {
        this.component = component;
        this.stream = stream;
        this.baseline = baseline;
        this.plevel = plevel;
    }

    /**
     * @return the baseline
     */
    public String getBaseline() {
        return baseline;
    }

    /**
     * @param baseline the baseline to set
     */
    public void setBaseline(String baseline) {
        this.baseline = baseline;
    }

    /**
     * @return the plevel
     */
    public String getPlevel() {
        return plevel;
    }

    /**
     * @param plevel the plevel to set
     */
    public void setPlevel(String plevel) {
        this.plevel = plevel;
    }

    /**
     * @return the component
     */
    public String getComponent() {
        return component;
    }

    /**
     * @param component the component to set
     */
    public void setComponent(String component) {
        this.component = component;
    }

    /**
     * @return the stream
     */
    public String getStream() {
        return stream;
    }

    /**
     * @param stream the stream to set
     */
    public void setStream(String stream) {
        this.stream = stream;
    }
    
}
