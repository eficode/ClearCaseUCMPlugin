package net.praqma;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;

import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.PollingResult;
import hudson.scm.SCMRevisionState;

import hudson.util.Digester2;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class CC4HClass extends SCM {
	
	private String levelToPoll;
	private String loadModule;
	private String component;
	private String stream;
	private boolean promote;
	private boolean tagPostBuild;
	
	private List<String> levels = null;
	private List<String> loadModules = null;
	
	protected static Debug logger = Debug.GetLogger();

	@DataBoundConstructor
	public CC4HClass(String component, String levelToPoll, String loadModule,
			String stream, boolean promote, boolean tagPostBuild) {
		
		logger.trace_function();
		this.component = component;
		this.levelToPoll = levelToPoll;
		this.loadModule = loadModule;
		this.stream = stream;
		this.setPromote(promote);
		this.setTagPostBuild(tagPostBuild);
		logger.log("promote: "+promote + " tagPostBuild "+tagPostBuild);
	}
	
	/**
	 * the repository is checked for new baselines, and if any, then the oldest will be built.
	 * 
	 */
	@Override
	public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace,
			BuildListener listener, File changelogFile) throws IOException,
			InterruptedException {
		logger.trace_function();
		component = "component:EH@\\PDS_PVOB";
		stream = "stream:EH@\\PDS_PVOB";
		levelToPoll = "INITIAL";
		//TODO perform actual checkout
		//Write the changelog to changelogFile
		//copypaste from bazaar:
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Component comp = new Component (component, true); // (true means that we know the component exists in PVOB)
		//TODO: set tag build in progress in CT
		//FOR USE WHEN FACTORY WORKS: 
		List<Baseline> baselines = comp.GetBlsWithPlevel(new Stream(stream, true), ClearBase.Plevel.valueOf(levelToPoll), false, false);
		Baseline bl = baselines.get(0);
		
		//below baseline is for testpurposes - we will call the real one from Component and get a list and find the oldest from that list
		//Baseline bl = new Baseline("baseline:Remote_15-10-2010_MPSX_Fixed_NFC_timer_subscription@\\PDS_PVOB", true);
		
		List<String> changes = bl.GetDiffs("list", true);

		//Here the .hudson/jobs/[project name]/changelog.xml is written
		baos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
		baos.write("<changelog>".getBytes());
		String temp;
		for(String s: changes)
		{
			baos.write("<changeset>".getBytes());
			temp = "<filepath>" + s + "</filepath>";
			baos.write(temp.getBytes());
			baos.write("</changeset>".getBytes());
		}
		baos.write("</changelog>".getBytes());
		FileOutputStream fos = new FileOutputStream(changelogFile);
	    fos.write(baos.toByteArray());
	    fos.close();
		return true;
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		logger.trace_function();
		return new ChangeLogParserImpl();
	}

	@Override
	public PollingResult compareRemoteRevisionWith(
			AbstractProject<?, ?> project, Launcher launcher,
			FilePath workspace, TaskListener listener, SCMRevisionState baseline)
			throws IOException, InterruptedException {
		logger.trace_function();
		return PollingResult.BUILD_NOW;
	}

	@Override
	public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build,
			Launcher launcher, TaskListener listener) throws IOException,
			InterruptedException {
		logger.trace_function();
		SCMRevisionStateImpl scmRS = new SCMRevisionStateImpl();
		logger.log (" scmRS: "+scmRS.toString());
		//DET  ER HER, DER SNER - her skal returneres null (ingen nye baselines) eller en liste af baselines eller noget boolean-noget
		return scmRS;
	}

	public String getLevelToPoll() {
		logger.trace_function();
		return levelToPoll;
	}

	public String getComponent() {
		logger.trace_function();
		return component;
	}

	public String getStream() {
		logger.trace_function();
		return stream;
	}

	public String getLoadModule() {
		logger.trace_function();
		return loadModule;
	}

	public void setPromote(boolean promote) {
		this.promote = promote;
	}

	public boolean isPromote() {
		return promote;
	}


	public void setTagPostBuild(boolean tagPostBuild) {
		this.tagPostBuild = tagPostBuild;
	}

	public boolean isTagPostBuild() {
		return tagPostBuild;
	}


	@Extension
	public static class CC4HClassDescriptor extends SCMDescriptor<CC4HClass> {
		private String cleartool;
		private List<String> levels;
		private List<String> loadModules;

		public CC4HClassDescriptor() {
			super(CC4HClass.class, null);
			logger.trace_function();
			
			levels = getLevels();
			loadModules = getLoadModules();
			load();
		}

		@Override
		public boolean configure(org.kohsuke.stapler.StaplerRequest req,
				JSONObject json) throws FormException {
			logger.trace_function();
			cleartool = req.getParameter("cc4h.cleartool").trim();
			save();
			return true;
		}

		@Override
		public String getDisplayName() {
			logger.trace_function();
			return "Clearcase 4 Hudson";
		}

		public FormValidation doExecutableCheck(@QueryParameter String value) {
			logger.trace_function();
			return FormValidation.validateExecutable(value);
		}

		public String getCleartool() {
			logger.trace_function();
			if (cleartool == null)
				return "ct";
			return cleartool;
		}
		
		public List<String> getLevels(){
			logger.trace_function();
			levels = new ArrayList<String>();
			levels.add("INITIAL");
			levels.add("BUILT");
			levels.add("TESTED");
			levels.add("RELEASED");
			levels.add("REJECTED");
			return levels;
		}
	
		public List<String> getLoadModules() {
			logger.trace_function();
			loadModules = new ArrayList<String>();
			loadModules.add("All");
			loadModules.add("Modifiable");
			return loadModules;
		}
	}
}
