package io.jenkins.plugins.gamekins.challenge;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;

public abstract class CoverageChallenge implements Challenge {

    final ChallengeFactory.ClassDetails classDetails;
    final int fullyCoveredLines;
    final int partiallyCoveredLines;
    final int notCoveredLines;
    final double coverage;
    final String branch;
    final long created = System.currentTimeMillis();
    long solved = 0;

    public CoverageChallenge(ChallengeFactory.ClassDetails classDetails, String branch) throws IOException {
        this.classDetails = classDetails;
        this.branch = branch;
        //this.classFile = new File(packagePath + "/" + className + ".java.html");
        Document document = Jsoup.parse(this.classDetails.jacocoSourceFile, "UTF-8");
        this.fullyCoveredLines = calculateCoveredLines(document, "fc");
        this.partiallyCoveredLines = calculateCoveredLines(document, "pc");
        this.notCoveredLines = calculateCoveredLines(document, "nc");
        this.coverage = this.fullyCoveredLines
                / (double) (this.fullyCoveredLines + this.partiallyCoveredLines + this.notCoveredLines);
    }

    public static int calculateCoveredLines(Document document, String modifier) {
        Elements elements = document.select("span." + modifier);
        return elements.size();
    }

    public static Document generateDocument(String filePath, String charset) throws IOException {
        return Jsoup.parse(new File(filePath), charset);
    }

    public static Document generateDocument(File file, String charset) throws IOException {
        return Jsoup.parse(file, charset);
    }

    public int getFullyCoveredLines() {
        return fullyCoveredLines;
    }

    public int getPartiallyCoveredLines() {
        return partiallyCoveredLines;
    }

    public int getNotCoveredLines() {
        return notCoveredLines;
    }
}
