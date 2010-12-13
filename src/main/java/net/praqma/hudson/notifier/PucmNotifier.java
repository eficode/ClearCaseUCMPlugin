package net.praqma.hudson.notifier;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Tag;
import net.praqma.utils.Debug;
import net.praqma.hudson.exception.NotifierException;
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
		hudsonOut.println("------------------------------------------------------------\nPraqmatic UCM - Post build section started\n------------------------------------------------------------\n");
		
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
			if( scm.doPostbuild() )
			{
				baseline = scm.getBaseline();
				st = scm.getStreamObject();
				if ( baseline == null )
				{
					// If baseline is null, the user has already been notified in
					// Console output from PucmScm.checkout()
					result = false;
				}
			} else
			{
				hudsonOut.println( "Error in SCM section - not performing any post build actions." );
				result = false;
			}
		}

		if( result )
		{
			try
			{
			processBuild(build);
			}catch (NotifierException ne){
				hudsonOut.println(ne.getMessage());
			}
		}
		
		logger.print_trace();
		hudsonOut.println("------------------------------------------------------------\nPraqmatic UCM - Post build section finished\n------------------------------------------------------------\n");
		return result;
	}
	
	private void processBuild( AbstractBuild build )throws NotifierException
	{
		String buildstatus = null;
		// Getting tag to set buildstatus
		Tag tag = baseline.GetTag( build.getParent().getDisplayName(), Integer.toString(build.getNumber()) );

		Result buildResult = build.getResult();
		hudsonOut.println("Buildresult: " + buildResult);

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
						throw new NotifierException("Could not promote baseline. " + e.getMessage());
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
						throw new NotifierException(  "Could not recommend baseline. " + e.getMessage() );
					}
				}
			}
			catch ( Exception e )
			{
				throw new NotifierException( "New plevel could not be set." );
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
						throw new NotifierException( "Could not demote baseline. " + e.getMessage());
					}
			}
			catch ( Exception e )
			{
				throw new NotifierException(  "New plevel could not be set. " + e.getMessage());
			}
		}
		else
		{
			logger.log( "Buildstatus (Result) was " + buildResult + ". Not handled by plugin." );
			throw new NotifierException( "Baseline not changed. Buildstatus: " + buildResult );
		}
		persistTag(tag);
		
		//The below hudsonOut are for a little plugin that can display the information on hudsons build-history page.
		hudsonOut.println( "\n\nDISPLAY_STATUS:<small>" + baseline.GetShortname() + "</small><BR/>" + buildResult.toString() + 
				(recommended?"<BR/><B>Recommended</B>":"")+"<BR/><small>Level:[" + baseline.GetPromotionLevel( true ) + "]</small>");
	}
	
	private void persistTag(Tag tag) throws NotifierException
	{
		try
		{
			tag = tag.Persist();
			hudsonOut.println( "Baseline now marked with tag: \n" + tag.Stringify() );
		}
		catch ( Exception e )
		{
			throw new NotifierException(  "Could not change tag in ClearCase. Contact ClearCase administrator to do this manually." );
		}
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
