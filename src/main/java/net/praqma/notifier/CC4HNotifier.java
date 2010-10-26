package net.praqma.notifier;

import java.io.IOException;

import org.kohsuke.stapler.StaplerRequest;

import net.praqma.Baseline;
import net.praqma.Debug;
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
import net.praqma.CC4HClass;
import net.sf.json.JSONObject;

public class CC4HNotifier extends Notifier {
	
	private boolean promote;
	private boolean tagPostBuild;
	private boolean recommended;
	private Baseline baselineHer;
	
	protected static Debug logger = Debug.GetLogger();
	
	public CC4HNotifier(/*boolean promote, boolean tagPostBuild, boolean recommended*/){
		/*this.promote = promote;
		this.tagPostBuild = tagPostBuild;
		this.recommended = recommended;*/
		
		logger.trace_function();
	}
	
	@Override
	public boolean needsToRunAfterFinalized(){
		logger.trace_function();
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
		SCM scmTemp = build.getProject().getScm();
		if (!(scmTemp instanceof CC4HClass)){
			listener.fatalError("Not a CC4H scm");
			return false;
		}
		CC4HClass scm = (CC4HClass) scmTemp;
		baselineHer = scm.getBaseline();
		Result result = build.getResult();
		if (result.equals(Result.SUCCESS)){
			//TODO: Tag baseline (ask expert if before or after succes is determined
			//baselineHer.promote(); - waiting for implementation
			logger.log("baseline promoted to something");
			
			return true;
		}
		else if (result.equals(Result.FAILURE)){
			//baselineHer.demote();- waiting for implementation
			logger.log("baseline demoted to something");
			return true;
		}
		else {
			logger.log("wut?!");
			return false;
		}
	}
	
	public boolean isPromote(){
		return promote;
	}
	
	
	public boolean isTagPostBuild(){
		return tagPostBuild;
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
            Notifier n = new CC4HNotifier();
            return n;
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
