package net.praqma.hudson.notifier;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Tag;
import net.praqma.utils.Debug;
import net.praqma.hudson.scm.PucmScm;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;

/**
 * PucmNotifier has the responsibility of 'cleaning up' in ClearCase after a
 * build.
 * 
 * @author Troels Selch Sørensen
 * @author Margit Bennetzen
 * 
 */
public class PucmNotifier extends Notifier
{
	private boolean promote;
	private boolean recommended;
	private Baseline baseline;
	private PrintStream hudsonOut;

	protected static Debug logger = Debug.GetLogger();

	/**
	 * This constructor is used in the inner class <code>DescriptorImpl</code>.
	 * 
	 * @param promote
	 *            if <code>promote</code> is <code>true</code>, the baseline
	 *            will be promoted after the build.
	 * @param recommended
	 *            if <code>recommended</code> is <code>true</code>, the baseline
	 *            will be marked 'recommended' in ClearCase.
	 */
	public PucmNotifier( boolean promote, boolean recommended )
	{
		logger.trace_function();
		this.promote = promote;
		this.recommended = recommended;
	}

	/**
	 * This message returns <code>true</code> to make sure that Hudson runs
	 * {@link <public boolean perform(AbstractBuild build, Launcher launcer,
	 * BuildListener listener)throws InterruptedException, IOException>
	 * [perform()]} after a build.
	 */
	@Override
	public boolean needsToRunAfterFinalized()
	{
		logger.trace_function();
		return true;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService()
	{
		logger.trace_function();
		return BuildStepMonitor.NONE;// http://hudson-ci.org/javadoc/hudson/tasks/BuildStep.html#getRequiredMonitorService%28%29
	}

	/**
	 * This message is called from Hudson when a build is done, but only if
	 * {@link <public boolean needsToRunAfterFinalized()>
	 * [needsToRunAfterFinalized()]} returns <code>true</code>.
	 */
	@Override
	public boolean perform( AbstractBuild build, Launcher launcer, BuildListener listener ) throws InterruptedException, IOException
	{
		logger.trace_function();
		boolean res = false;
		hudsonOut = listener.getLogger();
		hudsonOut.println( "\n* * * Post build actions * * *" );

		SCM scmTemp = build.getProject().getScm();
		if ( !( scmTemp instanceof PucmScm ) )
		{
			listener.fatalError( "Not a PUCM scm. This Extension can only be used when polling from ClearCase with PUCM plugin." );
			return false;
		}

		PucmScm scm = (PucmScm) scmTemp;
		baseline = scm.getBaseline();
		if ( baseline == null )
		{
			// If baseline is null, the user has already been notified in
			// Console output from PucmScm.checkout()
			return false;
		}

		// Getting tag to change buildstatus
		Tag tag = baseline.GetTag( "hudson", build.getParent().getDisplayName() );

		Result result = build.getResult();

		if ( result.equals( Result.SUCCESS ) )
		{
			try
			{
				baseline.Promote();
				tag.SetEntry( "buildstatus", "SUCCESS" );
				hudsonOut.println( "Baseline promoted to " + baseline.GetPromotionLevel( true ) );
				res = true;
			}
			catch ( Exception e )
			{
				hudsonOut.println( "Build success, but new plevel could not be set." );
			}
		}
		else
			if ( result.equals( Result.FAILURE ) )
			{
				try
				{
					baseline.Demote();
					tag.SetEntry( "buildstatus", "FAILURE" );
					hudsonOut.println( "Build failed - baseline is " + baseline.GetPromotionLevel( true ) );
					res = true;
				}
				catch ( Exception e )
				{
					hudsonOut.println( "Build failure, but new plevel could not be set." );
				}
			}
			else
			{
				hudsonOut.println( "Baseline not changed. Buildstatus: " + result );
				logger.log( "Buildstatus (Result) was " + result + ". Not handled by plugin." );
				res = false;
			}
		try
		{
			tag = tag.Persist();
			hudsonOut.println( "Baseline now marked with tag:" + tag.Stringify() );
		}
		catch ( Exception e )
		{
			hudsonOut.println( "Could not change tag in ClearCase. Contact ClearCase administrator to do this manually." );
		}
		logger.print_trace();
		return res;
	}

	public boolean isPromote()
	{
		logger.trace_function();
		return promote;
	}

	public boolean isRecommended()
	{
		logger.trace_function();
		return recommended;
	}

	/**
	 * This class is used by Hudson to define the plugin.
	 * 
	 * @author Troels Selch Sørensen
	 * @author Margit
	 * 
	 */
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher>
	{
		public DescriptorImpl()
		{
			super( PucmNotifier.class );
			logger.trace_function();
		}

		@Override
		public String getDisplayName()
		{
			logger.trace_function();
			return "Praqmatic UCM";
		}

		/**
		 * Hudson uses this method to create a new instance of
		 * <code>PucmNotifier</code>. The method gets information from Hudson
		 * config page. This information is about the configuration, which
		 * Hudson saves.
		 */
		@Override
		public Notifier newInstance( StaplerRequest req, JSONObject formData ) throws FormException
		{
			logger.trace_function();
			boolean promote = req.getParameter( "Pucm.promote" ) != null;
			boolean recommended = req.getParameter( "Pucm.recommended" ) != null;
			return new PucmNotifier( promote, recommended );
		}

		@Override
		public boolean isApplicable( Class<? extends AbstractProject> arg0 )
		{
			logger.trace_function();
			return true;
		}

		@Override
		public String getHelpFile()
		{
			logger.trace_function();
			return "/plugin/PucmScm/notifier/help.html";
		}
	}
}
