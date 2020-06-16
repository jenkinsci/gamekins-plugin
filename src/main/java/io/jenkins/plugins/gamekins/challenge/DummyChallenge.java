package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;

import java.util.HashMap;

public class DummyChallenge implements Challenge {
    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run) {
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
