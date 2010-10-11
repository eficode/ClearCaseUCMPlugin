package net.praqma;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

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
import hudson.util.FormValidation;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class CC4HClass extends SCM {

	@Override
	public boolean checkout(AbstractBuild arg0, Launcher arg1, FilePath arg2,
			BuildListener arg3, File arg4) throws IOException,
			InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		// TODO Auto-generated method stub
		return null;
	}

  @Override
	public PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener,
            SCMRevisionState baseline) throws IOException, InterruptedException {
		return PollingResult.NO_CHANGES;
	}
  
  @Override
	public SCMRevisionState	calcRevisionsFromBuild(AbstractBuild<?,?> build, Launcher launcher, TaskListener listener)  throws IOException, InterruptedException {
	  return null;
	}
  	
  @Extension
	public static class CC4HClassDescriptor extends SCMDescriptor<CC4HClass> {
		private String cleartool;
		private List<String> levels;
		
		public CC4HClassDescriptor(){
			super(CC4HClass.class,null);
			levels = new ArrayList<String>();
			levels.add("INITIAL");
			levels.add("BUILT");
			levels.add("TESTED");
			levels.add("RELEASED");
			levels.add("REJECTED");
			load();
		}

		@Override
		public boolean configure(org.kohsuke.stapler.StaplerRequest req, JSONObject json) throws FormException {
			cleartool = req.getParameter("cc4h.cleartool").trim();
			save();
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Clearcase 4 Hudson";
		}
				
		public FormValidation doExecutableCheck(@QueryParameter String value) {
		    return FormValidation.validateExecutable(value);
		}

		public List<String> getLevels() {
			return levels;
		}
		
		public String getCleartool() {
			if(cleartool==null)
				return "ct";
			return cleartool;
		}
	}
}
