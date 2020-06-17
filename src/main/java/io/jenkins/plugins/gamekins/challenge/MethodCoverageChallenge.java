package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MethodCoverageChallenge extends CoverageChallenge {

    final File coverageFile;
    final String methodName;
    final int lines;
    final int missedLines;

    public MethodCoverageChallenge(String packagePath, String className, String branch) throws IOException {
        super(packagePath, className, branch);
        this.coverageFile = new File(packagePath + "/" + className + ".html");
        Random random = new Random();
        ArrayList<CoverageMethod> methods = getNotFullyCoveredMethodEntries();
        CoverageMethod method = methods.get(random.nextInt(methods.size()));
        this.methodName = method.getMethodName();
        this.lines = method.getLines();
        this.missedLines = method.getMissedLines();
    }

    private ArrayList<CoverageMethod> getNotFullyCoveredMethodEntries() throws IOException {
        ArrayList<CoverageMethod> methods = getMethodEntries();
        methods.removeIf(method -> method.missedLines == 0);
        return methods;
    }

    private ArrayList<CoverageMethod> getMethodEntries() throws IOException {
        Elements elements = generateDocument(coverageFile.getPath(), "UTF-8").select("tr");
        ArrayList<CoverageMethod> methods = new ArrayList<>();
        for (Element element : elements) {
            boolean matches = false;
            for (Node node : element.childNodes()) {
                for (Attribute attribute : node.attributes()){
                    if (attribute.getKey().equals("id") && attribute.getValue().matches("a\\d+")) {
                        matches = true;
                        break;
                    }
                }
                if (matches) break;
            }
            if (matches) {
                String methodName = "";
                int lines = 0;
                int missedLines = 0;
                for (Node node : element.childNodes()) {
                    for (Attribute attribute : node.attributes()){
                        if (attribute.getKey().equals("id")) {
                            if (attribute.getValue().matches("a\\d+")) {
                                methodName = node.childNode(0).childNode(0).toString();
                            } else if (attribute.getValue().matches("h\\d+")) {
                                missedLines = Integer.parseInt(node.childNode(0).toString());
                            } else if (attribute.getValue().matches("i\\d+")) {
                                lines = Integer.parseInt(node.childNode(0).toString());
                            }
                            break;
                        }
                    }
                }
                methods.add(new CoverageMethod(methodName, lines, missedLines));
            }
        }
        return methods;
    }

    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run) {
        try {
            ArrayList<CoverageMethod> methods = getMethodEntries();
            for (CoverageMethod method : methods) {
                if (method.getMethodName().equals(this.methodName)) {
                    if (method.missedLines < this.missedLines) {
                        this.solved = System.currentTimeMillis();
                        return true;
                    }
                }
            }
        } catch (IOException ignored) { }
        return false;
    }

    @Override
    public boolean isSolvable(HashMap<String, String> constants) {
        if (!this.branch.equals(constants.get("branch"))) return true;
        try {
            ArrayList<CoverageMethod> methods = getMethodEntries();
            for (CoverageMethod method : methods) {
                if (method.getMethodName().equals(this.methodName)) {
                    return method.missedLines > 0;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    @Override
    public int getScore() {
        return ((this.lines - this.missedLines) / (double) this.lines) > 0.8 ? 3 : 2;
    }

    @Override
    public String toString() {
        String[] split = getPackagePath().split("/");
        return "Write a test to cover more lines of method " + this.methodName + " in class " + getClassName()
                + " in package " + split[split.length - 1] + " (created for branch " + branch + ")";
    }

    static class CoverageMethod {

        final String methodName;
        final int lines;
        final int missedLines;

        CoverageMethod (String methodName, int lines, int missedLines) {
            this.methodName = methodName;
            this.lines = lines;
            this.missedLines = missedLines;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getLines() {
            return lines;
        }

        public int getMissedLines() {
            return missedLines;
        }
    }
}
