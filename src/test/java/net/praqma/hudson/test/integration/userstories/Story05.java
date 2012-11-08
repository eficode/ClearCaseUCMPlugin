package net.praqma.hudson.test.integration.userstories;

import java.io.File;
import java.util.logging.Level;

import net.praqma.hudson.test.BaseTestClass;
import net.praqma.util.test.junit.LoggingRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;
import net.praqma.clearcase.Deliver;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.scm.CCUCMScm;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.junit.DescriptionRule;
import net.praqma.junit.TestDescription;
import net.praqma.util.debug.Logger;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;

import static org.junit.Assert.*;

public class Story05 extends BaseTestClass {
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm-story05", "setup-story5.xml" );
	
	@Rule
	public static DescriptionRule desc = new DescriptionRule();

	private static Logger logger = Logger.getLogger();

	@Test
	@TestDescription( title = "Story 5", text = "New baseline, bl2, on dev stream, poll on childs. Deliver in progress.", configurations = { "Force deliver = false" } )
	public void story05() throws Exception {
		
		/* First build to create a view */
		AbstractBuild<?, ?> firstbuild = jenkins.initiateBuild(  ccenv.getUniqueName(), "child", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, true, false );
		//assertEquals( Result.NOT_BUILT, build.getResult() );
		
		/* Do the deliver */
		Stream dev1 = ccenv.context.streams.get( "one_dev" );
		Stream dev2 = ccenv.context.streams.get( "two_dev" );
		//Stream target = ccenv.context.streams.get( "one_int" );
		CCUCMBuildAction preaction = jenkins.getBuildAction( firstbuild );
		Stream target = preaction.getStream();
		
		/* Target */
		String tviewtag = preaction.getViewTag();
		File tpath = preaction.getViewPath();
		
		/* Set deliver one up and make sure the baseline is not found by polling */
		String d1viewtag = ccenv.getUniqueName() + "_one_dev";
		File d1path = ccenv.setDynamicActivity( dev1, d1viewtag, "dip1" );
		Baseline bl1 = getNewBaseline( d1path, "dip1.txt", "dip1" );
		bl1.setPromotionLevel( PromotionLevel.BUILT );
		
		/* Do not complete deliver */
		Deliver deliver1 = new Deliver( bl1, dev1, target, tpath, tviewtag );
		deliver1.deliver( true, false, true, false );
		
		/* Setup dev 2 with new baseline */
		String d2viewtag = ccenv.getUniqueName() + "_two_dev";
		File d2path = ccenv.setDynamicActivity( dev2, d2viewtag, "dip2" );
		Baseline bl2 = getNewBaseline( d2path, "dip2.txt", "dip2" );
		
		
		AbstractBuild<?, ?> build = jenkins.buildProject( firstbuild.getProject(), false );
		

		/* Build validation */
		assertTrue( build.getResult().isBetterOrEqualTo( Result.FAILURE ) );
		
		/* Expected build baseline */
		Baseline buildBaseline = jenkins.getBuildBaseline( build );
		assertEquals( bl2, buildBaseline );
		assertEquals( PromotionLevel.REJECTED, buildBaseline.getPromotionLevel( true ) );
		
		/* Created baseline */
		Baseline createdBaseline = jenkins.getCreatedBaseline( build );
		assertNull( createdBaseline );
	}

	
	protected Baseline getNewBaseline( File path, String filename, String bname ) throws ClearCaseException {
		
		try {
			ccenv.addNewElement( ccenv.context.components.get( "Model" ), path, filename );
		} catch( ClearCaseException e ) {
			ExceptionUtils.print( e, System.out, true );
		}
		return Baseline.create( bname, ccenv.context.components.get( "_System" ), path, LabelBehaviour.FULL, false );
	}
}
