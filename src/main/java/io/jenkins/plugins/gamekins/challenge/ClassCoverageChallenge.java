package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;
import io.jenkins.plugins.gamekins.util.JacocoUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;

public class ClassCoverageChallenge extends CoverageChallenge {

    public ClassCoverageChallenge(JacocoUtil.ClassDetails classDetails, String branch) throws IOException {
        super(classDetails, branch);
    }

    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run) {
        Document document;
        try {
            document = Jsoup.parse(classDetails.getJacocoSourceFile(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        int fullyCoveredLines = JacocoUtil.calculateCoveredLines(document, "fc");
        int partiallyCoveredLines = JacocoUtil.calculateCoveredLines(document, "pc");
        int notCoveredLines = JacocoUtil.calculateCoveredLines(document, "nc");
        double newCoverage = fullyCoveredLines
                / (double) (fullyCoveredLines + partiallyCoveredLines + notCoveredLines);
        if (fullyCoveredLines > this.fullyCoveredLines && newCoverage > this.coverage) {
            this.solved = System.currentTimeMillis();
            this.solvedCoverage = JacocoUtil.
                    getCoverageInPercentageFromJacoco(this.classDetails.getClassName(),
                            this.classDetails.getJacocoCSVFile());
            return true;
        }
        return false;
    }

    @Override
    public boolean isSolvable(HashMap<String, String> constants, Run<?, ?> run) {
        if (!this.branch.equals(constants.get("branch"))) return true;
        Document document;
        try {
            document = Jsoup.parse(classDetails.getJacocoSourceFile(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return !(JacocoUtil.calculateCoveredLines(document, "pc") == 0
                && JacocoUtil.calculateCoveredLines(document, "nc") == 0);
    }

    @Override
    public int getScore() {
        return this.coverage >= 0.8 ? 2 : 1;
    }

    @Override
    public String toString() {
        return "Write a test to cover more lines in class " + classDetails.getClassName()
                + " in package " + classDetails.getPackageName() + " (created for branch " + branch + ")";
    }
}
