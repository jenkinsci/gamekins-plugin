package io.jenkins.plugins.gamekins.challenge;

import hudson.model.AbstractBuild;

import java.io.IOException;

public class ClassCoverageChallenge extends CoverageChallenge {

    public ClassCoverageChallenge(String packagePath, String className) throws IOException {
        super(packagePath, className);
    }

    @Override
    public boolean isSolved(AbstractBuild<?, ?> build) {
        return false;
    }

    @Override
    public String toString() {
        String[] split = getPackagePath().split("/");
        return "Write a test to cover more lines in class " + getClassName() + " in package " + split[split.length - 1];
    }
}
