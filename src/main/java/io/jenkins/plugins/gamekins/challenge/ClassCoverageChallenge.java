package io.jenkins.plugins.gamekins.challenge;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.gamekins.util.JacocoUtil;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;

public class ClassCoverageChallenge extends CoverageChallenge {

    public ClassCoverageChallenge(JacocoUtil.ClassDetails classDetails, String branch, FilePath workspace)
            throws IOException, InterruptedException {
        super(classDetails, branch, workspace);
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

        int fullyCoveredLines = JacocoUtil.calculateCoveredLines(document, "fc");
        double newCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(this.classDetails.getClassName(), jacocoCSVFile);
        if (fullyCoveredLines > this.fullyCoveredLines && newCoverage > this.coverage) {
            this.solved = System.currentTimeMillis();
            this.solvedCoverage = newCoverage;
            return true;
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
        return !(JacocoUtil.calculateCoveredLines(document, "pc") == 0
                && JacocoUtil.calculateCoveredLines(document, "nc") == 0);
    }

    @Override
    public int getScore() {
        return this.coverage >= 0.8 ? 2 : 1;
    }

    @Override
    String getName() {
        return "ClassCoverageChallenge";
    }

    @Override
    public String toString() {
        return "Write a test to cover more lines in class " + classDetails.getClassName()
                + " in package " + classDetails.getPackageName() + " (created for branch " + branch + ")";
    }
}
