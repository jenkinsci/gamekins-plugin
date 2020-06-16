package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;

import java.util.HashMap;

public interface Challenge {

    boolean isSolved(HashMap<String, String> constants, Run<?, ?> run);

    int getScore();

    @Override
    String toString();
}
