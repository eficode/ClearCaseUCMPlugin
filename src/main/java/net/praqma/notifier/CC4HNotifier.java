package net.praqma.notifier;

import java.io.IOException;

import org.kohsuke.stapler.StaplerRequest;
import net.praqma.clearcase.objects.Baseline;
import net.praqma.debug.Debug;
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
import net.praqma.scm.CC4HClass;
import net.sf.json.JSONObject;

/**
 * CC4HNotifier has the resposibility of 'cleaning up' in ClearCase after a build.
 * 
 * @author Troels Selch Sørensen
 * @author Margit
 *
 */
public class CC4HNotifier extends Notifier {
	private boolean promote;
	private boolean recommended;
	private Baseline baseline;
	
	protected static Debug logger = Debug.GetLogger();
	
	/**
	 * This constructor is used in the inner class <code>DescriptorImpl</code>.
	 * 
	 * @param promote if <code>promote</code> is <code>true</code>, the baseline will be promoted after the build.
	 * @param recommended if <code>recommended</code> is <code>true</code>, the baseline will be marked 'recommended' in ClearCase.
	 */
	public CC4HNotifier(boolean promote, boolean recommended){
		this.promote = promote;
		this.recommended = recommended;
		
		logger.trace_function();
	}
	
	/**
	 * This message returns <code>true</code> to make sure that Hudson runs {@link <public boolean perform(AbstractBuild build, Launcher launcer, BuildListener listener)throws InterruptedException, IOException> [perform()]} after a build.
	 */
	@Override
	public boolean needsToRunAfterFinalized(){
		logger.trace_function();
		return true;
	}

	//@Override
	public BuildStepMonitor getRequiredMonitorService() {
		logger.trace_function();
		//TODO Check to see when BUILD should be returned and when not.
		return BuildStepMonitor.BUILD;//we use BUILD because cleacase-plugin does(clearcase/ucm/UcmMakeBaseline
	}
	
	/**
	 * This message is called from Hudson when a build is done, but only if {@link <public boolean needsToRunAfterFinalized()> [needsToRunAfterFinalized()]} returns <code>true</code>.
	 */
	//@SuppressWarnings("unchecked")
	@Override
	public boolean perform(AbstractBuild build, Launcher launcer, BuildListener listener)throws InterruptedException, IOException {
		logger.trace_function();
		boolean res = false;

		SCM scmTemp = build.getProject().getScm();
		if (!(scmTemp instanceof CC4HClass)){
			listener.fatalError("Not a CC4H scm. This Extension can only be used when polling from ClearCase with CC4H plugin.");
			//Needs to return false right away to avoid errors
			return false;
		}

		CC4HClass scm = (CC4HClass)scmTemp;
		baseline = scm.getBaseline();
		baseline.UnMarkBuildInProgess(); //Unmark as buildInProgress (not relevant when working with Tag).
		
		Result result = build.getResult();
		if (result.equals(Result.SUCCESS)){
			
			//TODO: Should Tag also keep track of build-status?
			baseline.Promote(); 
			logger.log("Baseline promoted to next level.");			
			res = true;
		} else if (result.equals(Result.FAILURE)){
			baseline.Demote();
			logger.log("Baseline demoted to rejected.");
			res = true;
		} else {
			logger.log("Result was " + result + ". Not handled by plugin.");
			res = false;
		}
		//TODO: set Tag on Baseline
		logger.log("baseline plevel after build: "+baseline.GetPlevel());
		return res;
	}
	
	public boolean isPromote(){
		return promote;
	}
	
	public boolean isRecommended(){
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
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		public DescriptorImpl(){
			super(CC4HNotifier.class);
		}
		
		@Override
		public String getDisplayName() {
			return "ClearCase 4 Hudson";			
		}

		/**
		 * Hudson uses this method to create a new instance of <code>CC4HNotifier</code>.
		 * The method gets information from Hudson config page.
		 * This information is about the configuration, which Hudson saves.
		 */
        @Override
        public Notifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        	boolean promote = req.getParameter("CC4H.promote")!=null;
        	boolean recommended = req.getParameter("CC4H.recommended")!=null;
        	logger.log("booleans: promote="+promote+" | recommended="+recommended);
            return new CC4HNotifier(promote,recommended);
        }

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> arg0) {
			return true;
		}
		
		@Override
		public String getHelpFile() {
			return "/plugin/CC4H/notifier/help.html";
		}
	}
}
