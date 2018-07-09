package net.praqma.jenkins.test.unit;

import hudson.model.*;
import net.praqma.clearcase.ucm.entities.*;
import net.praqma.hudson.*;
import org.junit.*;
import org.jvnet.hudson.test.*;

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
        Stream s = new Stream();;
        Component c = (Component)Component.getEntity("component:componentA@\\vobA");
        CCUCMBuildAction action = new CCUCMBuildAction(s, c);
        fsb.addAction(action);
        fsb.save();
    }

}
