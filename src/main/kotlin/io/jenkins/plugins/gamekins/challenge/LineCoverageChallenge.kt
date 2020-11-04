package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.gamekins.util.JacocoUtil
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*
import kotlin.math.abs

/**
 * Specific [Challenge] to motivate the user to cover a random line of a specific class.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class LineCoverageChallenge(classDetails: ClassDetails, branch: String, workspace: FilePath, line: Element)
    : CoverageChallenge(classDetails, branch, workspace) {

    private val coverageType: String = line.attr("class")
    private val currentCoveredBranches: Int
    private val lineContent: String = line.text()
    private val lineNumber: Int = line.attr("id").substring(1).toInt()
    private val maxCoveredBranches: Int

    init {
        val split = line.attr("title").split(" ".toRegex())
        when {
            split.isEmpty() || (split.size == 1 && split[0].isBlank()) -> {
                currentCoveredBranches = 0
                maxCoveredBranches = 1
            }
            line.attr("class").startsWith("pc") -> {
                currentCoveredBranches = split[2].toInt() - split[0].toInt()
                maxCoveredBranches = split[2].toInt()
            }
            else -> {
                currentCoveredBranches = 0
                maxCoveredBranches = split[1].toInt()
            }
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

        val jacocoSourceFile = JacocoUtil.calculateCurrentFilePath(workspace,
                classDetails.jacocoSourceFile, classDetails.workspace)
        val document: Document
        document = try {
            if (!jacocoSourceFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo source file "
                        + jacocoSourceFile.remote + " exists " + jacocoSourceFile.exists())
                return true
            }
            JacocoUtil.generateDocument(jacocoSourceFile)
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
        val jacocoSourceFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.jacocoSourceFile,
                        classDetails.workspace), branch)
        val jacocoCSVFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.jacocoCSVFile,
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
            JacocoUtil.generateDocument(jacocoSourceFile)
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return false
        }

        val elements = document.select("span." + "fc")
        elements.addAll(document.select("span." + "pc"))
        for (element in elements) {
            if (element.text() == lineContent && element.attr("id").substring(1).toInt() == lineNumber) {
                return setSolved(elements[0], jacocoCSVFile)
            }
        }

        elements.addAll(document.select("span." + "nc"))
        elements.removeIf { it.text() != lineContent }

        if (elements.isNotEmpty()) {
            if (elements.size == 1 && elements[0].attr("class") != "nc") {
                return setSolved(elements[0], jacocoCSVFile)
            } else {
                val nearestElement = elements.minByOrNull { abs(lineNumber - it.attr("id").substring(1).toInt()) }
                if (nearestElement != null && nearestElement.attr("class") != "nc") {
                    return setSolved(elements[0], jacocoCSVFile)
                }
            }
        }

        return false
    }

    /**
     * Checks whether the line [element] has more covered branches than during creation and sets the time and
     * coverage if solved.
     */
    private fun setSolved(element: Element, jacocoCSVFile: FilePath): Boolean {
        if (maxCoveredBranches > 1 && maxCoveredBranches - element.attr("title").split(" ".toRegex())[0].toInt()
                <= currentCoveredBranches) {
            return false
        }
        solved = System.currentTimeMillis()
        solvedCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(classDetails.className, jacocoCSVFile)
        return true
    }

    override fun toString(): String {
        //TODO: Add content of line
        val prefix =
                if (maxCoveredBranches > 1) "Write a test to cover more branches (currently $currentCoveredBranches " +
                        "of $maxCoveredBranches covered) of line "
                else "Write a test to fully cover line "
        return (prefix + lineNumber + " in class " + classDetails.className
                + " in package " + classDetails.packageName + " (created for branch " + branch + ")")
    }
}
