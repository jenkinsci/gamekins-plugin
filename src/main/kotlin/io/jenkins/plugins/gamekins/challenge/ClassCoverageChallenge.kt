package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.gamekins.util.JacocoUtil
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import org.jsoup.nodes.Document
import java.util.*

/**
 * Specific [Challenge] to motivate the user to cover more lines in a specific class.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class ClassCoverageChallenge(classDetails: ClassDetails, branch: String, workspace: FilePath?)
    : CoverageChallenge(classDetails, branch, workspace) {

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is ClassCoverageChallenge) return false
        return other.classDetails.packageName == this.classDetails.packageName
                && other.classDetails.className == this.classDetails.className
    }

    override fun getName(): String {
        return "ClassCoverageChallenge"
    }

    override fun getScore(): Int {
        return if (coverage >= 0.8) 2 else 1
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    /**
     * Checks whether the [ClassCoverageChallenge] is solvable if the [run] was in the [branch] (taken from
     * [constants]), where it has been generated. There must be uncovered or not fully covered lines left in the class.
     * The [workspace] is the folder with the code and execution rights, and the [listener] reports the events to the
     * console output of Jenkins.
     */
    override fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                            workspace: FilePath): Boolean {
        if (branch != constants["branch"]) return true

        val jacocoSourceFile = JacocoUtil.calculateCurrentFilePath(workspace,
                classDetails.jacocoSourceFile, classDetails.workspace)
        val document: Document
        document = try {
            if (!jacocoSourceFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo source file "
                        + jacocoSourceFile.remote + JacocoUtil.EXISTS + jacocoSourceFile.exists())
                return true
            }
            JacocoUtil.generateDocument(jacocoSourceFile)
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return false
        }

        return !(JacocoUtil.calculateCoveredLines(document, "pc") == 0
                && JacocoUtil.calculateCoveredLines(document, "nc") == 0)
    }

    /**
     * The [ClassCoverageChallenge] is solved if the coverage, according to the [classDetails] JaCoCo files, is higher
     * than during generation. The [workspace] is the folder with the code and execution rights, and the [listener]
     * reports the events to the console output of Jenkins.
     */
    override fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                          workspace: FilePath): Boolean {
        val jacocoSourceFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.jacocoSourceFile,
                        classDetails.workspace), branch)
        val jacocoCSVFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.jacocoCSVFile,
                        classDetails.workspace), branch)

        val document = JacocoUtil.generateDocument(jacocoSourceFile, jacocoCSVFile, listener) ?: return false

        val fullyCoveredLines = JacocoUtil.calculateCoveredLines(document, "fc")
        if (fullyCoveredLines > this.fullyCoveredLines) {
            super.setSolved(System.currentTimeMillis())
            solvedCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(
                    classDetails.className, jacocoCSVFile)
            return true
        }

        return false
    }

    override fun toString(): String {
        return ("Write a test to cover more lines in class " + classDetails.className
                + " in package " + classDetails.packageName + " (created for branch " + branch + ")")
    }
}
