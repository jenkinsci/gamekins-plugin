package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Result;
import hudson.model.Run;

import java.util.HashMap;

public class BuildChallenge implements Challenge {

    private final long created = System.currentTimeMillis();
    private long solved = 0;

    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run) {
        if (run.getResult() == Result.SUCCESS) {
            solved = System.currentTimeMillis();
            return true;
        }
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
        return "Let the Build run successfully";
    }
}
