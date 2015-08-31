package net.praqma.hudson.scm;

import java.util.ArrayList;
import java.util.List;
import javaposse.jobdsl.dsl.Context;
import net.praqma.hudson.scm.pollingmode.JobNameRequirement;

class JobsJobDslContext implements Context {
    List<JobNameRequirement> jobs = new ArrayList<JobNameRequirement>();

    public void job(String name) {
        jobs.add(new JobNameRequirement(name, null));
    }

    public void job(String name, String ignores) {
        jobs.add(new JobNameRequirement(name, ignores));
    }
}
