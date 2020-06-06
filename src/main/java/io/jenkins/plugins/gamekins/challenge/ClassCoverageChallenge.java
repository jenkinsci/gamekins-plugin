package io.jenkins.plugins.gamekins.challenge;

import hudson.model.AbstractBuild;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class ClassCoverageChallenge extends CoverageChallenge {

    public ClassCoverageChallenge(String packagePath, String className) throws IOException {
        super(packagePath, className);
    }

    @Override
    public boolean isSolved(AbstractBuild<?, ?> build) {
        Document document;
        try {
            document = Jsoup.parse(classFile, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        int fullyCoveredLines = calculateCoveredLines(document, "fc");
        int partiallyCoveredLines = calculateCoveredLines(document, "pc");
        int notCoveredLines = calculateCoveredLines(document, "nc");
        double newCoverage = fullyCoveredLines / (double) (fullyCoveredLines + partiallyCoveredLines + notCoveredLines);
        return fullyCoveredLines > this.fullyCoveredLines && newCoverage > this.coverage;
    }

    @Override
    public int getScore() {
        return this.coverage >= 0.8 ? 2 : 1;
    }

    @Override
    public String toString() {
        String[] split = getPackagePath().split("/");
        return "Write a test to cover more lines in class " + getClassName() + " in package " + split[split.length - 1];
    }
}
