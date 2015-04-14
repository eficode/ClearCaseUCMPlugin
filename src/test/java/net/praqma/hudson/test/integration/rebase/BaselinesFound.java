package net.praqma.hudson.test.integration.rebase;


import net.praqma.hudson.test.BaseTestClass;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.hudson.test.SystemValidator;
import net.praqma.util.test.junit.DescriptionRule;
import net.praqma.util.test.junit.TestDescription;

import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.hudson.scm.pollingmode.PollRebaseMode;

public class BaselinesFound extends BaseTestClass {

      
    private static final Logger logger = Logger.getLogger(BaselinesFound.class.getName());
    
    
    @Rule
    public ClearCaseRule ccenv = new ClearCaseRule("ccucm", "setup-interproject.xml" );
    @Rule
    public DescriptionRule desc = new DescriptionRule();

    public AbstractBuild<?, ?> initiateBuild(String projectName, boolean recommend, boolean tag, boolean description, boolean fail, boolean mkbl) throws Exception {
        PollRebaseMode mode = new PollRebaseMode("INITIAL");
        mode.setCreateBaseline(mkbl);
        return jenkins.initiateBuild(projectName, mode, "_System@" + ccenv.getPVob(), "one_int@" + ccenv.getPVob(), recommend, tag, description, fail, true);
    }

    @Test
    @ClearCaseUniqueVobName(name = "basic-polling-succes")
    @TestDescription(title = "Rebase polling, succes", text = "rebase baseline available")
    public void testBasic() throws Exception {
        Baseline baseline = getNewBaseline();
        
        AbstractBuild<?, ?> build = initiateBuild("no-options-" + ccenv.getUniqueName(), false, false, false, false, false);
        SystemValidator validator = new SystemValidator(build)
            .validateBuild(Result.SUCCESS)
            .validateBuildView()
            .validateBuiltBaselineIsInFoundation(true)
            .validateBuiltBaseline(PromotionLevel.INITIAL, baseline, false)
            .validate();
    }
       
    @Test
    @ClearCaseUniqueVobName(name = "basic-polling-fail")
    @TestDescription(title = "Rebase polling, failure", text = "rebase baseline available")
    public void testBasicFail() throws Exception {
        Baseline baseline = getNewBaseline();
        AbstractBuild<?, ?> build = initiateBuild("no-options-fail" + ccenv.getUniqueName(), false, false, false, true, false);
        SystemValidator validator = new SystemValidator(build)
            .validateBuild(Result.FAILURE)
            .validateBuildView()
            .validateBuiltBaselineIsInFoundation(false)
            .validateBuiltBaseline(PromotionLevel.INITIAL, baseline, false)
            .validate();
    }
    
    @Test
    @ClearCaseUniqueVobName(name = "rebase-mkbl")
    @TestDescription(title = "Rebase polling, succes", text = "rebase baseline available, create a baseline")
    public void testBasicMkBl() throws Exception {
        Baseline baseline = getNewBaseline();
        
        AbstractBuild<?, ?> build = initiateBuild("rebase-mkbl-" + ccenv.getUniqueName(), false, false, false, false, true);
        SystemValidator validator = new SystemValidator(build)
            .validateBuild(Result.SUCCESS)
            .validateBuildView()
            .validateCreatedBaseline(true)
            .validateBuiltBaselineIsInFoundation(true)
            .validateBuiltBaseline(PromotionLevel.INITIAL, baseline, false)
            .validate();
    }
    
    protected Baseline getNewBaseline() throws ClearCaseException {
        /**/
        String viewtag = ccenv.getUniqueName() + "_bootstrap_int";
        System.out.println("VIEW: " + ccenv.context.views.get(viewtag));
        File path = new File(ccenv.context.mvfs + "/" + viewtag + "/" + ccenv.getVobName());

        System.out.println("PATH: " + path);

        Stream stream = Stream.get("bootstrap_int", ccenv.getPVob());
        Activity activity = Activity.create("ccucm-activity", stream, ccenv.getPVob(), true, "ccucm activity", null, path);
        UCMView.setActivity(activity, path, null, null);

        try {
            ccenv.addNewElement(ccenv.context.components.get("Model"), path, "test2.txt");
        } catch (ClearCaseException e) {
            ExceptionUtils.log(e, true);
            ExceptionUtils.print(e, System.out, true);
        }
        return Baseline.create("baseline-for-test", ccenv.context.components.get("_System"), path, Baseline.LabelBehaviour.INCREMENTAL, false);
    }
}
