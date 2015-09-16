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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.praqma.clearcase.Cool;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.clearcase.ucm.utils.ReadOnlyVersionFilter;
import net.praqma.clearcase.ucm.utils.VersionList;
import net.praqma.hudson.Util;
import org.jenkinsci.remoting.RoleChecker;

/**
 *
 * @author Mads
 */
public class CreateChangeSetRemote implements FilePath.FileCallable<String>{
    
    private static final Logger logger = Logger.getLogger(CreateChangeSetRemote.class.getName());
    
    public final List<Activity> activities;
    public final Baseline bl;
    public final boolean trimmed;
    public final File viewRoot;
    public final List<String> readOnly;
    public final boolean ignoreReadOnly;
    
    public CreateChangeSetRemote(List<Activity> activities, Baseline bl, boolean trimmed, File viewRoot, List<String> readonly, boolean ignoreReadOnly) {
        this.activities = activities;
        this.bl = bl;
        this.trimmed = trimmed;
        this.viewRoot = viewRoot;
        this.readOnly = readonly;
        this.ignoreReadOnly = ignoreReadOnly;
    }

    @Override
    public String invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
        logger.fine( String.format("Trim changeset: %s", trimmed));
        Util.ChangeSetGenerator csg = new Util.ChangeSetGenerator().createHeader( bl.getShortname() );

        if( trimmed ) {
            logger.fine("Creating trimmed change set");
            VersionList vl = new VersionList().addActivities( activities ).setBranchName( "^.*" + Cool.qfs + bl.getStream().getShortname() + ".*$" );
            
            if(ignoreReadOnly) {
                vl = vl.addFilter(new ReadOnlyVersionFilter(viewRoot, readOnly)).apply();                
            }
                       
            Map<Activity, List<Version>> acts = vl.getLatestForActivities();
 
            for( Activity activity : acts.keySet() ) {
                csg.addAcitivity( activity.getShortname(), activity.getHeadline(), activity.getUser(), acts.get( activity ) );
            }
        } else {
            logger.fine("Creating non-trimmed changeset");
            for( Activity activity : activities ) {
                VersionList versions = new VersionList( activity.changeset.versions, activities ).getLatest();
                if(ignoreReadOnly) {
                    versions = versions.addFilter(new ReadOnlyVersionFilter(viewRoot, readOnly)).apply();                
                }
                csg.addAcitivity( activity.getShortname(), activity.getHeadline(), activity.getUser(), versions );
            }
        }

        return csg.close().get();
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        
    }
    
}
