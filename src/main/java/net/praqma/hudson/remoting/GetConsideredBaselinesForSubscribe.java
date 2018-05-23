/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.remoting;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.hudson.scm.pollingmode.ComponentSelectionCriteriaRequirement;
import org.jenkinsci.remoting.RoleChecker;

/**
 *
 * @author Mads
 */
public class GetConsideredBaselinesForSubscribe implements FileCallable<List<Baseline>> {

    private static final Logger logger = Logger.getLogger(GetConsideredBaselinesForSubscribe.class.getName());
    public final Baseline bl;
    public final List<ComponentSelectionCriteriaRequirement> componentsToMonitor;
    
    public GetConsideredBaselinesForSubscribe(Baseline bl, List<ComponentSelectionCriteriaRequirement> componentsToMonitor) {
        this.bl = bl;
        this.componentsToMonitor = componentsToMonitor;
    }
    
    @Override
    public List<Baseline> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        try {
            if(bl == null) {
                logger.fine("Baseline was null, no baselines to select");
                return Collections.EMPTY_LIST;
            }
            
            List<Baseline> consider = bl.getCompositeDependantBaselines();
            logger.fine("Listing composites of the selected baseline for poll subscribe");
            for(Baseline cbl : consider) {
                logger.fine(String.format("* %s", cbl));
            }

            List<Baseline> bls = new ArrayList<Baseline>();
            Set<Component> comps = new HashSet<Component>();

            for(ComponentSelectionCriteriaRequirement req : componentsToMonitor) {
                comps.add(req.toComponent());
            }

            logger.fine("We have the following components for selection criteria");

            for(Component c : comps) {
                logger.fine(String.format("* %s", c));
            }

            for(Baseline blc : consider) {                        
                if(comps.contains(blc.getComponent())) {
                    bls.add(blc);
                }
            } 

            return bls;
        } catch (UnableToInitializeEntityException ex) {
            throw new IOException(ex);
        } catch (CleartoolException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {

    }
}
