package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.gamekins.util.JacocoUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MethodCoverageChallenge extends CoverageChallenge {

    final String methodName;
    final int lines;
    final int missedLines;

    public MethodCoverageChallenge(JacocoUtil.ClassDetails classDetails, String branch) throws IOException {
        super(classDetails, branch);
        Random random = new Random();
        ArrayList<JacocoUtil.CoverageMethod> methods =
                JacocoUtil.getNotFullyCoveredMethodEntries(classDetails.getJacocoMethodFile());
        JacocoUtil.CoverageMethod method = methods.get(random.nextInt(methods.size()));
        this.methodName = method.getMethodName();
        this.lines = method.getLines();
        this.missedLines = method.getMissedLines();
    }

    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener) {
        File jacocoMethodFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                classDetails.getJacocoMethodFile(), this.branch);
        File jacocoCSVFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                classDetails.getJacocoCSVFile(), this.branch);
        if (!jacocoMethodFile.exists() || !jacocoCSVFile.exists()) {
            listener.getLogger().println("[Gamekins] JaCoCo method file " + jacocoMethodFile.getAbsolutePath()
                    + " exists " + jacocoMethodFile.exists());
            listener.getLogger().println("[Gamekins] JaCoCo csv file " + jacocoCSVFile.getAbsolutePath()
                    + " exists " + jacocoCSVFile.exists());
            return false;
        }
        try {
            ArrayList<JacocoUtil.CoverageMethod> methods = JacocoUtil.getMethodEntries(jacocoMethodFile);
            for (JacocoUtil.CoverageMethod method : methods) {
                if (method.getMethodName().equals(this.methodName)) {
                    if (method.getMissedLines() < this.missedLines) {
                        this.solved = System.currentTimeMillis();
                        this.solvedCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(
                                this.classDetails.getClassName(), jacocoCSVFile);
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace(listener.getLogger());
        }
        return false;
    }

    @Override
    public boolean isSolvable(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener) {
        if (!this.branch.equals(constants.get("branch"))) return true;
        if (!this.classDetails.getJacocoMethodFile().exists()) {
            listener.getLogger().println("[Gamekins] JaCoCo method file "
                    + this.classDetails.getJacocoMethodFile().getAbsolutePath()
                    + " exists " + this.classDetails.getJacocoMethodFile().exists());
            return true;
        }
        try {
            ArrayList<JacocoUtil.CoverageMethod> methods =
                    JacocoUtil.getMethodEntries(classDetails.getJacocoMethodFile());
            for (JacocoUtil.CoverageMethod method : methods) {
                if (method.getMethodName().equals(this.methodName)) {
                    return method.getMissedLines() > 0;
                }
            }
        } catch (IOException e) {
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
    public String toString() {
        return "Write a test to cover more lines of method " + this.methodName + " in class "
                + classDetails.getClassName() + " in package " + classDetails.getPackageName()
                + " (created for branch " + branch + ")";
    }

}
