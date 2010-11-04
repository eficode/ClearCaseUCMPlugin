package net.praqma.hudson.scm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.PollingResult;
import hudson.scm.SCMRevisionState;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import net.praqma.clearcase.objects.Baseline;
import net.praqma.clearcase.objects.Component;
import net.praqma.clearcase.objects.Stream;
import net.praqma.clearcase.objects.Version;
import net.praqma.debug.Debug;
import net.praqma.clearcase.objects.ClearBase;

/**
 * CC4HClass is responsible for everything regarding Hudsons connection to ClearCase pre-build.
 * This class defines all the files required by the user. The information can be entered on the config page.
 * 
 * @author Troels Selch Sørensen
 * @author Margit Bennetzen
 *
 */
public class CC4HClass extends SCM {
	
	private String levelToPoll;
	private String loadModule;
	private String component;
	private String stream;
	private boolean newest;
	private boolean newerThanRecommended;
	
	private Baseline baseline;
	private List<String> levels = null;
	private List<String> loadModules = null;
	
	private PrintStream hudsonOut;//hudsonOut.println("");
	
	protected static Debug logger = Debug.GetLogger();

	/**
	 * The constructor is used by Hudson to create the instance of the plugin needed for a connection to ClearCase.
	 * It is annotated with <code>@DataBoundConstructor</code> to tell Hudson where to put the information retrieved from the configuration page in the WebUI.
	 * 
	 * @param component This string defines the component needed to find baselines.
	 * @param levelToPoll This string defines the level to poll ClearCase for.
	 * @param loadModule This string tells if we should load all modules or only the ones that are modifiable.
	 * @param stream This string defines the stream needed to find baselines.
	 * @param newest This boolean tells whether we should build only the newest baseline.
	 * @param newerThanRecommended This boolean tells whether we should look at all baselines or only ones newer than the recommended baseline 
	 */
	@DataBoundConstructor
	public CC4HClass(String component, String levelToPoll, String loadModule,
			String stream, boolean newest, boolean newerThanRecommended) {
		
		logger.trace_function();
		this.component = component;
		this.levelToPoll = levelToPoll;
		this.loadModule = loadModule;
		this.stream = stream;
		this.newest = newest;
		this.newerThanRecommended = newerThanRecommended;
	}
	
	/**
	 * The repository is checked for new baselines, and if any, then the oldest will be built.
	 * 
	 */
	@Override
	public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace,
			BuildListener listener, File changelogFile) throws IOException,
			InterruptedException {
		logger.trace_function();
		hudsonOut = listener.getLogger();

		/* Examples to use from testbase.xml:
		 *   stream = "stream:STREAM_TEST1@\PDS_PVOB"
		 *   component = "component:COMPONENT_TEST1@\PDS_PVOB"
		 *   Level to poll = "INITIAL
		 *   (3 files changed)
		 */
		
		//setting up workspace
		Component comp = null;
		try{
			comp = Component.GetObject(component, false); // (false means that we don't trust that the component exists in PVOB)
		}catch(NullPointerException n){
			listener.fatalError("Component "+component+" does not exist");
		}
			
		Stream s = null;
		try{
			s = Stream.GetObject(stream, false);
		}catch (NullPointerException n){
			listener.fatalError("Stream "+stream+" does not exist");
		}
	
		if (s == null || comp == null)			
			return false;
	
		hudsonOut.println("Getting " + (newerThanRecommended?"baselines newer than the recomended baseline ":"all baselines ")
				+ "for "+component+" and "+stream+ " on promotionlevel "+levelToPoll);
		
		/*TODO consider moving rest of method to calcRevisionsFromBuild - remember to make variables global*/
		List<Baseline> baselines = comp.GetBlsWithPlevel(s, ClearBase.Plevel.valueOf(levelToPoll), false, false/*TODO:newerThanRecommended*/);
		
		//Here is logic for getting baseline depending on boolean 'newest'
		if(baselines.size()>0){
			hudsonOut.println("Retrieved baselines:");
			for(Baseline b : baselines)
				hudsonOut.println(b.GetShortname());
			if(newest){
				baseline = baselines.get(0);
				hudsonOut.println("Building newest baseline: "+baseline);
			} else {
				baseline = baselines.get(baselines.size()-1);
				hudsonOut.println("Building next baseline: "+baseline);
			}
		} else {
			//Nothing to do
			hudsonOut.println("No baselines on chosen parameters");
			return false;
		}
		
		//possible values for tag
		hudsonOut.println("build.getNumber(): "+build.getNumber());
		hudsonOut.println("build.getTimestampString(): "+build.getTimestampString());
		hudsonOut.println("build.getTimestampString2(): "+build.getTimestampString2());
		hudsonOut.println("build.getParent().getDisplayName(): "+build.getParent().getDisplayName());
		
		baseline.MarkBuildInProgess(); //TODO: Here we need Tag instead, including Hudson job info
	
		//List<Tag> tags = baseline.getTags() //with build.getParent().getDisplayName()
		//go through list and check for buildInProgress
		/*if (buildInprogress)
			hudsonOut.println("baseline is marked with build in progress");
			return false
		else
			så sætter vi tag
			tag.setValue() 
			tag.persist();
		*/
		hudsonOut.println("baseline is marked with: ");//TODO insert tag toString()
		
		List<Version> changes = baseline.GetDiffs();//TODO Should we move it to compareRemoteRevisionsWith()?
			
		hudsonOut.println(changes.size()+ " elements changed");
		/*If there is a new baseline, but no new files - we DO NOT build*/
		if (changes.size()==0)
			return false;
		return writeChangelog(changelogFile,changes);
	}
	
	/**
	 * This method is used by {@link <public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace,
			BuildListener listener, File changelogFile) throws IOException,
			InterruptedException> [checkout()]} to write the changelog used uses.
	 * 
	 * @param changelogFile The file given by Hudson.
	 * @param changes The list of changes to be written as XML.
	 * @return true if the changelog was persisted, false if not.
	 * @throws IOException
	 */
	private boolean writeChangelog(File changelogFile, List<Version> changes) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		//Here the .hudson/jobs/[project name]/changelog.xml is written
		//TODO: Skal der flere levels i et changeset? Afklar med Lars og Christian hvordan strukturen ser ud..
		hudsonOut.print("Writing Hudson changelog...");
		try{
			baos.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
			baos.write("<changelog>".getBytes());
			String temp;
			for(Version v: changes)
			{
				baos.write("<changeset>".getBytes());
				temp = "<filepath>" + "Branch: "+v.GetBranch()+" Date "+v.GetDate()+" filename: "+v.GetFilename()+" machine: "+v.GetMachine()+" revision: "+v.GetRevision()+" user: "+v.GetUser()+ "</filepath>";
				baos.write(temp.getBytes());
				baos.write("</changeset>".getBytes());
			}
			baos.write("</changelog>".getBytes());
			FileOutputStream fos = new FileOutputStream(changelogFile);
			//FileOutputStream fos = new FileOutputStream(new File("F:\temp")); //use this line to test changelog write failure
		    fos.write(baos.toByteArray());
		    fos.close();
			hudsonOut.println(" DONE");
			return true;
		}catch (Exception e){
			//If the changelog cannot be written, the baseline will not be build
			hudsonOut.println(" FAILED");
			logger.log("Changelog failed with "+e.getMessage());
			return false;
		}
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		logger.trace_function();
		return new ChangeLogParserImpl();
	}

	/**
	 * Currently this method returns BUILD_NOW, but later it should evaluate IF Hudson should build.
	 */
	@Override
	public PollingResult compareRemoteRevisionWith(
			AbstractProject<?, ?> project, Launcher launcher,
			FilePath workspace, TaskListener listener, SCMRevisionState baseline)
			throws IOException, InterruptedException {
		logger.trace_function();
		hudsonOut.println("UDVIKLING: compareRemoteRevisionsWith");
		/*IF a baseline is made WITHOUT any changed files - we could return PollingResult.SIGNIFICANT and a changelog (with user and comment) but not build.*/
		//This method doesn't do anything - checkout() does the work for now
		return PollingResult.BUILD_NOW; 
	}

	@Override
	public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build,
			Launcher launcher, TaskListener listener) throws IOException,
			InterruptedException {
		logger.trace_function();
		
		if (baseline == null){
			return null;
		}
		hudsonOut.println("UDVIKLING:  Getting new SCMRevisionStateImpl...");//TODO make sensible text here
		SCMRevisionStateImpl scmRS = new SCMRevisionStateImpl();
		
		logger.log (" scmRS: "+scmRS.toString());
		//TODO: DET  ER HER, DER SNER - her skal returneres null (ingen nye baselines) eller en liste af baselines eller noget boolean-noget
		return scmRS;
	}

	/**
	 * This method is used by Hudson to load persisted data when users enter the job config page. 
	 * @return
	 */
	public String getLevelToPoll() {
		logger.trace_function();
		return levelToPoll;
	}

	/**
	 * This method is used by Hudson to load persisted data when users enter the job config page. 
	 * @return component
	 */
	public String getComponent() {
		logger.trace_function();
		return component;
	}

	/**
	 * This method is used by Hudson to load persisted data when users enter the job config page. 
	 * @return stream
	 */
	public String getStream() {
		logger.trace_function();
		return stream;
	}

	/**
	 * This method is used by Hudson to load persisted data when users enter the job config page. 
	 * @return loadModule - which can be "all" or "modifiable"
	 */
	public String getLoadModule() {
		logger.trace_function();
		return loadModule;
	}

	/**
	 * This method is used by CC4HNotifier.perform for tagging the baseline after build
	 * @return baseline
	 */
	public Baseline getBaseline(){
		logger.trace_function();
		return baseline;
	}

	/**
	 * This method is used by Hudson to load persisted data when users enter the job config page. 
	 * @return newest - whether the user wants the newest or the latest baseline 
	 */
	public boolean isNewest() {
		logger.trace_function();
		return newest;
	}

	/**
	 * This method is used by Hudson to load persisted data when users enter the job config page. 
	 * @return newerThanRecommended - whether the user only wants baselines newer than recommended
	 */
	public boolean isNewerThanRecommended() {
		logger.trace_function();
		return newerThanRecommended;
	}

	/**
	 * This class is used to describe the plugin to Hudson
	 * @author Troels Selch Sørensen
	 * @author Margit Bennetzen
	 *
	 */
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
			load(); //load() MUST be called to get persisted data (check out save() as well)
		}

		/**
		 * This method is called, when the user saves the global Hudson configuration.
		 * 
		 */
		@Override
		public boolean configure(org.kohsuke.stapler.StaplerRequest req,
				JSONObject json) throws FormException {
			logger.trace_function();
			cleartool = req.getParameter("cc4h.cleartool").trim();
			save();
			//If no exception has been thrown at this point, then it's safe to return true
			return true;
		}

		/**
		 * This is called by Hudson to discover the plugin name
		 */
		@Override
		public String getDisplayName() {
			logger.trace_function();
			return "Clearcase 4 Hudson";
		}

		/**
		 * This method is called by the scm/CC4HClass/global.jelly to validate the input without reloading the global configuration page
		 * 
		 * @param value
		 * @return
		 */
		public FormValidation doExecutableCheck(@QueryParameter String value) {
			logger.trace_function();
			return FormValidation.validateExecutable(value);
		}

		/**
		 * Called by Hudson. If the user does not input a command for Hudson to use when polling, default value is returned 
		 * @return
		 */
		public String getCleartool() {
			logger.trace_function();
			if (cleartool == null || cleartool.equals(""))
				return "cleartool";
			return cleartool;
		}
		/**
		 * Used by Hudson to display a list of valid promotionlevels to build from
		 * @return
		 */
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
	
		/**
		 * Used by Hudson to display a list of loadModules (whether to poll all or only modifiable elements
		 * @return
		 */
		public List<String> getLoadModules() {
			logger.trace_function();
			loadModules = new ArrayList<String>();
			loadModules.add("All");
			loadModules.add("Modifiable");
			return loadModules;
		}
	}
}
