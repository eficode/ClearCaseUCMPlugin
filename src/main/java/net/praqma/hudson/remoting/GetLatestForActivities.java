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
import net.praqma.clearcase.exceptions.UnableToLoadEntityException;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.clearcase.ucm.utils.VersionList;

/**
 *
 * @author Mads
 */
public class GetLatestForActivities implements FilePath.FileCallable<Map<Activity, List<Version>>> {
    
    private static final Logger LOG = Logger.getLogger(GetLatestForActivities.class.getName());
    private VersionList list;
    
    public GetLatestForActivities(VersionList list) {
        this.list = list;
    }

    @Override
    public Map<Activity, List<Version>> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        Map<Activity, List<Version>> activities = list.getLatestForActivities();
        for(Activity a : activities.keySet()) {
            try {
                a.load();
            } catch (UnableToLoadEntityException ex) {
                LOG.severe("Could not autoload actitity "+a);
            }
        }
        return activities;
    }

    /**
     * @return the list
     */
    public VersionList getList() {
        return list;
    }

    /**
     * @param list the list to set
     */
    public void setList(VersionList list) {
        this.list = list;
    }
  
}
