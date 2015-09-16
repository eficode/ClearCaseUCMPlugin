/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.remoting;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import net.praqma.clearcase.Rebase;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.exceptions.RebaseException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import org.jenkinsci.remoting.RoleChecker;

/**
 *
 * @author Mads
 */
public class RebaseTask implements FilePath.FileCallable<Boolean> {

    public final Stream stream;
    public final List<Baseline> baselines;
    public final TaskListener listener;
    public final String viewtag;
    public final Boolean complete;    
    
    public RebaseTask(Stream stream, Baseline baseline, TaskListener listener, String viewtag, Boolean complete) {
        this.stream = stream;
        this.baselines = Arrays.asList(baseline);
        this.listener = listener;
        this.viewtag = viewtag;
        this.complete = complete;
    }
    
    public RebaseTask(Stream stream, List<Baseline> baselines, TaskListener listener, String viewtag, Boolean complete) {
        this.stream = stream;
        this.baselines = baselines;
        this.listener = listener;
        this.viewtag = viewtag;
        this.complete = complete;
    }
    
    @Override
    public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        try {            
            if(Rebase.isInProgress(stream)) {
                Rebase.cancelRebase(stream);
            }            
            new Rebase(stream).addBaselines(baselines).setViewTag(viewtag).rebase(complete);            
        } catch (RebaseException ex) {
           ex.printStackTrace(listener.getLogger());
           return false;
        } catch (CleartoolException ex) {
           ex.printStackTrace(listener.getLogger());
           return false;
        }
        return true;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        
    }
    
}
