package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import io.jenkins.plugins.gamekins.util.JacocoUtil.calculateCurrentFilePath
import io.jenkins.plugins.gamekins.util.JacocoUtil.getCoverageInPercentageFromJacoco
import io.jenkins.plugins.gamekins.util.JacocoUtil.getJacocoFileInMultiBranchProject
import io.jenkins.plugins.gamekins.util.JacocoUtil.getMethodEntries
import io.jenkins.plugins.gamekins.util.JacocoUtil.getNotFullyCoveredMethodEntries
import java.io.IOException
import java.util.*

class MethodCoverageChallenge(classDetails: ClassDetails, branch: String, workspace: FilePath?)
    : CoverageChallenge(classDetails, branch, workspace) {

    var methodName: String? = null
    var lines = 0
    var missedLines = 0

    override fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                          workspace: FilePath): Boolean {
        val jacocoMethodFile = getJacocoFileInMultiBranchProject(run, constants,
                calculateCurrentFilePath(workspace, classDetails.jacocoMethodFile,
                        classDetails.workspace), branch)
        val jacocoCSVFile = getJacocoFileInMultiBranchProject(run, constants,
                calculateCurrentFilePath(workspace, classDetails.jacocoCSVFile,
                        classDetails.workspace), branch)
        try {
            if (!jacocoMethodFile.exists() || !jacocoCSVFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo method file " + jacocoMethodFile.remote
                        + " exists " + jacocoMethodFile.exists())
                listener.logger.println("[Gamekins] JaCoCo csv file " + jacocoCSVFile.remote
                        + " exists " + jacocoCSVFile.exists())
                return false
            }
            val methods = getMethodEntries(jacocoMethodFile)
            for (method in methods) {
                if (method.methodName == methodName) {
                    if (method.missedLines < missedLines) {
                        solved = System.currentTimeMillis()
                        solvedCoverage = getCoverageInPercentageFromJacoco(
                                classDetails.className, jacocoCSVFile)
                        return true
                    }
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
        }
        return false
    }

    override fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                            workspace: FilePath): Boolean {
        if (branch != constants["branch"]) return true
        val jacocoMethodFile = calculateCurrentFilePath(workspace,
                classDetails.jacocoMethodFile, classDetails.workspace)
        try {
            if (!jacocoMethodFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo method file "
                        + jacocoMethodFile.remote + " exists " + jacocoMethodFile.exists())
                return true
            }
            val methods = getMethodEntries(jacocoMethodFile)
            for (method in methods) {
                if (method.methodName == methodName) {
                    return method.missedLines > 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return false
        }
        return false
    }

    override fun getScore(): Int {
        return if ((lines - missedLines) / lines.toDouble() > 0.8) 3 else 2
    }

    override fun getName(): String {
        return "MethodCoverageChallenge"
    }

    override fun toString(): String {
        return ("Write a test to cover more lines of method " + methodName + " in class "
                + classDetails.className + " in package " + classDetails.packageName
                + " (created for branch " + branch + ")")
    }

    init {
        val random = Random()
        val methods = getNotFullyCoveredMethodEntries(calculateCurrentFilePath(workspace!!,
                classDetails.jacocoMethodFile, classDetails.workspace))
        if (methods.size != 0) {
            val method = methods[random.nextInt(methods.size)]
            methodName = method.methodName
            lines = method.lines
            missedLines = method.missedLines
        } else {
            methodName = null
            lines = 0
            missedLines = 0
        }
    }
}
