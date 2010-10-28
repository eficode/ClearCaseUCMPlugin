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

public class CC4HNotifier extends Notifier {
	
	private boolean promote;
	private boolean recommended;
	private Baseline baseline;
	
	protected static Debug logger = Debug.GetLogger();
	
	public CC4HNotifier(boolean promote, boolean recommended){
		this.promote = promote;
		this.recommended = recommended;
		
		logger.trace_function();
	}
	
	@Override
	public boolean needsToRunAfterFinalized(){
		logger.trace_function();
		//TODO: set Tag on Baseline
		return true;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		logger.trace_function();
		return BuildStepMonitor.BUILD;//we use BUILD because cleacase-plugin does(clearcase/ucm/UcmMakeBaseline
	}
	
	//@SuppressWarnings("unchecked")
	@Override
	public boolean perform (AbstractBuild build, Launcher launcer, BuildListener listener)throws InterruptedException, IOException {
		logger.trace_function();
		boolean res;
		SCM scmTemp = build.getProject().getScm();
		if (!(scmTemp instanceof CC4HClass)){
			listener.fatalError("Not a CC4H scm");
			res = false;
		}
		CC4HClass scm = (CC4HClass) scmTemp;
		baseline = scm.getBaseline();
		
		Result result = build.getResult();
		if (result.equals(Result.SUCCESS)){
			//TODO: Tag baseline (ask expert if before or after succes is determined
			//baseline.promote(); - waiting for implementation
			logger.log("baseline promoted to something");
			
			res = true;
		}
		else if (result.equals(Result.FAILURE)){
			//baselineHer.demote();- waiting for implementation
			logger.log("baseline demoted to something");
			res = true;
		}
		else {
			logger.log("wut?!");
			res = false;
		}
		return res;
	}
	
	public boolean isPromote(){
		return promote;
	}
	
	public boolean isRecommended(){
		return recommended;
	}
	
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		public DescriptorImpl(){
			super(CC4HNotifier.class);
		}
		
		@Override
		public String getDisplayName() {
			return "ClearCase 4 Hudson";
			
		}
		

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
		
		/*@Override
		public String getHelpFile() {
			return "/plugin/cc4h/notifier/help.html";
		}*/
	}

}
