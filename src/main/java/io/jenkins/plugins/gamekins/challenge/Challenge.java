package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;

public interface Challenge {

    boolean isSolved(String workspace, Run<?, ?> run);

    int getScore();

    @Override
    String toString();
}
