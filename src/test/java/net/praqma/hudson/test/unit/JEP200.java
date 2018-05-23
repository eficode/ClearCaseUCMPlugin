package net.praqma.hudson.test.unit;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.hudson.CCUCMBuildAction;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Created by mads on 5/23/18.
 */
public class JEP200 {

    @Rule
    public JenkinsRule jr = new JenkinsRule();

    @Test
    public void testJEP200() throws Exception {
        FreeStyleProject fsp = jr.createProject(FreeStyleProject.class);
        FreeStyleBuild fsb = jr.buildAndAssertSuccess(fsp);

        Stream s = new Stream();
        CCUCMBuildAction action = new CCUCMBuildAction(s, null);
        fsb.addAction(action);
        fsb.save();
    }

}
