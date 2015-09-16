package net.praqma.hudson.remoting.deliver;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.hudson.Util;
import net.praqma.hudson.exception.ScmException;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.jenkinsci.remoting.RoleChecker;

/**
 * @author cwolfgang
 *
 * @since 1.4.0
 */
public class MakeDeliverView implements FilePath.FileCallable<SnapshotView> {

    private static final Logger logger = Logger.getLogger( MakeDeliverView.class.getName() );;

    private final BuildListener listener;
    private String viewtag;
    private final String jobName;
    private final String loadModule;
    private final Stream destinationStream;

    public MakeDeliverView( BuildListener listener, String jobName, String loadModule, Stream destinationStream ) {
        this.listener = listener;
        this.jobName = jobName;
        this.loadModule = loadModule;
        this.destinationStream = destinationStream;
    }

    @Override
    public SnapshotView invoke( File workspace, VirtualChannel channel ) throws IOException, InterruptedException {        
        logger.fine( "Make deliver view in " + workspace );
        try {
            return makeDeliverView( destinationStream, workspace );
        } catch( Exception e ) {
            throw new IOException( "Error while creating deliver view", e );
        }
    }

    private SnapshotView makeDeliverView( Stream stream, File workspace ) throws ScmException {
        viewtag = Util.createViewTag( jobName, stream );
        File viewroot = new File( workspace, "view" );
        SnapshotView view = Util.makeView( stream, workspace, listener, loadModule, viewroot, viewtag, true );
        return view;
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        
    }
}
