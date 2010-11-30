package net.praqma.hudson.notifier;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
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
 * PucmNotifier perfoms the user-chosen PUCM post-build actions
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
	private Stream st;

	protected static Debug logger = Debug.GetLogger();

	/**
	 * This constructor is used in the inner class <code>DescriptorImpl</code>.
	 * 
	 * @param promote  if <code>true</code>, the baseline will be promoted after the build.
	 * @param recommended if <code>true</code>, the baseline will be marked 'recommended' in ClearCase.
	 */
	public PucmNotifier( boolean promote, boolean recommended )
	{
		logger.trace_function();
		this.promote = promote;
		this.recommended = recommended;
	}

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
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform( AbstractBuild build, Launcher launcer, BuildListener listener ) throws InterruptedException, IOException
	{
		logger.trace_function();
		boolean result = true;
		hudsonOut = listener.getLogger();
				
		SCM scmTemp = null;
		if (result)
		{
			scmTemp = build.getProject().getScm();
			if ( !( scmTemp instanceof PucmScm ) )
			{
				listener.fatalError( "Not a PUCM scm. This Post build action can only be used when polling from ClearCase with PUCM plugin." );
				result = false;
			}
		}
		
		if( result )
		{
			PucmScm scm = (PucmScm) scmTemp;
			baseline = scm.getBaseline();
			st = scm.getStreamObject();
			if ( baseline == null )
			{
				// If baseline is null, the user has already been notified in
				// Console output from PucmScm.checkout()
				result = false;
			}
		}

		if( result )
		{
			hudsonOut.println( "\n* * * Post build actions * * *" );
			processBuild(build);
		}
		
		logger.print_trace();
		return result;
	}
	
	private boolean processBuild( AbstractBuild build )
	{
		boolean result = true;
		String buildstatus = null;
		// Getting tag to change buildstatus
		Tag tag = baseline.GetTag( "hudson", build.getParent().getDisplayName() );

		Result buildResult = build.getResult();

		if ( buildResult.equals( Result.SUCCESS ) )
		{
			hudsonOut.println( "Build successful" );
			try
			{
				tag.SetEntry( "buildstatus", "SUCCESS" );
				if ( promote )
				{
					try
					{
						baseline.Promote();
						hudsonOut.println( "Baseline promoted to " + baseline.GetPromotionLevel( true ) + "." );
					}
					catch ( Exception e )
					{
						hudsonOut.println( "Could not promote baseline. " + e.getMessage());
						result = false;
					}
				}
				if ( recommended )
				{
					try
					{
						st.RecommendBaseline( baseline );
						hudsonOut.println( "Baseline " + baseline.GetShortname() + " is now recommended ");
					}
					catch ( Exception e )
					{
						hudsonOut.println( "Could not recommend baseline. " + e.getMessage());
						result = false;
					}
				}
			}
			catch ( Exception e )
			{
				hudsonOut.println( "New plevel could not be set." );
				result = false;
			}
		}
		else if ( buildResult.equals( Result.FAILURE ) )
		{
			hudsonOut.println( "Build failed" );
			try
			{
				tag.SetEntry( "buildstatus", "FAILURE" );
				if ( promote )
					try
					{
						baseline.Demote();
						hudsonOut.println( "Baseline is " + baseline.GetPromotionLevel( true )+"." );
					}
					catch ( Exception e )
					{
						hudsonOut.println( "Could not demote baseline. " + e.getMessage());
						result = false;
					}
			}
			catch ( Exception e )
			{
				hudsonOut.println( "New plevel could not be set. " + e.getMessage());
				result = false;
			}
		}
		else
		{
			hudsonOut.println( "Baseline not changed. Buildstatus: " + buildResult );
			logger.log( "Buildstatus (Result) was " + buildResult + ". Not handled by plugin." );
			result = false;
		}
		if( result )
		{
			result = persistTag(tag);
		}
		return result;
	}
	
	private boolean persistTag(Tag tag)
	{
		boolean result = true;
		try
		{
			tag = tag.Persist();
			hudsonOut.println( "Baseline now marked with tag: \n" + tag.Stringify() );
		}
		catch ( Exception e )
		{
			hudsonOut.println( "Could not change tag in ClearCase. Contact ClearCase administrator to do this manually." );
			result = false;
		}
		return result;
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
			load();
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
			save();
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
