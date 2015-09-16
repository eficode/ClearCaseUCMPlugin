/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.hudson.remoting;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import net.praqma.clearcase.Rebase;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.ucm.entities.Stream;
import org.jenkinsci.remoting.RoleChecker;

/**
 *
 * @author Mads
 */
public class RebaseCompleteTask implements FilePath.FileCallable<Boolean>{
    public final Stream stream;

    public RebaseCompleteTask(Stream stream) {
        this.stream = stream ;
    }

    @Override
    public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        try {
            Rebase.completeRebase(stream);
            return true;
        } catch (CleartoolException ex) {
            throw new IOException("Failed to complete rebase", ex);
        }
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        
    }
}
