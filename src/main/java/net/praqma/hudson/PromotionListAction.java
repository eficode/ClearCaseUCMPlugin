/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson;

import hudson.model.InvisibleAction;
import java.util.List;
import net.praqma.clearcase.ucm.entities.Baseline;

/**
 *
 * @author Mads
 */
public class PromotionListAction extends InvisibleAction {
    
    public final List<Baseline> baselines;

    public PromotionListAction(List<Baseline> baselines) {
        this.baselines = baselines;
    }
    
}
