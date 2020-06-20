package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;
import io.jenkins.plugins.gamekins.util.JacocoUtil;

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
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run) {
        try {
            ArrayList<JacocoUtil.CoverageMethod> methods =
                    JacocoUtil.getMethodEntries(classDetails.getJacocoMethodFile());
            for (JacocoUtil.CoverageMethod method : methods) {
                if (method.getMethodName().equals(this.methodName)) {
                    if (method.getMissedLines() < this.missedLines) {
                        this.solved = System.currentTimeMillis();
                        this.solvedCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(
                                this.classDetails.getClassName(), this.classDetails.getJacocoCSVFile());
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
            ArrayList<JacocoUtil.CoverageMethod> methods =
                    JacocoUtil.getMethodEntries(classDetails.getJacocoMethodFile());
            for (JacocoUtil.CoverageMethod method : methods) {
                if (method.getMethodName().equals(this.methodName)) {
                    return method.getMissedLines() > 0;
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
        return "Write a test to cover more lines of method " + this.methodName + " in class "
                + classDetails.getClassName() + " in package " + classDetails.getPackageName()
                + " (created for branch " + branch + ")";
    }

}
