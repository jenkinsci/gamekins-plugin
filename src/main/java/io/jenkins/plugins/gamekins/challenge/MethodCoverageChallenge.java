package io.jenkins.plugins.gamekins.challenge;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.gamekins.util.JacocoUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MethodCoverageChallenge extends CoverageChallenge {

    final String methodName;
    final int lines;
    final int missedLines;

    public MethodCoverageChallenge(JacocoUtil.ClassDetails classDetails, String branch, FilePath workspace)
            throws IOException, InterruptedException {
        super(classDetails, branch, workspace);
        Random random = new Random();
        ArrayList<JacocoUtil.CoverageMethod> methods =
                JacocoUtil.getNotFullyCoveredMethodEntries(JacocoUtil.calculateCurrentFilePath(workspace,
                        classDetails.getJacocoMethodFile(), classDetails.getWorkspace()));
        if (methods.size() != 0) {
            JacocoUtil.CoverageMethod method = methods.get(random.nextInt(methods.size()));
            this.methodName = method.getMethodName();
            this.lines = method.getLines();
            this.missedLines = method.getMissedLines();
        } else {
            this.methodName = null;
            this.lines = 0;
            this.missedLines = 0;
        }
    }

    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener,
                            FilePath workspace) {
        FilePath jacocoMethodFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.getJacocoMethodFile(),
                        classDetails.getWorkspace()), this.branch);
        FilePath jacocoCSVFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.getJacocoCSVFile(),
                        classDetails.getWorkspace()), this.branch);

        try {
            if (!jacocoMethodFile.exists() || !jacocoCSVFile.exists()) {
                listener.getLogger().println("[Gamekins] JaCoCo method file " + jacocoMethodFile.getRemote()
                        + " exists " + jacocoMethodFile.exists());
                listener.getLogger().println("[Gamekins] JaCoCo csv file " + jacocoCSVFile.getRemote()
                        + " exists " + jacocoCSVFile.exists());
                return false;
            }

            ArrayList<JacocoUtil.CoverageMethod> methods = JacocoUtil.getMethodEntries(jacocoMethodFile);
            for (JacocoUtil.CoverageMethod method : methods) {
                if (method.getMethodName().equals(this.methodName)) {
                    if (method.getMissedLines() < this.missedLines) {
                        this.solved = System.currentTimeMillis();
                        this.solvedCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(
                                this.classDetails.getClassName(), jacocoCSVFile);
                        return true;
                    }
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(listener.getLogger());
        }
        return false;
    }

    @Override
    public boolean isSolvable(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener,
                              FilePath workspace) {
        if (!this.branch.equals(constants.get("branch"))) return true;
        FilePath jacocoMethodFile = JacocoUtil.calculateCurrentFilePath(workspace,
                this.classDetails.getJacocoMethodFile(), classDetails.getWorkspace());

        try {
            if (!jacocoMethodFile.exists()) {
                listener.getLogger().println("[Gamekins] JaCoCo method file "
                        + jacocoMethodFile.getRemote() + " exists " + jacocoMethodFile.exists());
                return true;
            }

            ArrayList<JacocoUtil.CoverageMethod> methods =
                    JacocoUtil.getMethodEntries(jacocoMethodFile);
            for (JacocoUtil.CoverageMethod method : methods) {
                if (method.getMethodName().equals(this.methodName)) {
                    return method.getMissedLines() > 0;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(listener.getLogger());
            return false;
        }
        return false;
    }

    @Override
    public int getScore() {
        return ((this.lines - this.missedLines) / (double) this.lines) > 0.8 ? 3 : 2;
    }

    @Override
    String getName() {
        return "MethodCoverageChallenge";
    }

    @Override
    public String toString() {
        return "Write a test to cover more lines of method " + this.methodName + " in class "
                + classDetails.getClassName() + " in package " + classDetails.getPackageName()
                + " (created for branch " + branch + ")";
    }

    public String getMethodName() {
        return this.methodName;
    }
}
