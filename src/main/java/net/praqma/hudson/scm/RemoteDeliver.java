package net.praqma.hudson.scm;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.hudson.exception.ScmException;
import net.praqma.util.debug.PraqmaLogger.Logger;

/**
 *
 * @author wolfgang
 *
 */
class RemoteDeliver implements FileCallable<Integer> {

    private static final long serialVersionUID = 1L;
    private String jobName;
    private String baseline;
    private String stream;
    private BuildListener listener;
    private String id = "";
    private SnapshotView snapview;

    /*
     * private boolean apply4level; private String alternateTarget; private
     * String baselineName;
     */
    private String component;
    private String loadModule;
    // UCMDeliver ucmDeliver = null;
    private Logger logger = null;
    private PrintStream hudsonOut = null;

    public RemoteDeliver(String stream, BuildListener listener,
            /* Common values */
            String component, String loadModule, String baseline, String jobName) {
        this.jobName = jobName;

        this.baseline = baseline;
        this.stream = stream;

        this.listener = listener;

        this.component = component;
        this.loadModule = loadModule;

        // this.ucmDeliver = ucmDeliver;
    }

    public Integer invoke(File workspace, VirtualChannel channel) throws IOException {
        
        hudsonOut = listener.getLogger();
        
        hudsonOut.print( "[PUCM] Setting up remote. " );

        //TODO this should not be necessary cause its done in the Config.java file ????.
        UCM.setContext(UCM.ContextType.CLEARTOOL);

        hudsonOut.print( "Baseline " );
        
        /* Create the baseline object */
        Baseline baseline = null;
        try {
            baseline = UCMEntity.getBaseline(this.baseline);
        } catch (UCMException e) {
            if (e.stdout != null) {
                hudsonOut.println(e.stdout);
            }
            throw new IOException("[PUCM] Could not create Baseline object: " + e.getMessage());
        }
        
        hudsonOut.print( "done. Stream " );

        /* Create the development stream object */
        /* Append vob to dev stream */

        Stream stream = null;
        try {
            stream = UCMEntity.getStream(this.stream);
        } catch (UCMException e) {
            if (e.stdout != null) {
                hudsonOut.println(e.stdout);
            }
            throw new IOException("[PUCM] Could not create Stream object: " + e.getMessage());
        }
        
        hudsonOut.print( "done. Component " );

        /* Create the component object */
        Component component = null;
        try {
            component = UCMEntity.getComponent(this.component);
        } catch (UCMException e) {
            if (e.stdout != null) {
                hudsonOut.println(e.stdout);
            }
            throw new IOException("[PUCM] Could not create Component object: " + e.getMessage());
        }
        
        hudsonOut.print( "done. Target " );

        /* Get the target Stream */
        Stream target = null;
        try {
            target = stream.getDefaultTarget();
        } catch (UCMException e) {
            if (e.stdout != null) {
                hudsonOut.println(e.stdout);
            }
            throw new IOException("[PUCM] The Stream did not have a default target: " + e.getMessage());
        }
        
        hudsonOut.println( "done. View " );
        
        hudsonOut.println( "STREAM = " + stream );

        /* Make deliver view */
        try {
            snapview = makeDeliverView(target, workspace);
            baseline.deliver(baseline.getStream(), stream, snapview.GetViewRoot(), snapview.GetViewtag(), true, false, true);
            //baseline.promote();
        } catch (UCMException e) {
            throw new IOException(e.getMessage());
        } catch (ScmException e) {
            throw new IOException("Could not create deliver view: " + e.getMessage());
        }
        
        hudsonOut.println( "done. " );

        /* End of deliver */
        return 1;
    }

    private SnapshotView makeDeliverView(Stream stream, File workspace) throws ScmException {
        /* Replace evil characters with less evil characters */
        String newJobName = jobName.replaceAll("\\s", "_");

        String viewtag = newJobName + "_" + System.getenv("COMPUTERNAME") + "_" + stream.getShortname();
        hudsonOut.println("[PUCM] Trying to make deliver view " + viewtag);

        File viewroot = new File(workspace.getPath() + File.separator + "view");

        hudsonOut.println("[PUCM] viewtag: " + viewtag);

        try {
            if (viewroot.exists()) {
                hudsonOut.println("[PUCM] Reusing viewroot: " + viewroot.toString());
            } else {
                if (viewroot.mkdir()) {
                    hudsonOut.println("[PUCM] Created folder for viewroot:  " + viewroot.toString());
                } else {
                    throw new ScmException("Could not create folder for viewroot:  " + viewroot.toString());
                }
            }
        } catch (Exception e) {
            throw new ScmException("Could not make workspace (for viewroot " + viewroot.toString() + "). Cause: " + e.getMessage());

        }

        if (UCMView.ViewExists(viewtag)) {
            hudsonOut.println("[PUCM] Reusing viewtag: " + viewtag + "\n");
            try {
                SnapshotView.ViewrootIsValid(viewroot);
                hudsonOut.println("[PUCM] Viewroot is valid in ClearCase");
            } catch (UCMException ucmE) {
                try {
                    hudsonOut.println("[PUCM] Viewroot not valid - now regenerating.... ");
                    SnapshotView.RegenerateViewDotDat(viewroot, viewtag);
                } catch (UCMException ucmEx) {
                    if (ucmEx.stdout != null) {
                        hudsonOut.println(ucmEx.stdout);
                    }
                    throw new ScmException("Could not make workspace - could not regenerate view: " + ucmEx.getMessage() + " Type: " + "");
                }
            }

            hudsonOut.println("[PUCM] Getting snapshotview...");
            try {
                snapview = UCMView.GetSnapshotView(viewroot);
            } catch (UCMException e) {
                if (e.stdout != null) {
                    hudsonOut.println(e.stdout);
                }
                throw new ScmException("Could not get view for workspace. " + e.getMessage());
            }
        } else {
            try {
                snapview = SnapshotView.Create(stream, viewroot, viewtag);

                hudsonOut.println("[PUCM] View doesn't exist. Created new view in local workspace: " + viewroot.getAbsolutePath());
            } catch (UCMException e) {
                if (e.stdout != null) {
                    hudsonOut.println(e.stdout);
                }
                throw new ScmException("View not found in this region, but view with viewtag '" + viewtag
                        + "' might exists in the other regions. Try changing the region Hudson or the slave runs in.");
            }
        }

        try {
            hudsonOut.println("[PUCM] Updating deliver view using " + loadModule.toLowerCase() + " modules...");
            snapview.Update(true, true, true, false, null, loadModule);
        } catch (UCMException e) {
            if (e.stdout != null) {
                hudsonOut.println(e.stdout);
            }
            throw new ScmException("Could not update snapshot view. " + e.getMessage());
        }

        return snapview;
    }

    public SnapshotView getSnapShotView() {
        return this.snapview;
    }
}