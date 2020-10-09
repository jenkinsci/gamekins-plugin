package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import io.jenkins.plugins.gamekins.util.JacocoUtil.calculateCurrentFilePath
import io.jenkins.plugins.gamekins.util.JacocoUtil.generateDocument
import io.jenkins.plugins.gamekins.util.JacocoUtil.getCoverageInPercentageFromJacoco
import io.jenkins.plugins.gamekins.util.JacocoUtil.getJacocoFileInMultiBranchProject
import io.jenkins.plugins.gamekins.util.JacocoUtil.getLines
import org.jsoup.nodes.Document
import java.util.*

/**
 * Specific [Challenge] to motivate the user to cover a random line of a specific class.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class LineCoverageChallenge(classDetails: ClassDetails, branch: String, workspace: FilePath?)
    : CoverageChallenge(classDetails, branch, workspace) {

    private var coverageType: String? = null
    internal var lineContent: String? = null
    private var lineNumber = 0

    /**
     * Chooses a random uncovered or partially covered line from the current class.
     */
    init {
        val elements = getLines(calculateCurrentFilePath(workspace!!,
                classDetails.jacocoSourceFile, classDetails.workspace))
        val random = Random()
        if (elements.size != 0) {
            val element = elements[random.nextInt(elements.size)]
            lineNumber = element.attr("id").substring(1).toInt()
            coverageType = element.attr("class")
            lineContent = element.text()
        } else {
            lineNumber = 0
            coverageType = null
            lineContent = null
        }
    }

    override fun getName(): String {
        return "LineCoverageChallenge"
    }

    override fun getScore(): Int {
        return if (coverage >= 0.8 || coverageType == "pc") 3 else 2
    }

    /**
     * Checks whether the [LineCoverageChallenge] is solvable if the [run] was in the [branch] (taken from
     * [constants]), where it has been generated. The line must not be covered and still be in the class.
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

        val elements = document.select("span." + "pc")
        elements.addAll(document.select("span." + "nc"))
        for (element in elements) {
            if (element.text() == lineContent) {
                return true
            }
        }

        return false
    }

    /**
     * The [LineCoverageChallenge] is solved if the line, according to the [classDetails] JaCoCo files, is fully
     * covered or partially covered (only if it was uncovered during generation). The [workspace] is the folder with
     * the code and execution rights, and the [listener] reports the events to the console output of Jenkins.
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

        //TODO: Case with more than two branches (bnc/bpc/bfc)
        val elements = document.select("span." + "fc")
        if (coverageType!!.startsWith("nc")) elements.addAll(document.select("span." + "pc"))
        for (element in elements) {
            //TODO: Some content in multiple lines in one class
            if (element.text() == lineContent) {
                solved = System.currentTimeMillis()
                solvedCoverage = getCoverageInPercentageFromJacoco(classDetails.className,
                        jacocoCSVFile)
                return true
            }
        }

        return false
    }

    override fun toString(): String {
        //TODO: Add content of line
        //TODO: Fully cover / cover more branches
        val prefix =
                if (coverageType!!.startsWith("nc")) "Write a test to cover more branches of line "
                else "Write a test to fully cover line "
        return (prefix + lineNumber + " in class " + classDetails.className
                + " in package " + classDetails.packageName + " (created for branch " + branch + ")")
    }
}
