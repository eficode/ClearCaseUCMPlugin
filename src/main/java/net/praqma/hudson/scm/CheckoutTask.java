package net.praqma.hudson.scm;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.utils.BaselineDiff;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.Cool;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.ucm.view.SnapshotView.COMP;
import net.praqma.hudson.*;
import net.praqma.hudson.exception.ScmException;
import net.praqma.util.debug.PraqmaLogger;
import net.praqma.util.debug.PraqmaLogger.Logger;
import net.praqma.util.structure.Tuple;

import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

public class CheckoutTask implements FileCallable<Tuple<String, String>> {

	private static final long serialVersionUID = -7029877626574728221L;
	private PrintStream hudsonOut;
	private Stream integrationstream;
	private String jobname;
	private SnapshotView sv;
	private String loadModule;
	private Baseline bl;
	private String buildProject;
	private Logger logger;
	private String intStream;
	private String baselinefqname;
	private BuildListener listener;
	private Integer jobNumber;
	private String id = "";

	private String log = "";

	public CheckoutTask( BuildListener listener, String jobname, Integer jobNumber, String intStream, String loadModule, String baselinefqname, String buildProject, Logger logger ) {
		this.jobname = jobname;
		this.jobNumber = jobNumber;
		this.intStream = intStream;
		this.loadModule = loadModule;
		this.baselinefqname = baselinefqname;
		this.buildProject = buildProject;
		this.listener = listener;
		this.logger = logger;

		this.id = "[" + jobname + "::" + jobNumber + "]";
	}

	@Override
	public Tuple<String, String> invoke( File workspace, VirtualChannel channel ) throws IOException {
		PraqmaLogger.getLogger( logger );
		/* Make sure that the local log file is not written */
		logger.setLocalLog( null );
		Cool.setLogger( logger );
		hudsonOut = listener.getLogger();

		log += logger.info( "Starting CheckoutTask" );

		boolean doPostBuild = true;
		String diff = "";

		try {
			UCM.setContext( UCM.ContextType.CLEARTOOL );
			makeWorkspace( workspace );
			BaselineDiff bldiff = bl.getDifferences( sv );
			diff = Util.createChangelog( bldiff, bl );
			doPostBuild = true;
		} catch (net.praqma.hudson.exception.ScmException e) {
			log += logger.debug( id + "SCM exception: " + e.getMessage() );
			hudsonOut.println( "[PUCM] SCM exception: " + e.getMessage() );
		} catch (UCMException e) {
			log += logger.debug( id + "Could not get changes. " + e.getMessage() );
			log += logger.info( e );
			hudsonOut.println( "[PUCM] Could not get changes. " + e.getMessage() );
		}

		log += logger.info( "CheckoutTask finished normally" );

		return new Tuple<String, String>( diff, log );
	}

	private void makeWorkspace( File workspace ) throws ScmException {
		// We know we have a stream (st), because it is set in
		// baselinesToBuild()
		try {
			integrationstream = UCMEntity.getStream( intStream, false );
			bl = Baseline.getBaseline( baselinefqname );
		} catch (UCMException e) {
			throw new ScmException( "Could not get stream. Job might run on machine with different region. " + e.getMessage() );
		}
		if( workspace != null ) {
			log += logger.debug( id + "workspace: " + workspace.getAbsolutePath() );
		} else {
			log += logger.debug( id + "workspace must be null???" );
		}

		String newJobName = jobname.replaceAll("\\s", "_");
		String viewtag = newJobName + "_" + System.getenv("COMPUTERNAME") + "_" + integrationstream.getShortname();

		File viewroot = new File( workspace, viewtag );
		
		Stream devstream = null;

		devstream = getDeveloperStream( "stream:" + viewtag, Config.getPvob( integrationstream ), hudsonOut );

		sv = Util.makeView( devstream, workspace, listener, loadModule, viewroot, viewtag );
		

		// Now we have to rebase - if a rebase is in progress, the
		// old one must be stopped and the new started instead
		if( devstream.isRebaseInProgress() ) {
			hudsonOut.print( "[PUCM] Cancelling previous rebase..." );
			devstream.cancelRebase();
			hudsonOut.println( " DONE" );
		}
		// The last boolean, complete, must always be true from PUCM
		// as we are always working on a read-only stream according
		// to LAK
		hudsonOut.print( "[PUCM] Rebasing development stream (" + devstream.getShortname() + ") against parent stream (" + integrationstream.getShortname() + ")" );
		devstream.rebase( sv, bl, true );
		hudsonOut.println( " DONE" );
		hudsonOut.println( "[PUCM] Log written to " + logger.getPath() );
	}

	private Stream getDeveloperStream( String streamname, String pvob, PrintStream hudsonOut ) throws ScmException {
		Stream devstream = null;

		try {
			if( Stream.streamExists( streamname + pvob ) ) {
				devstream = Stream.getStream( streamname + pvob, false );
			} else {
				if( buildProject.equals( "" ) ) {
					buildProject = null;
				}
				devstream = Stream.create( Config.getIntegrationStream( bl, buildProject ), streamname + pvob, true, bl );
			}
		}
		/*
		 * This tries to handle the issue where the project hudson is not
		 * available
		 */
		catch (ScmException se) {
			throw se;

		} catch (Exception e) {
			throw new ScmException( "Could not get stream: " + e.getMessage() );
		}

		return devstream;
	}

	public SnapshotView getSnapshotView() {
		return sv;
	}

	public BaselineDiff getBaselineDiffs() throws UCMException {
		return bl.getDifferences( sv );
	}

}
