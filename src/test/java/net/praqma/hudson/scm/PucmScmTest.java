import org.jvnet.hudson.test.HudsonTestCase;
import org.apache.commons.io.FileUtils;
import hudson.model.*;
import hudson.tasks.Shell;

public class PucmScmTest extends HudsonTestCase
{
    public void test1() throws Exception {
        FreeStyleProject project = createFreeStyleProject();
        project.getBuildersList().add(new Shell("echo hello"));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        System.out.println(build.getDisplayName()+" completed");

        // TODO: change this to use HtmlUnit
        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("+ echo hello"));
    }
}
