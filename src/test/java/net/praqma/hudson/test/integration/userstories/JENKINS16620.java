package net.praqma.hudson.test.integration.userstories;

import hudson.model.*;
import hudson.model.Project;
import hudson.scm.PollingResult;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.*;
import net.praqma.hudson.scm.pollingmode.PollSelfMode;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author cwolfgang
 *         Date: 04-02-13
 *         Time: 12:00
 */
public class JENKINS16620 extends BaseTestClass {

    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule( "JENKINS-16620", "setup-JENKINS-16620.xml" );

    @Rule
    public DescriptionRule desc = new DescriptionRule();

    @Test
    @TestDescription( title = "JENKINS-16620", text = "Changed baselines cannot be rebuild" )
    @ClearCaseUniqueVobName( name = "NORMAL" )
    public void jenkins16620() throws Exception {
        
        PollSelfMode m = new PollSelfMode("INITIAL");
        
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-16620", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() )
                .setMode(m)
                .getProject();

        AbstractBuild build1 = jenkins.getProjectBuilder( project ).failBuild( true ).build();

        Baseline bl = ccenv.context.baselines.get( "model-1" ).load();

        bl.setPromotionLevel( net.praqma.clearcase.ucm.entities.Project.PromotionLevel.INITIAL );

        AbstractBuild build2 = jenkins.getProjectBuilder( project ).build();

        new SystemValidator( build2 ).validateBuild( Result.SUCCESS ).validate();
    }

    @Test
    @TestDescription( title = "JENKINS-16620", text = "Changed baselines MUST NOT ABLE TO be rebuild on ANY" )
    @ClearCaseUniqueVobName( name = "ANY" )
    public void jenkins16620Any() throws Exception {
        
        PollSelfMode mode = new PollSelfMode("ANY");
        
        Project project = new CCUCMRule.ProjectCreator( "JENKINS-16620-ANY", "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob() )
            .setMode( mode )
            .getProject();

        AbstractBuild build1 = jenkins.getProjectBuilder( project ).failBuild( false ).build();

        new SystemValidator( build1 ).validateBuild( Result.SUCCESS ).validate();

        Baseline bl = ccenv.context.baselines.get( "model-1" ).load();

        bl.setPromotionLevel( net.praqma.clearcase.ucm.entities.Project.PromotionLevel.INITIAL );

        PollingResult result = project.poll( jenkins.createTaskListener() );

        assertFalse( result.hasChanges() );
    }

}
