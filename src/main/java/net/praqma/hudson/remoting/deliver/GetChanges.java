package net.praqma.hudson.remoting.deliver;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import net.praqma.clearcase.api.DiffBl;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author cwolfgang
 *
 * @since 1.4.0
 */
public class GetChanges implements FilePath.FileCallable<List<Activity>> {

    private static final Logger logger = Logger.getLogger(GetChanges.class.getName());

    private final BuildListener listener;
    private final Stream destinationStream;
    private final Baseline baseline;
    private final String viewPath;

    public GetChanges( BuildListener listener, Stream destinationStream, Baseline baseline, String viewPath ) {
        this.listener = listener;
        this.destinationStream = destinationStream;
        this.baseline = baseline;
        this.viewPath = viewPath;
    }

    @Override
    public List<Activity> invoke( File workspace, VirtualChannel channel ) throws IOException, InterruptedException {
        logger.fine( "Get changeset" );
        try {
            DiffBl diffBl = new DiffBl( baseline, destinationStream ).setVersions( true ).setActivities( true ).setViewRoot( new File( viewPath ) );
            Activity.Parser parser = new Activity.Parser( diffBl ).setActivityUserAsVersionUser( true ).addDirection( Activity.Parser.Direction.RIGHT ).addDirection( Activity.Parser.Direction.RIGHTI );
            List<Activity> activities = parser.parse().getActivities();
            for( Activity activity : activities ) {
                activity.load();
            }
            return activities;
        } catch( Exception e ) {
            throw new IOException( "Error while retrieving changes", e );
        }
    }

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {

    }
}
