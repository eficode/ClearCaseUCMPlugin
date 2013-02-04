package net.praqma.hudson.test.integration.userstories;

import hudson.model.*;
import hudson.model.Project;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.*;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.junit.DescriptionRule;
import net.praqma.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author cwolfgang
 *         Date: 04-02-13
 *         Time: 12:00
 */
public class JENKINS16620 extends BaseTestClass {

    @Rule
    public static ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-16620", "setup-JENKINS-16620.xml" );

    @Rule
    public static DescriptionRule desc = new DescriptionRule();

    @Test
    @TestDescription( title = "JENKINS-16620", text = "Cancelled builds cannot be rebuild" )
    public void jenkins16620() throws Exception {
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-16620", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() ).getProject();

        AbstractBuild build1 = new CCUCMRule.ProjectBuilder( project ).failBuild( true ).build();

        Baseline bl = ccenv.context.baselines.get( "model-1" ).load();

        bl.setPromotionLevel( net.praqma.clearcase.ucm.entities.Project.PromotionLevel.INITIAL );

        AbstractBuild build2 = new CCUCMRule.ProjectBuilder( project ).build();

        new SystemValidator( build2 ).validateBuild( Result.SUCCESS ).validate();
    }

}
