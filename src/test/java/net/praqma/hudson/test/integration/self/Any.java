package net.praqma.hudson.test.integration.self;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.junit.DescriptionRule;
import net.praqma.junit.TestDescription;
import net.praqma.util.debug.Logger;
import org.junit.Rule;
import org.junit.Test;

/**
 * User: cwolfgang
 * Date: 08-11-12
 * Time: 22:12
 */
public class Any extends BaseTestClass {

    @Rule
    public static ClearCaseRule ccenv = new ClearCaseRule( "ccucm" );

    @Rule
    public static DescriptionRule desc = new DescriptionRule();

    private static Logger logger = Logger.getLogger();

    public AbstractBuild<?, ?> initiateBuild( String projectName, boolean recommend, boolean tag, boolean description, boolean fail ) throws Exception {
        return jenkins.initiateBuild( projectName, "self", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), recommend, tag, description, fail, false, false, "", "ANY" );
    }

    @Test
    @ClearCaseUniqueVobName( name = "self-any" )
    @TestDescription( title = "Self polling", text = "baseline available" )
    public void test() throws Exception {
        AbstractBuild<?, ?> build = initiateBuild( ccenv.getUniqueName(), false, false, false, false );

        Baseline baseline = ccenv.context.baselines.get( "client-3" );
        SystemValidator validator = new SystemValidator( build )
                .validateBuild( Result.SUCCESS )
                .validateBuiltBaseline( Project.PromotionLevel.INITIAL, baseline, false )
                .validate();
    }
}
