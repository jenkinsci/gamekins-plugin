package io.jenkins.plugins.gamekins.challenge;

import hudson.FilePath;
import io.jenkins.plugins.gamekins.util.JacocoUtil;
import org.jsoup.nodes.Document;

import java.io.IOException;

public abstract class CoverageChallenge implements Challenge {

    final JacocoUtil.ClassDetails classDetails;
    final int fullyCoveredLines;
    final int partiallyCoveredLines;
    final int notCoveredLines;
    final double coverage;
    double solvedCoverage = 0;
    final String branch;
    final long created = System.currentTimeMillis();
    long solved = 0;

    public CoverageChallenge(JacocoUtil.ClassDetails classDetails, String branch, FilePath workspace)
            throws IOException, InterruptedException {
        this.classDetails = classDetails;
        this.branch = branch;
        Document document = JacocoUtil.generateDocument(JacocoUtil.calculateCurrentFilePath(workspace,
                this.classDetails.getJacocoSourceFile(), classDetails.getWorkspace()));
        this.fullyCoveredLines = JacocoUtil.calculateCoveredLines(document, "fc");
        this.partiallyCoveredLines = JacocoUtil.calculateCoveredLines(document, "pc");
        this.notCoveredLines = JacocoUtil.calculateCoveredLines(document, "nc");
        this.coverage = classDetails.getCoverage();
    }

    @Override
    public long getCreated() {
        return this.created;
    }

    @Override
    public long getSolved() {
        return this.solved;
    }

    abstract String getName();

    @Override
    public String printToXML(String reason, String indentation) {
        String print = indentation + "<" + getName() + " created=\"" + this.created + "\" solved=\"" + this.solved
                + "\" class=\"" + classDetails.getClassName() + "\" coverage=\"" + this.coverage
                + "\" coverageAtSolved=\"" + this.solvedCoverage;
        if (!reason.isEmpty()) {
            print += "\" reason=\"" + reason;
        }
        print += "\"/>";
        return print;
    }
}
