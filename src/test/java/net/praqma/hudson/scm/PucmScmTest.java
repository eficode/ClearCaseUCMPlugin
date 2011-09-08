package net.praqma.hudson.scm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;

import net.praqma.hudson.notifier.CCUCMNotifier;

import org.jvnet.hudson.test.HudsonTestCase;

public class PucmScmTest extends HudsonTestCase {
	// public void test1() throws Exception {
	// /*
	// * Disabled since it fails when run on jenkins
	// FreeStyleProject project = createFreeStyleProject();
	// project.getBuildersList().add(new Shell("echo hello"));
	//
	// FreeStyleBuild build = project.scheduleBuild2(0).get();
	// System.out.println(build.getDisplayName()+" completed");
	//
	// // TODO: change this to use HtmlUnit
	// String s = FileUtils.readFileToString(build.getLogFile());
	// assertTrue(s.contains("+ echo hello"));*/
	//
	// assertTrue(true);
	// }

	public void testPrintBaselineWhenNull() throws Exception {
		CCUCMScm scm = new CCUCMScm();

		try {
			scm.printBaselines( null, System.out );
		} catch (Exception e) {
			fail( "Could not handle baseline list when null" );
		}
		assertTrue( true );
	}
	
	public void testSpecificBaselineTest() throws IOException {
		FreeStyleProject proj = createFreeStyleProject();
		proj.getPublishersList().add( new CCUCMNotifier( false, false, false ) );
        List<ParameterDefinition> list = new ArrayList<ParameterDefinition>();
        list.add(new StringParameterDefinition("pucm_baseline", "empty"));
        ParametersDefinitionProperty pdp = new ParametersDefinitionProperty(list);
        proj.addProperty(pdp);
	}

}
