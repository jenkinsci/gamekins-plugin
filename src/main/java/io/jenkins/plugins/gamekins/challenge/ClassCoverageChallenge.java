package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;

public class ClassCoverageChallenge extends CoverageChallenge {

    public ClassCoverageChallenge(ChallengeFactory.ClassDetails classDetails, String branch) throws IOException {
        super(classDetails, branch);
    }

    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run) {
        Document document;
        try {
            document = Jsoup.parse(classDetails.jacocoSourceFile, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        int fullyCoveredLines = calculateCoveredLines(document, "fc");
        int partiallyCoveredLines = calculateCoveredLines(document, "pc");
        int notCoveredLines = calculateCoveredLines(document, "nc");
        double newCoverage = fullyCoveredLines
                / (double) (fullyCoveredLines + partiallyCoveredLines + notCoveredLines);
        if (fullyCoveredLines > this.fullyCoveredLines && newCoverage > this.coverage) {
            this.solved = System.currentTimeMillis();
            this.solvedCoverage = ChallengeFactory.
                    getCoverageInPercentageFromJacoco(this.classDetails.className, this.classDetails.jacocoCSVFile);
            return true;
        }
        return false;
    }

    @Override
    public boolean isSolvable(HashMap<String, String> constants) {
        if (!this.branch.equals(constants.get("branch"))) return true;
        Document document;
        try {
            document = Jsoup.parse(classDetails.jacocoSourceFile, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return !(calculateCoveredLines(document, "pc") == 0
                && calculateCoveredLines(document, "nc") == 0);
    }

    @Override
    public int getScore() {
        return this.coverage >= 0.8 ? 2 : 1;
    }

    @Override
    public String toString() {
        return "Write a test to cover more lines in class " + classDetails.className
                + " in package " + classDetails.packageName + " (created for branch " + branch + ")";
    }
}
