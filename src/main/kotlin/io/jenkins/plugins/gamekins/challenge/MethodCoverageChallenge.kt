package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.gamekins.util.JacocoUtil
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import io.jenkins.plugins.gamekins.util.JacocoUtil.CoverageMethod
import java.util.*

/**
 * Specific [Challenge] to motivate the user to cover more lines in a random method of a specific class.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class MethodCoverageChallenge(classDetails: ClassDetails, branch: String, workspace: FilePath,
                              method: CoverageMethod)
    : CoverageChallenge(classDetails, branch, workspace) {

    private val lines = method.lines
    private val methodName = method.methodName
    private val missedLines = method.missedLines

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is MethodCoverageChallenge) return false
        return other.classDetails.packageName == this.classDetails.packageName
                && other.classDetails.className == this.classDetails.className
                && other.methodName == this.methodName
    }

    override fun getName(): String {
        return "MethodCoverageChallenge"
    }

    override fun getScore(): Int {
        return if ((lines - missedLines) / lines.toDouble() > 0.8) 3 else 2
    }

    override fun hashCode(): Int {
        var result = lines
        result = 31 * result + methodName.hashCode()
        result = 31 * result + missedLines
        return result
    }

    /**
     * Checks whether the [MethodCoverageChallenge] is solvable if the [run] was in the [branch] (taken from
     * [constants]), where it has been generated. There must be uncovered or not fully covered lines left in the
     * method. The [workspace] is the folder with the code and execution rights, and the [listener] reports the events
     * to the console output of Jenkins.
     */
    override fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                            workspace: FilePath): Boolean {
        if (branch != constants["branch"]) return true

        val jacocoMethodFile = JacocoUtil.calculateCurrentFilePath(workspace,
                classDetails.jacocoMethodFile, classDetails.workspace)
        try {
            if (!jacocoMethodFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo method file "
                        + jacocoMethodFile.remote + JacocoUtil.EXISTS + jacocoMethodFile.exists())
                return true
            }

            val methods = JacocoUtil.getMethodEntries(jacocoMethodFile)
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

    /**
     * The [MethodCoverageChallenge] is solved if the number of missed lines, according to the [classDetails] JaCoCo
     * files, is less than during generation. The [workspace] is the folder with the code and execution rights, and
     * the [listener] reports the events to the console output of Jenkins.
     */
    override fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                          workspace: FilePath): Boolean {
        val jacocoMethodFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.jacocoMethodFile,
                        classDetails.workspace), branch)
        val jacocoCSVFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.jacocoCSVFile,
                        classDetails.workspace), branch)

        try {
            if (!jacocoMethodFile.exists() || !jacocoCSVFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo method file " + jacocoMethodFile.remote
                        + JacocoUtil.EXISTS + jacocoMethodFile.exists())
                listener.logger.println("[Gamekins] JaCoCo csv file " + jacocoCSVFile.remote
                        + JacocoUtil.EXISTS + jacocoCSVFile.exists())
                return false
            }

            val methods = JacocoUtil.getMethodEntries(jacocoMethodFile)
            for (method in methods) {
                if (method.methodName == methodName) {
                    if (method.missedLines < missedLines) {
                        super.setSolved(System.currentTimeMillis())
                        solvedCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(
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

    override fun toString(): String {
        return ("Write a test to cover more lines of method " + methodName + " in class "
                + classDetails.className + " in package " + classDetails.packageName
                + " (created for branch " + branch + ")")
    }
}
