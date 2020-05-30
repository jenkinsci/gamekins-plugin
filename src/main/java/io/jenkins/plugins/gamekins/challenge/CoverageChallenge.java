package io.jenkins.plugins.gamekins.challenge;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;

public abstract class CoverageChallenge implements Challenge {

    private final String packagePath;
    private final String className;
    private final int fullyCoveredLines;
    private final int partiallyCoveredLines;
    private final int notCoveredLines;
    private final File classFile;

    public CoverageChallenge(String packagePath, String className) throws IOException {
        this.packagePath = packagePath;
        this.className = className;
        this.classFile = new File(packagePath + "/" + className + ".java.html");
        Document document = Jsoup.parse(classFile, "UTF-8");
        this.fullyCoveredLines = calculateCoveredLines(document, "fc");
        this.partiallyCoveredLines = calculateCoveredLines(document, "pc");
        this.notCoveredLines = calculateCoveredLines(document, "nc");
    }

    public static int calculateCoveredLines(Document document, String modifier) {
        Elements elements = document.select("span." + modifier);
        return elements.size();
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
