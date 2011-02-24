package net.praqma.hudson.scm;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.FilePath.FileCallable;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.utils.BaselineList;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.ucm.view.SnapshotView.COMP;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.ucm.utils.BaselineDiff;
import net.praqma.hudson.Config;
import net.praqma.hudson.exception.ScmException;
import net.praqma.util.debug.Logger;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * CC4HClass is responsible for everything regarding Hudsons connection to
 * ClearCase pre-build. This class defines all the files required by the user.
 * The information can be entered on the config page.
 * 
 * @author Troels Selch Sørensen
 * @author Margit Bennetzen
 * 
 */
public class PucmScm extends SCM
{

	private String levelToPoll;
	private String loadModule;
	private String component;
	private String stream;
	private boolean newest;
	private Baseline bl;
	private List<String> levels = null;
	private List<String> loadModules = null;
	private BaselineList baselines;
	private boolean compRevCalled;
	private StringBuffer pollMsgs = new StringBuffer();
	private Stream integrationstream;
	private Component comp;
	private SnapshotView sv = null;
	private boolean doPostBuild = true;
	private String buildProject;

	protected static Logger logger = Logger.getLogger();

	/**
	 * The constructor is used by Hudson to create the instance of the plugin
	 * needed for a connection to ClearCase. It is annotated with
	 * <code>@DataBoundConstructor</code> to tell Hudson where to put the
	 * information retrieved from the configuration page in the WebUI.
	 * 
	 * @param component
	 *            defines the component needed to find baselines.
	 * @param levelToPoll
	 *            defines the level to poll ClearCase for.
	 * @param loadModule
	 *            tells if we should load all modules or only the ones that are
	 *            modifiable.
	 * @param stream
	 *            defines the stream needed to find baselines.
	 * @param newest
	 *            tells whether we should build only the newest baseline.
	 * @param newerThanRecommended
	 *            tells whether we should look at all baselines or only ones
	 *            newer than the recommended baseline
	 */
	@DataBoundConstructor
	public PucmScm( String component, String levelToPoll, String loadModule, String stream, boolean newest, boolean testing, String buildProject )
	{
		logger.trace_function();
		logger.debug( "PucmSCM constructor" );
		this.component = component;
		this.levelToPoll = levelToPoll;
		this.loadModule = loadModule;
		this.stream = stream;
		this.newest = newest;
		this.buildProject = buildProject;
	}

	@Override
	public boolean checkout( AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile ) throws IOException, InterruptedException
	{
		logger.trace_function();
		logger.debug( "PucmSCM checkout" );
		boolean result = true;
		
		// consoleOutput Printstream is from Hudson, so it can only be accessed
		// here
		PrintStream consoleOutput = listener.getLogger();
		consoleOutput.println( "---------------------------Praqmatic UCM - SCM section started---------------------------\n" );


		String jobname = build.getParent().getDisplayName();

		if ( build.getBuildVariables().get( "pucm_baseline" ) != null )
		{
			Stream integrationstream = null;
			String baselinename = (String) build.getBuildVariables().get( "pucm_baseline" );
			try
			{
				bl = Baseline.GetBaseline( baselinename );
				integrationstream = bl.GetStream();
				consoleOutput.println( "Starting parameterized build with a pucm_baseline.\nUsing baseline: " + baselinename + " from integrationstream " + integrationstream.GetShortname() );
			}
			catch ( UCMException e )
			{
				consoleOutput.println( "Could not find baseline" );
				doPostBuild = false;
				result = false;
			}
		}
		else
		{

			// compRevCalled tells whether we have polled for baselines to build
			// -
			// so if we haven't polled, we do it now
			if ( !compRevCalled )
			{
				try
				{
					baselinesToBuild( component, stream );
				}
				catch ( ScmException e )
				{
					pollMsgs.append( e.getMessage() );
					result = false;
				}
			}

			compRevCalled = false;

			// pollMsgs are set in either compareRemoteRevisionWith() or
			// baselinesToBuild()
			consoleOutput.println( pollMsgs );
			pollMsgs = new StringBuffer();
		}

		if ( result )
		{
			try
			{
				//Dto dto = new Dto(bl);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				//CheckoutTask ct = new CheckoutTask(consoleOutput, jobname, integrationstream,loadModule, dto, buildProject );
				/* Force the Baseline to be loaded */
				bl.Load();
				CheckoutTask ct = new CheckoutTask( listener, jobname, stream, loadModule, bl.GetFQName(), buildProject );
				//doPostBuild = workspace.act( ct );
				String test = workspace.act( ct );
				
				/* Write change log */
				FileOutputStream fos = new FileOutputStream( changelogFile );
				fos.write( test.getBytes() );
				fos.close();
				
				doPostBuild = test.length() > 0 ? true : false;
				
				//makeWorkspace( consoleOutput, workspace, jobname );
				
				/*
				BaselineDiff changes = ct.getBaselineDiffs();
				*/
				//consoleOutput.println( bldiff.size() + " elements changed" );

				//writeChangelog( changelogFile, bldiff, consoleOutput );
				/*
				doPostBuild = true;
				*/
			}
			catch ( Exception e )
			{
				consoleOutput.println( "An error occured: " + e.getMessage() );
				doPostBuild = false;
				result = false;
			}
		}

		consoleOutput.println( "---------------------------Praqmatic UCM - SCM section finished---------------------------\n" );

		return result;
	}

	private void writeChangelog( File changelogFile, BaselineDiff changes, PrintStream hudsonOut ) throws ScmException
	{
		logger.trace_function();

		StringBuffer buffer = new StringBuffer();

		// Here the .hudson/jobs/[project
		// name]/builds/[buildnumber]/changelog.xml is written
		hudsonOut.print( "Writing Hudson changelog..." );
		try
		{
			buffer.append( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
			buffer.append( "<changelog>" );
			buffer.append( "<changeset>" );
			buffer.append( "<entry>" );
			buffer.append( ( "<blName>" + bl.GetShortname() + "</blName>" ) );
			for ( Activity act : changes )
			{
				buffer.append( "<activity>" );
				buffer.append( ( "<actName>" + act.GetShortname() + "</actName>" ) );
				buffer.append( ( "<author>" + act.GetUser() + "</author>" ) );
				List<Version> versions = act.changeset.versions;
				String temp;
				for ( Version v : versions )
				{
					temp = "<file>" + v.GetSFile() + "[" + v.GetRevision() + "] user: " + v.Blame() + "</file>";
					buffer.append( temp );
				}
				buffer.append( "</activity>" );
			}
			buffer.append( "</entry>" );
			buffer.append( "</changeset>" );

			buffer.append( "</changelog>" );
			FileOutputStream fos = new FileOutputStream( changelogFile );
			fos.write( buffer.toString().getBytes() );
			fos.close();
			hudsonOut.println( " DONE" );
		}
		catch ( Exception e )
		{
			hudsonOut.println( "FAILED" );
			logger.log( "Changelog failed with " + e.getMessage() );
			throw new ScmException( "Changelog failed with " + e.getMessage() );
		}
	}

	@Override
	public ChangeLogParser createChangeLogParser()
	{
		logger.trace_function();
		return new ChangeLogParserImpl();
	}
	
	//private static Map<>

	@Override
	public PollingResult compareRemoteRevisionWith( AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline ) throws IOException, InterruptedException
	{
		logger.trace_function();
		logger.debug( "PucmSCM Pollingresult" );
		
		PucmScm scm = (PucmScm)project.getScm();
		

		PollingResult p;
		try
		{
			baselinesToBuild( component, stream );
			compRevCalled = true;
			p = PollingResult.BUILD_NOW;
		}
		catch ( ScmException e )
		{
			p = PollingResult.NO_CHANGES;
			PrintStream consoleOut = listener.getLogger();
			consoleOut.println( pollMsgs + "\n" + e.getMessage() );
			pollMsgs = new StringBuffer();
		}

		return p;
	}
	

	@Override
	public SCMRevisionState calcRevisionsFromBuild( AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener ) throws IOException, InterruptedException
	{
		logger.trace_function();
		// PrintStream hudsonOut = listener.getLogger();
		SCMRevisionStateImpl scmRS = null;

		if ( !( bl == null ) )
		{
			scmRS = new SCMRevisionStateImpl();
		}
		return scmRS;
	}

	private void baselinesToBuild( String component, String stream ) throws ScmException
	{
		logger.trace_function();

		comp = null;
		integrationstream = null;

		try
		{
			comp = UCMEntity.GetComponent( component, false );
		}
		catch ( UCMException e )
		{
			throw new ScmException( "Could not get component. " + e.getMessage() );
		}

		try
		{
			integrationstream = UCMEntity.GetStream( stream, false );
		}
		catch ( UCMException e )
		{
			throw new ScmException( "Could not get stream. " + e.getMessage() );
		}

		try
		{
			pollMsgs.append( "Getting all baselines for :\n* Stream:         " );
			pollMsgs.append( stream );
			pollMsgs.append( "\n* Component:      " );
			pollMsgs.append( component );
			pollMsgs.append( "\n* Promotionlevel: " );
			pollMsgs.append( levelToPoll );
			pollMsgs.append( "\n" );

			baselines = comp.GetBaselines( integrationstream, Project.Plevel.valueOf( levelToPoll ) );
		}
		catch ( UCMException e )
		{
			throw new ScmException( "Could not retrieve baselines from repository. " + e.getMessage() );
		}

		if ( baselines.size() > 0 )
		{
			printBaselines( baselines );

			try
			{
				List<Baseline> baselinelist = integrationstream.GetRecommendedBaselines();
				pollMsgs.append( "\nRecommended baseline(s): \n" );
				for ( Baseline b : baselinelist )
				{
					pollMsgs.append( b.GetShortname() + "\n" );
				}

				if ( newest )
				{
					bl = baselines.get( baselines.size() - 1 );
					pollMsgs.append( "\nBuilding newest baseline: " );
					pollMsgs.append( bl );
					pollMsgs.append( "\n" );
				}
				else
				{
					bl = baselines.get( 0 );
					pollMsgs.append( "\nBuilding next baseline: " );
					pollMsgs.append( bl );
					pollMsgs.append( "\n" );
				}
			}
			catch ( UCMException e )
			{
				throw new ScmException( "Could not get recommended baselines. " + e.getMessage() );
			}

		}
		else
		{
			throw new ScmException( "No baselines on chosen parameters." );
		}

	}

	private void printBaselines( BaselineList baselines )
	{
		pollMsgs.append( "--------------------\nRetrieved baselines:\n--------------------\n" );
		if ( !( baselines.size() > 20 ) )
		{
			for ( Baseline b : baselines )
			{
				pollMsgs.append( b.GetShortname() );
				pollMsgs.append( "\n" );
			}
		}
		else
		{
			int i = baselines.size();
			pollMsgs.append( "There are " + i + " baselines - only printing first and last three\n" );
			pollMsgs.append( baselines.get( 0 ).GetShortname() + "\n" );
			pollMsgs.append( baselines.get( 1 ).GetShortname() + "\n" );
			pollMsgs.append( baselines.get( 2 ).GetShortname() + "\n" );
			pollMsgs.append( "...\n" );
			pollMsgs.append( baselines.get( i - 3 ).GetShortname() + "\n" );
			pollMsgs.append( baselines.get( i - 2 ).GetShortname() + "\n" );
			pollMsgs.append( baselines.get( i - 1 ).GetShortname() + "\n" );
		}
	}

	/*
	 * The following getters and booleans (six in all) are used to display saved
	 * userdata in Hudsons gui
	 */

	public String getLevelToPoll()
	{
		logger.trace_function();
		return levelToPoll;
	}

	public String getComponent()
	{
		logger.trace_function();
		return component;
	}

	public String getStream()
	{
		logger.trace_function();
		return stream;
	}

	public String getLoadModule()
	{
		logger.trace_function();
		return loadModule;
	}

	public boolean isNewest()
	{
		logger.trace_function();
		return newest;
	}

	/*
	 * getStreamObject() and getBaseline() are used by PucmNotifier to get the
	 * Baseline and Stream in use
	 */

	public Stream getStreamObject()
	{
		logger.trace_function();
		return integrationstream;
	}

	public Baseline getBaseline()
	{
		logger.trace_function();
		return bl;
	}

	public boolean doPostbuild()
	{
		logger.trace_function();
		return doPostBuild;
	}

	public String getBuildProject()
	{
		logger.trace_function();
		return buildProject;
	}

	/**
	 * This class is used to describe the plugin to Hudson
	 * 
	 * @author Troels Selch Sørensen
	 * @author Margit Bennetzen
	 * 
	 */
	@Extension
	public static class PucmScmDescriptor extends SCMDescriptor<PucmScm>
	{

		private String cleartool;
		private List<String> loadModules;

		public PucmScmDescriptor()
		{
			super( PucmScm.class, null );
			logger.trace_function();
			loadModules = getLoadModules();
			load();
			Config.setContext();
		}

		/**
		 * This method is called, when the user saves the global Hudson
		 * configuration.
		 */
		@Override
		public boolean configure( org.kohsuke.stapler.StaplerRequest req, JSONObject json ) throws FormException
		{
			logger.trace_function();
			cleartool = req.getParameter( "PUCM.cleartool" ).trim();
			save();
			return true;
		}

		/**
		 * This is called by Hudson to discover the plugin name
		 */
		@Override
		public String getDisplayName()
		{
			logger.trace_function();
			return "Praqmatic UCM";
		}

		/**
		 * This method is called by the scm/Pucm/global.jelly to validate the
		 * input without reloading the global configuration page
		 * 
		 * @param value
		 * @return
		 */
		public FormValidation doExecutableCheck( @QueryParameter String value )
		{
			logger.trace_function();
			return FormValidation.validateExecutable( value );
		}

		/**
		 * Called by Hudson. If the user does not input a command for Hudson to
		 * use when polling, default value is returned
		 * 
		 * @return
		 */
		public String getCleartool()
		{
			logger.trace_function();
			if ( cleartool == null || cleartool.equals( "" ) )
			{
				return "cleartool";
			}
			return cleartool;
		}

		/**
		 * Used by Hudson to display a list of valid promotion levels to build
		 * from. The list of promotion levels is hard coded in
		 * net.praqma.hudson.Config.java
		 * 
		 * @return
		 */
		public List<String> getLevels()
		{
			logger.trace_function();
			return Config.getLevels();
		}

		/**
		 * Used by Hudson to display a list of loadModules (whether to poll all
		 * or only modifiable elements
		 * 
		 * @return
		 */
		public List<String> getLoadModules()
		{
			logger.trace_function();
			loadModules = new ArrayList<String>();
			loadModules.add( "All" );
			loadModules.add( "Modifiable" );
			return loadModules;
		}

	}
}
