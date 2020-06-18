package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;
import hudson.model.User;

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

    public TestChallenge(String currentCommit, int testCount, User user, String branch) {
        this.currentCommit = currentCommit;
        this.testCount = testCount;
        this.user = user;
        this.branch = branch;
    }

    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run) {
        if (!this.branch.equals(constants.get("branch"))) return false;
        try {
            if (ChallengeFactory.getTestCount(constants, run) <= this.testCount) {
                return false;
            }
            ArrayList<String> lastChangedFilesOfUser =
                    new ArrayList<>(ChallengeFactory.getLastChangedTestFilesOfUser(
                            constants.get("workspace"), user, 0, currentCommit));
            if (lastChangedFilesOfUser.size() > 0) {
                this.solved = System.currentTimeMillis();
                return true;
            }
        } catch (IOException ignored) { }
        return false;
    }

    @Override
    public boolean isSolvable(HashMap<String, String> constants) {
        return true;
    }

    @Override
    public int getScore() {
        return 1;
    }

    @Override
    public String toString() {
        return "Write a new test in branch " + branch;
    }
}
