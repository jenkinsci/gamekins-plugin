package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.User;
import io.jenkins.plugins.gamekins.util.GitUtil;
import io.jenkins.plugins.gamekins.util.JacocoUtil;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class TestChallenge implements Challenge {

    private final String currentCommit;
    private final int testCount;
    private final User user;
    private final String branch;
    private final long created = System.currentTimeMillis();
    private long solved = 0;
    private int testCountSolved = 0;

    public TestChallenge(String currentCommit, int testCount, User user, String branch) {
        this.currentCommit = currentCommit;
        this.testCount = testCount;
        this.user = user;
        this.branch = branch;
    }

    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener) {
        if (!this.branch.equals(constants.get("branch"))) return false;
        try {
            int testCountSolved = JacocoUtil.getTestCount(constants, run);
            if (testCountSolved <= this.testCount) {
                return false;
            }
            ArrayList<String> lastChangedFilesOfUser =
                    new ArrayList<>(GitUtil.getLastChangedTestFilesOfUser(
                            constants.get("workspace"), user, 0, currentCommit));
            if (lastChangedFilesOfUser.size() > 0) {
                this.solved = System.currentTimeMillis();
                this.testCountSolved = testCountSolved;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace(listener.getLogger());
        }
        return false;
    }

    @Override
    public boolean isSolvable(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener) {
        if (run.getParent().getParent() instanceof WorkflowMultiBranchProject) {
            for (WorkflowJob workflowJob : ((WorkflowMultiBranchProject) run.getParent().getParent()).getItems()) {
                if (workflowJob.getName().equals(this.branch)) return true;
            }
        } else {
            return true;
        }
        return false;
    }

    @Override
    public int getScore() {
        return 1;
    }

    @Override
    public long getCreated() {
        return this.created;
    }

    @Override
    public long getSolved() {
        return this.solved;
    }

    @Override
    public String printToXML(String reason, String indentation) {
        String print = indentation + "<TestChallenge created=\"" + this.created + "\" solved=\"" + this.solved
                + "\" tests=\"" + this.testCount + "\" testsAtSolved=\"" + this.testCountSolved;
        if (!reason.isEmpty()) {
            print += "\" reason=\"" + reason;
        }
        print += "\"/>";
        return print;
    }

    @Override
    public String toString() {
        return "Write a new test in branch " + branch;
    }
}
