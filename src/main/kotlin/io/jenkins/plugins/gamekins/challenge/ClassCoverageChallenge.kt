package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import io.jenkins.plugins.gamekins.util.JacocoUtil.calculateCoveredLines
import io.jenkins.plugins.gamekins.util.JacocoUtil.calculateCurrentFilePath
import io.jenkins.plugins.gamekins.util.JacocoUtil.generateDocument
import io.jenkins.plugins.gamekins.util.JacocoUtil.getCoverageInPercentageFromJacoco
import io.jenkins.plugins.gamekins.util.JacocoUtil.getJacocoFileInMultiBranchProject
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

    override fun getName(): String {
        return "ClassCoverageChallenge"
    }

    override fun getScore(): Int {
        return if (coverage >= 0.8) 2 else 1
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

        val jacocoSourceFile = calculateCurrentFilePath(workspace,
                classDetails.jacocoSourceFile, classDetails.workspace)
        val document: Document
        document = try {
            if (!jacocoSourceFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo source file "
                        + jacocoSourceFile.remote + " exists " + jacocoSourceFile.exists())
                return true
            }
            generateDocument(jacocoSourceFile)
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return false
        }

        return !(calculateCoveredLines(document, "pc") == 0
                && calculateCoveredLines(document, "nc") == 0)
    }

    /**
     * The [ClassCoverageChallenge] is solved if the coverage, according to the [classDetails] JaCoCo files, is higher
     * than during generation. The [workspace] is the folder with the code and execution rights, and the [listener]
     * reports the events to the console output of Jenkins.
     */
    override fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                          workspace: FilePath): Boolean {
        val jacocoSourceFile = getJacocoFileInMultiBranchProject(run, constants,
                calculateCurrentFilePath(workspace, classDetails.jacocoSourceFile,
                        classDetails.workspace), branch)
        val jacocoCSVFile = getJacocoFileInMultiBranchProject(run, constants,
                calculateCurrentFilePath(workspace, classDetails.jacocoCSVFile,
                        classDetails.workspace), branch)

        val document: Document
        document = try {
            if (!jacocoSourceFile.exists() || !jacocoCSVFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo source file " + jacocoSourceFile.remote
                        + " exists " + jacocoSourceFile.exists())
                listener.logger.println("[Gamekins] JaCoCo csv file " + jacocoCSVFile.remote
                        + " exists " + jacocoCSVFile.exists())
                return false
            }
            generateDocument(jacocoSourceFile)
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return false
        }

        val fullyCoveredLines = calculateCoveredLines(document, "fc")
        if (fullyCoveredLines > this.fullyCoveredLines) {
            solved = System.currentTimeMillis()
            solvedCoverage = getCoverageInPercentageFromJacoco(
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
