package net.praqma.hudson.test.integration.userstories;

import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.slaves.DumbSlave;
import net.praqma.clearcase.Deliver;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Baseline.LabelBehaviour;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.CCUCMBuildAction;
import net.praqma.hudson.notifier.CCUCMNotifier;
import net.praqma.hudson.scm.CCUCMScm;
import net.praqma.hudson.test.BaseTestClass;
import net.praqma.hudson.test.CCUCMRule;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import org.junit.Rule;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static net.praqma.hudson.test.BaseTestClass.jenkins;

import static net.praqma.hudson.test.CCUCMRule.ProjectCreator.Type;

public abstract class Story06Base extends BaseTestClass {

    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule("ccucm-story06", "setup-story5.xml");
    @Rule
    public DescriptionRule desc = new DescriptionRule();
    private static final Logger logger = Logger.getLogger(Story06Base.class.getName());

    /**
     *
     * @param stream1 If set a new baseline will be delivered from this stream
     * in a view
     * @param streamToMakeAnotherBaseline The stream used to make the baseline
     * found for the second build
     * @param viewTag1 .... Using this view tag to create the first baseline
     * @param viewTagToMakeAnotherBaseline .... Using this view tag to create
     * the second baseline
     * @param slave The slave executing the build, null if on master
     * @param jenkinsWorkspace If true, the first build will not run the post
     * build and therefore not complete the deliver
     * @throws Exception
     */
    
    public void runWithSlave(Stream stream1, Stream streamToMakeAnotherBaseline, String viewTag1, String viewTagToMakeAnotherBaseline, boolean jenkinsWorkspace) throws IOException, Exception {
                /* First build to create a view */
        Project project = new CCUCMRule.ProjectCreator(ccenv.getUniqueName(), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob())
                .setType(Type.child)
                .setForceDeliver(true)
                .setCreateBaseline(true) 
               .getProject();
        
        Slave slave = jenkins.createSlave();
        project.setAssignedLabel(slave.getSelfLabel());

        if (jenkinsWorkspace) {
            //project.getBuildersList().add( builder );
            project.getPublishersList().remove(CCUCMNotifier.class);
            ((CCUCMScm) project.getScm()).setAddPostBuild(false);
        }

        AbstractBuild<?, ?> firstbuild = jenkins.buildProject(project, false);

        Stream target = null;
        if (firstbuild != null) {
            CCUCMBuildAction preaction = jenkins.getBuildAction(firstbuild);
            target = preaction.getStream();
        } else {
            target = Stream.get(((CCUCMScm) project.getScm()).getStream());
        }

        logger.fine("Target stream is " + target);

        if (!jenkinsWorkspace) {
            /* Set deliver one up and make sure the baseline is not found by polling */
            Baseline bl1 = createNewContent(stream1, viewTag1, 1, PromotionLevel.BUILT);

            /* Do not complete deliver */
            Deliver deliver1 = new Deliver(stream1, target);
            deliver1.deliver(true, false, true, false);
        }

        /* Setup dev 2 with new baseline */
        Baseline bl2 = createNewContent(streamToMakeAnotherBaseline, viewTagToMakeAnotherBaseline, 2, null);

        if (jenkinsWorkspace) {
            //project.getBuildersList().remove( builder.getClass() );
            project.getPublishersList().add(new CCUCMNotifier());

            CCUCMBuildAction preaction = jenkins.getBuildAction(firstbuild);
            preaction.getBaseline().setPromotionLevel(PromotionLevel.BUILT);
        }

        AbstractBuild<?, ?> build = jenkins.buildProject(firstbuild.getProject(), false);

        /* Validate */
        SystemValidator validator = new SystemValidator(build);
        validator.validateBuild(Result.SUCCESS).validateBuiltBaseline(PromotionLevel.BUILT, bl2, false).validateCreatedBaseline(true).validate();
    }
    
    public void run(Stream stream1, Stream streamToMakeAnotherBaseline, String viewTag1, String viewTagToMakeAnotherBaseline, boolean jenkinsWorkspace) throws Exception {

        /* First build to create a view */
        Project project = new CCUCMRule.ProjectCreator(ccenv.getUniqueName(), "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob())
                .setType(Type.child)
                .setForceDeliver(true)
                .setCreateBaseline(true) 
               .getProject();
        


        if (jenkinsWorkspace) {
            //project.getBuildersList().add( builder );
            project.getPublishersList().remove(CCUCMNotifier.class);
            ((CCUCMScm) project.getScm()).setAddPostBuild(false);
        }

        AbstractBuild<?, ?> firstbuild = jenkins.buildProject(project, false);

        Stream target = null;
        if (firstbuild != null) {
            CCUCMBuildAction preaction = jenkins.getBuildAction(firstbuild);
            target = preaction.getStream();
        } else {
            target = Stream.get(((CCUCMScm) project.getScm()).getStream());
        }

        logger.fine("Target stream is " + target);

        if (!jenkinsWorkspace) {
            /* Set deliver one up and make sure the baseline is not found by polling */
            Baseline bl1 = createNewContent(stream1, viewTag1, 1, PromotionLevel.BUILT);

            /* Do not complete deliver */
            Deliver deliver1 = new Deliver(stream1, target);
            deliver1.deliver(true, false, true, false);
        }

        /* Setup dev 2 with new baseline */
        Baseline bl2 = createNewContent(streamToMakeAnotherBaseline, viewTagToMakeAnotherBaseline, 2, null);

        if (jenkinsWorkspace) {
            //project.getBuildersList().remove( builder.getClass() );
            project.getPublishersList().add(new CCUCMNotifier());

            CCUCMBuildAction preaction = jenkins.getBuildAction(firstbuild);
            preaction.getBaseline().setPromotionLevel(PromotionLevel.BUILT);
        }

        AbstractBuild<?, ?> build = jenkins.buildProject(firstbuild.getProject(), false);

        /* Validate */
        SystemValidator validator = new SystemValidator(build);
        validator.validateBuild(Result.SUCCESS).validateBuiltBaseline(PromotionLevel.BUILT, bl2, false).validateCreatedBaseline(true).validate();
    }

    protected Baseline createNewContent(Stream stream, String viewtag, int num, PromotionLevel level) throws ClearCaseException {
        File path = null;
        try {
            path = ccenv.setDynamicActivity(stream, viewtag, "dip-" + stream.getShortname());
        } catch (Exception e) {
            logger.log(Level.WARNING, "failed", e);
            path = ccenv.getDynamicPath(viewtag);
        }

        Baseline baseline = getNewBaseline(path, "dip" + num + ".txt", "dip" + num + "_" + stream.getShortname());
        if (level != null) {
            baseline.setPromotionLevel(level);
        }

        return baseline;
    }

    protected Baseline getNewBaseline(File path, String filename, String bname) throws ClearCaseException {

        try {
            ccenv.addNewElement(ccenv.context.components.get("Model"), path, filename);
        } catch (Exception e) {
            ExceptionUtils.print(e, System.out, true);
        }
        return Baseline.create(bname, ccenv.context.components.get("_System"), path, LabelBehaviour.FULL, false);
    }
}
