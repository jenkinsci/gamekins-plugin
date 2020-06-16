package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public class LineCoverageChallenge extends CoverageChallenge {

    private final int lineNumber;
    private final String lineContent;
    private final String coverageType;

    public LineCoverageChallenge(String packagePath, String className) throws IOException {
        super(packagePath, className);
        Elements elements = getLines();
        Random random = new Random();
        Element element = elements.get(random.nextInt(elements.size()));
        this.lineNumber = Integer.parseInt(element.attr("id").substring(1));
        this.coverageType = element.attr("class");
        this.lineContent = element.text();
    }

    private Elements getLines() throws IOException {
        Document document = Jsoup.parse(classFile, "UTF-8");
        Elements elements = document.select("span." + "pc");
        elements.addAll(document.select("span." + "nc"));
        elements.removeIf(e -> e.text().contains("{")
                || e.text().contains("}")
                || e.text().contains("class")
                || e.text().contains("void")
                || e.text().contains("public")
                || e.text().contains("private")
                || e.text().contains("protected")
                || e.text().contains("static")
                || e.text().equals("(")
                || e.text().equals(")"));
        return elements;
    }

    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run) {
        Document document;
        try {
            document = Jsoup.parse(classFile, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Elements elements = document.select("span." + "fc");
        if (coverageType.equals("nc")) elements.addAll(document.select("span." + "pc"));
        for (Element element : elements) {
            if (element.text().equals(this.lineContent)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSolvable(HashMap<String, String> constants) {
        Document document;
        try {
            document = Jsoup.parse(classFile, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Elements elements = document.select("span." + "pc");
        elements.addAll(document.select("span." + "nc"));
        for (Element element : elements) {
            if (element.text().equals(this.lineContent)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getScore() {
        return (this.coverage >= 0.8 || this.coverageType.equals("pc")) ? 3 : 2;
    }

    @Override
    public String toString() {
        String[] split = getPackagePath().split("/");
        //TODO: Add content of line
        return "Write a test to cover line " + this.lineNumber + " in class " + getClassName()
                + " in package " + split[split.length - 1];
    }
}
