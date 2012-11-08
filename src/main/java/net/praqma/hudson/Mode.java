package net.praqma.hudson;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;

import java.io.File;

/**
 * User: cwolfgang
 * Date: 08-11-12
 * Time: 21:23
 */
public abstract class Mode {

    private CCUCMBuildAction action;

    public Mode( CCUCMBuildAction action ) {
        this.action = action;
    }

    /* Polling */
    public abstract PollingResult poll( AbstractProject<?, ?> project, FilePath workspace, TaskListener listener );

    /* Checkout part */
    public abstract void printParameters( TaskListener listener );
    public abstract void resolveBaseline( AbstractBuild<?, ?> build, TaskListener listener );
    public abstract void initializeWorkspace( AbstractBuild<?, ?> build, File changelogFile, TaskListener listener );


    /* Notifier */
    public abstract void completeDeliver( AbstractBuild<?, ?> build, TaskListener listener );
    public abstract void update( AbstractBuild<?, ?> build, TaskListener listener );
    public abstract void setDescription( AbstractBuild<?, ?> build, String message );

    public abstract void doit();

    public static Mode getMode( CCUCMBuildAction action ) {
        return null;
    }
}
