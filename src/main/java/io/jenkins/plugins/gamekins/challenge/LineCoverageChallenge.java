package io.jenkins.plugins.gamekins.challenge;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.gamekins.util.JacocoUtil;
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

    public LineCoverageChallenge(JacocoUtil.ClassDetails classDetails, String branch, FilePath workspace)
            throws IOException, InterruptedException {
        super(classDetails, branch, workspace);
        Elements elements = JacocoUtil.getLines(JacocoUtil.calculateCurrentFilePath(workspace,
                classDetails.getJacocoSourceFile(), classDetails.getWorkspace()));
        Random random = new Random();
        Element element = elements.get(random.nextInt(elements.size()));
        this.lineNumber = Integer.parseInt(element.attr("id").substring(1));
        this.coverageType = element.attr("class");
        this.lineContent = element.text();
    }

    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener,
                            FilePath workspace) {
        FilePath jacocoSourceFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.getJacocoSourceFile(),
                        classDetails.getWorkspace()), this.branch);
        FilePath jacocoCSVFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.getJacocoCSVFile(),
                        classDetails.getWorkspace()), this.branch);

        Document document;
        try {
            if (!jacocoSourceFile.exists() || !jacocoCSVFile.exists()) {
                listener.getLogger().println("[Gamekins] JaCoCo source file " + jacocoSourceFile.getRemote()
                        + " exists " + jacocoSourceFile.exists());
                listener.getLogger().println("[Gamekins] JaCoCo csv file " + jacocoCSVFile.getRemote()
                        + " exists " + jacocoCSVFile.exists());
                return false;
            }

            document = JacocoUtil.generateDocument(jacocoSourceFile);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(listener.getLogger());
            return false;
        }

        Elements elements = document.select("span." + "fc");
        if (coverageType.equals("nc")) elements.addAll(document.select("span." + "pc"));
        for (Element element : elements) {
            if (element.text().equals(this.lineContent)) {
                this.solved = System.currentTimeMillis();
                this.solvedCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(this.classDetails.getClassName(),
                                jacocoCSVFile);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSolvable(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener,
                              FilePath workspace) {
        if (!this.branch.equals(constants.get("branch"))) return true;
        FilePath jacocoSourceFile = JacocoUtil.calculateCurrentFilePath(workspace,
                this.classDetails.getJacocoSourceFile(), classDetails.getWorkspace());

        Document document;
        try {
            if (!jacocoSourceFile.exists()) {
                listener.getLogger().println("[Gamekins] JaCoCo source file "
                        + jacocoSourceFile.getRemote() + " exists " + jacocoSourceFile.exists());
                return true;
            }

            document = JacocoUtil.generateDocument(jacocoSourceFile);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(listener.getLogger());
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
    String getName() {
        return "LineCoverageChallenge";
    }

    @Override
    public String toString() {
        //TODO: Add content of line
        return "Write a test to cover line " + this.lineNumber + " in class " + classDetails.getClassName()
                + " in package " + classDetails.getPackageName() + " (created for branch " + branch + ")";
    }
}
