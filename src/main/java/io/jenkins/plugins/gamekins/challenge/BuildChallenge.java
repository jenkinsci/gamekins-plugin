package io.jenkins.plugins.gamekins.challenge;

import hudson.model.AbstractBuild;
import hudson.model.Result;

public class BuildChallenge implements Challenge {
    @Override
    public boolean isSolved(AbstractBuild<?, ?> build) {
        return build.getResult() == Result.SUCCESS;
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
