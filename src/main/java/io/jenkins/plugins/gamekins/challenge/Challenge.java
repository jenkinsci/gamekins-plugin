package io.jenkins.plugins.gamekins.challenge;

import hudson.model.AbstractBuild;

public interface Challenge {

    boolean isSolved(AbstractBuild<?, ?> build);

    int getScore();

    @Override
    String toString();
}
