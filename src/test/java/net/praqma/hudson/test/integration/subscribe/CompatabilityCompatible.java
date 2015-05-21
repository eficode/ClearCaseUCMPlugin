/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.test.integration.subscribe;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * Simple marker interface that tells that the class is meant to be stored in our compatability database
 * 
 * @author Mads
 */
public interface CompatabilityCompatible extends Serializable {    
    public Date getRegistrationDate();
    public void setRegistrationDate(Date registrationDate);    
}
