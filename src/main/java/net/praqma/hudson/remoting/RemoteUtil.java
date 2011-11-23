package net.praqma.hudson.remoting;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.entities.Project.Plevel;
import net.praqma.hudson.exception.CCUCMException;
import net.praqma.hudson.scm.CCUCMState.State;
import net.praqma.util.debug.Logger;
import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.remoting.Future;
import hudson.remoting.Pipe;

public abstract class RemoteUtil {

    private static Logger logger = Logger.getLogger();

    public static void completeRemoteDeliver(FilePath workspace, BuildListener listener, State state, boolean complete) throws CCUCMException {

        try {
            if (workspace.isRemote()) {
                final Pipe pipe = Pipe.createRemoteToLocal();
                Future<Boolean> i = null;
                i = workspace.actAsync(new RemoteDeliverComplete(state.getBaseline(), state.getStream(), state.getSnapView(), state.getChangeset(), complete, listener, pipe, Logger.getSubscriptions()));
                logger.redirect(pipe.getIn());
                i.get();
            } else {
                Future<Boolean> i = null;
                i = workspace.actAsync(new RemoteDeliverComplete(state.getBaseline(), state.getStream(), state.getSnapView(), state.getChangeset(), complete, listener, null, Logger.getSubscriptions()));
                i.get();
            }
            return;

        } catch (Exception e) {
            throw new CCUCMException("Failed to " + (complete ? "complete" : "cancel") + " the deliver: " + e.getMessage());
        }
    }

    public static Baseline createRemoteBaseline(FilePath workspace, BuildListener listener, String baseName, Component component, File view, String username) throws CCUCMException {

        try {
            if (workspace.isRemote()) {
                final Pipe pipe = Pipe.createRemoteToLocal();
                Future<Baseline> i = null;
                i = workspace.actAsync(new CreateRemoteBaseline(baseName, component, view, username, listener, pipe, Logger.getSubscriptions()));
                logger.redirect(pipe.getIn());
                return i.get();
            } else {
                Future<Baseline> i = null;
                i = workspace.actAsync(new CreateRemoteBaseline(baseName, component, view, username, listener, null, Logger.getSubscriptions()));
                return i.get();
            }

        } catch (Exception e) {
            throw new CCUCMException(e.getMessage());
        }
    }

    public static List<Baseline> getRemoteBaselinesFromStream(FilePath workspace, Component component, Stream stream, Plevel plevel, boolean slavePolling) throws CCUCMException {

        GetRemoteBaselineFromStream t = new GetRemoteBaselineFromStream(component, stream, plevel, null, Logger.getSubscriptions());

        try {
            if (slavePolling) {
                if (workspace.isRemote()) {
                    final Pipe pipe = Pipe.createRemoteToLocal();
                    Future<List<Baseline>> i = null;
                    i = workspace.actAsync(new GetRemoteBaselineFromStream(component, stream, plevel, pipe, Logger.getSubscriptions()));
                    logger.redirect(pipe.getIn());
                    return i.get();
                } else {
                    Future<List<Baseline>> i = null;
                    i = workspace.actAsync(new GetRemoteBaselineFromStream(component, stream, plevel, null, Logger.getSubscriptions()));
                    return i.get();
                }
            } else {
                return t.invoke(null, null);
            }

        } catch (Exception e) {
            throw new CCUCMException(e.getMessage());
        }
    }

    public static List<Stream> getRelatedStreams(FilePath workspace, TaskListener listener, Stream stream, boolean pollingChildStreams, boolean slavePolling) throws CCUCMException {

        PrintStream out = listener.getLogger();

        try {
            if (slavePolling) {

                if (workspace.isRemote()) {
                    final Pipe pipe = Pipe.createRemoteToLocal();
                    Future<List<Stream>> i = null;
                    i = workspace.actAsync(new GetRelatedStreams(listener, stream, pollingChildStreams, pipe, Logger.getSubscriptions()));
                    logger.redirect(pipe.getIn());
                    return i.get();
                } else {
                    Future<List<Stream>> i = null;
                    i = workspace.actAsync(new GetRelatedStreams(listener, stream, pollingChildStreams, null, Logger.getSubscriptions()));
                    return i.get();

                }
            } else {
                GetRelatedStreams t = new GetRelatedStreams(listener, stream, pollingChildStreams, null, Logger.getSubscriptions());
                return t.invoke(null, null);
            }

        } catch (Exception e) {
            e.printStackTrace(out);
            throw new CCUCMException(e.getMessage());
        }
    }

    public static UCMEntity loadEntity(FilePath workspace, UCMEntity entity, boolean slavePolling) throws CCUCMException {

        try {
            if (slavePolling) {
                Future<UCMEntity> i = null;
                System.out.println( "Starting" );
                if (workspace.isRemote()) {
                    final Pipe pipe = Pipe.createRemoteToLocal();

                    i = workspace.actAsync(new LoadEntity(entity, pipe, Logger.getSubscriptions()));
                    logger.redirect(pipe.getIn());

                } else {
                    i = workspace.actAsync(new LoadEntity(entity, null, Logger.getSubscriptions()));
                }
                System.out.println( "Done" );
                return i.get();
            } else {
                LoadEntity t = new LoadEntity(entity, null, Logger.getSubscriptions());
                return t.invoke(null, null);
            }

        } catch (Exception e) {
            throw new CCUCMException(e.getMessage());
        }
    }

    public static String getClearCaseVersion(FilePath workspace, Project project) throws CCUCMException {

        try {
            Future<String> i = null;

            if (workspace.isRemote()) {
                final Pipe pipe = Pipe.createRemoteToLocal();

                i = workspace.actAsync(new GetClearCaseVersion(project, pipe, Logger.getSubscriptions()));
                logger.redirect(pipe.getIn());

            } else {
                i = workspace.actAsync(new GetClearCaseVersion(project, null, Logger.getSubscriptions()));
            }

            return i.get();

        } catch (Exception e) {
            throw new CCUCMException(e.getMessage());
        }
    }
    
    public static void endView(FilePath workspace, String viewtag) throws CCUCMException {

        try {

            workspace.act( new EndView( viewtag ) );

        } catch (Exception e) {
            throw new CCUCMException(e.getMessage());
        }
    }
}
