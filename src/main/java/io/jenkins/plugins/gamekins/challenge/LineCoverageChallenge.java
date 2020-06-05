package io.jenkins.plugins.gamekins.challenge;

import hudson.model.AbstractBuild;

import java.io.IOException;

public class LineCoverageChallenge extends CoverageChallenge {

    public LineCoverageChallenge(String packagePath, String className) throws IOException {
        super(packagePath, className);
    }

    @Override
    public boolean isSolved(AbstractBuild<?, ?> build) {
        return false;
    }

    @Override
    public int getScore() {
        return 2;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
