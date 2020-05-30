package io.jenkins.plugins.gamekins.challenge;

import java.io.IOException;

public class LineCoverageChallenge extends CoverageChallenge {

    public LineCoverageChallenge(String packagePath, String className) throws IOException {
        super(packagePath, className);
    }

    @Override
    public boolean isSolved() {
        return false;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
