package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;

public class DummyChallenge implements Challenge {
    @Override
    public boolean isSolved(String workspace, Run<?, ?> run) {
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
