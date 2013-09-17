package net.praqma.hudson.scm;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;

import java.io.IOException;

/**
 * @author cwolfgang
 */
@Extension
public class Environment extends BuildWrapper {
    /*
    @Override
    public Environment setUp( AbstractBuild build, Launcher launcher, BuildListener listener ) throws IOException, InterruptedException {
        listener.getLogger().println("AH YEAH!");
        Environment env = new Envi
    }
    */
}
