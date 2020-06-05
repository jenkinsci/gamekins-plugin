package io.jenkins.plugins.gamekins.challenge;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;

public abstract class CoverageChallenge implements Challenge {

    final String packagePath;
    final String className;
    final int fullyCoveredLines;
    final int partiallyCoveredLines;
    final int notCoveredLines;
    final double coverage;
    final File classFile;

    public CoverageChallenge(String packagePath, String className) throws IOException {
        this.packagePath = packagePath;
        this.className = className;
        this.classFile = new File(packagePath + "/" + className + ".java.html");
        Document document = Jsoup.parse(classFile, "UTF-8");
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

    String getPackagePath() {
        return this.packagePath;
    }

    String getClassName() {
        return this.className;
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
