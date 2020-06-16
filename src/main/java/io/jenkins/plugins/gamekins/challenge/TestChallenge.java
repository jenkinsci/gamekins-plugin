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

    public TestChallenge(String currentCommit, int testCount, User user) {
        this.currentCommit = currentCommit;
        this.testCount = testCount;
        this.user = user;
    }

    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run) {
        try {
            if (ChallengeFactory.getTestCount(run) <= this.testCount) {
                return false;
            }
            ArrayList<String> lastChangedFilesOfUser =
                    new ArrayList<>(ChallengeFactory.getLastChangedTestFilesOfUser(
                            constants.get("workspace"), user, 0, currentCommit));
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
