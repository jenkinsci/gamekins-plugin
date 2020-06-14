package io.jenkins.plugins.gamekins.challenge;

import hudson.model.AbstractBuild;
import hudson.model.User;

import java.io.IOException;
import java.util.ArrayList;

public class TestChallenge implements Challenge {

    private final String currentCommit;
    private final int testCount;
    private final User user;

    public TestChallenge(String currentCommit, int testCount, User user) {
        this.currentCommit = currentCommit;
        this.testCount = testCount;
        this.user = user;
    }

    @Override
    public boolean isSolved(AbstractBuild<?, ?> build) {
        try {
            if (ChallengeFactory.getTestCount(build) <= this.testCount) {
                return false;
            }
            String workspace = build.getWorkspace().getRemote();
            ArrayList<String> lastChangedFilesOfUser =
                    new ArrayList<>(ChallengeFactory.getLastChangedTestFilesOfUser(
                            workspace, user, 0, currentCommit));
            return lastChangedFilesOfUser.size() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public int getScore() {
        return 1;
    }

    @Override
    public String toString() {
        return "Write a new test";
    }
}
