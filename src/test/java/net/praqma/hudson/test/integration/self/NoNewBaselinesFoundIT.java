package net.praqma.hudson.test.integration.self;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

import java.util.logging.Logger;
import net.praqma.hudson.scm.pollingmode.PollSelfMode;

public class NoNewBaselinesFoundIT extends BaseTestClass {
	
	@Rule
	public ClearCaseRule ccenv = new ClearCaseRule( "no-new-baselines", "setup-basic.xml" );
	
	private static Logger logger = Logger.getLogger(NoNewBaselinesFoundIT.class.getName() );
	
	public AbstractBuild<?, ?> initiateBuild( String projectName, boolean recommend, boolean tag, boolean description, boolean fail ) throws Exception {
		return jenkins.initiateBuild( projectName, new PollSelfMode("INITIAL"), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), recommend, tag, description, fail );
	}

	@Test
	@ClearCaseUniqueVobName( name = "one" )
	@TestDescription( title = "Self polling", text = "no baseline available" )
	public void basic() throws Exception {
		AbstractBuild<?, ?> build = initiateBuild( ccenv.getUniqueName(), false, false, false, false );
		
		Baseline baseline = ccenv.context.baselines.get( "model-1" );
		SystemValidator validator = new SystemValidator( build )
		.validateBuild( Result.NOT_BUILT )
		.validate();
	}

}
