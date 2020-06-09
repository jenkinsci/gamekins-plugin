package io.jenkins.plugins.gamekins.challenge;

import hudson.model.AbstractBuild;

public class DummyChallenge implements Challenge {
    @Override
    public boolean isSolved(AbstractBuild<?, ?> build) {
        return true;
    }

    @Override
    public int getScore() {
        return 0;
    }

    @Override
    public String toString() {
        return "You have nothing developed recently";
    }
}
