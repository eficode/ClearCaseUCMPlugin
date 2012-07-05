package net.praqma.hudson.test.integration.userstories;

import java.io.File;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.scm.CCUCMScm;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.util.debug.Logger;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;

import static org.junit.Assert.*;

public class Story04 {

	@ClassRule
	public static CCUCMRule jenkins = new CCUCMRule();
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm-story04" );

	private static Logger logger = Logger.getLogger();

	@Test
	@ClearCaseUniqueVobName( name = "story04" )
	public void story03a() throws Exception {
		
		Stream source = ccenv.context.streams.get( "one_dev" );
		Stream target = ccenv.context.streams.get( "one_int" );
		
		/* Prepare */
		/* Integration */
		String tviewtag = ccenv.getVobName() + "_one_int";
		File tpath = ccenv.setDynamicActivity( target, tviewtag, "strict-deliver" );
		Baseline tb = getNewBaseline( tpath, "merge.txt", "one" );
		target.recommendBaseline( tb );
		
		/* Development */
		String viewtag = ccenv.getVobName() + "_one_dev";
		File path = ccenv.setDynamicActivity( source, viewtag, "strict-deliver-dev" );
		Baseline b = getNewBaseline( path, "merge.txt", "two" );
		
		
		AbstractBuild<?, ?> build = jenkins.initiateBuild( "story04", "child", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), false, false, false, false, true );

		/* Build validation */
		assertTrue( build.getResult().isBetterOrEqualTo( Result.FAILURE ) );
		
		/* Expected build baseline */
		Baseline buildBaseline = jenkins.getBuildBaseline( build );
		assertEquals( b, buildBaseline );
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
