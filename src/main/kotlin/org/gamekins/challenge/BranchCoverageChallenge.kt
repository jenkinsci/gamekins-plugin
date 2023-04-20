package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.file.SourceFileDetails
import org.gamekins.util.Constants
import org.gamekins.util.JacocoUtil
import org.jsoup.nodes.Element
import kotlin.math.abs

class BranchCoverageChallenge(data: Challenge.ChallengeGenerationData)
    : CoverageChallenge(data.selectedFile as SourceFileDetails, data.parameters.workspace) {

    private val currentCoveredBranches: Int
    private val lineContent: String = data.line!!.text()
    private val lineNumber: Int = data.line!!.attr("id").substring(1).toInt()
    private val maxCoveredBranches: Int
    private var solvedCoveredBranches: Int = 0

    init {
        codeSnippet = LineCoverageChallenge.createCodeSnippet(details, lineNumber,  data.parameters.workspace)
        val split = data.line!!.attr("title").split(" ".toRegex())
        when {
            split.isEmpty() || (split.size == 1 && split[0].isBlank()) -> {
                currentCoveredBranches = 0
                maxCoveredBranches = 1
            }
            data.line!!.attr("class").startsWith("pc") -> {
                currentCoveredBranches = split[2].toInt() - split[0].toInt()
                maxCoveredBranches = split[2].toInt()
            }
            else -> {
                currentCoveredBranches = 0
                maxCoveredBranches = split[1].toInt()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is BranchCoverageChallenge) return false
        return other.details.packageName == this.details.packageName
                && other.details.fileName == this.details.fileName
                && other.lineNumber == this.lineNumber
                && other.lineContent == this.lineContent
    }

    fun getMaxCoveredBranchesIfFullyCovered(): Int {
        return if (solvedCoveredBranches == maxCoveredBranches) maxCoveredBranches else 0
    }

    override fun getName(): String {
        return "Branch Coverage"
    }

    override fun getScore(): Int {
        return 3
    }

    override fun getSnippet(): String {
        return codeSnippet.ifEmpty { "Code snippet is not available" }
    }

    override fun getToolTipText(): String {
        return "Line content: ${lineContent.trim()}"
    }

    override fun hashCode(): Int {
        var result = currentCoveredBranches
        result = 31 * result + lineContent.hashCode()
        result = 31 * result + lineNumber
        result = 31 * result + maxCoveredBranches
        result = 31 * result + solvedCoveredBranches
        return result
    }

    override fun isSolvable(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (details.parameters.branch != parameters.branch) return true
        if (!details.update(parameters).filesExists()) return false

        val jacocoSourceFile = JacocoUtil.calculateCurrentFilePath(parameters.workspace, details.jacocoSourceFile,
            details.parameters.remote)
        val jacocoCSVFile = JacocoUtil.calculateCurrentFilePath(parameters.workspace, details.jacocoCSVFile,
            details.parameters.remote)
        if (!jacocoSourceFile.exists() || !jacocoCSVFile.exists()) return true

        val document = JacocoUtil.generateDocument(jacocoSourceFile, jacocoCSVFile, listener) ?: return false

        val elements = document.select("span." + "pc")

        return elements.any { it.text().trim() == lineContent.trim() }
    }

    override fun isSolved(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        val jacocoSourceFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, parameters,
            JacocoUtil.calculateCurrentFilePath(parameters.workspace, details.jacocoSourceFile,
                details.parameters.remote), details.parameters.branch)
        val jacocoCSVFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, parameters,
            JacocoUtil.calculateCurrentFilePath(parameters.workspace, details.jacocoCSVFile,
                details.parameters.remote), details.parameters.branch)

        val document = JacocoUtil.generateDocument(jacocoSourceFile, jacocoCSVFile, listener) ?: return false

        val elements = document.select("span." + "fc")
        elements.addAll(document.select("span." + "pc"))
        for (element in elements) {
            if (element.text().trim() == lineContent.trim() && element.attr("id").substring(1).toInt() == lineNumber) {
                return setSolved(element, jacocoCSVFile)
            }
        }

        elements.removeIf { it.text().trim() != lineContent.trim() }
        if (elements.isNotEmpty()) {
            if (elements.size == 1 && elements[0].attr("class") != "nc") {
                return setSolved(elements[0], jacocoCSVFile)
            } else {
                val nearestElement = elements.minByOrNull { abs(lineNumber - it.attr("id").substring(1).toInt()) }
                if (nearestElement != null && nearestElement.attr("class") != "nc") {
                    return setSolved(nearestElement, jacocoCSVFile)
                }
            }
        }

        return false
    }

    override fun isToolTip(): Boolean {
        return true
    }

    /**
     * Checks whether the line [element] has more covered branches than during creation and sets the time and
     * coverage if solved.
     */
    private fun setSolved(element: Element, jacocoCSVFile: FilePath): Boolean {
        val split = element.attr("title").split(" ".toRegex())
        val title = split[0]
        if (split.size >= 4 && split[3] == "missed.") return false
        if (title != "All"
            && maxCoveredBranches > 1 && maxCoveredBranches - title.toInt() <= currentCoveredBranches) {
            return false
        }

        solvedCoveredBranches = when (title) {
            "All" -> maxCoveredBranches
            "" -> maxCoveredBranches
            else -> maxCoveredBranches - title.toInt()
        }
        super.setSolved(System.currentTimeMillis())
        solvedCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(details.fileName, jacocoCSVFile)
        return true
    }

    override fun toString(): String {
        return ("Write a test to cover more branches (currently $currentCoveredBranches "
                + "of $maxCoveredBranches covered) of line " + "<b>" + lineNumber + "</b> in class <b>"
                + details.fileName + "</b> in package <b>" + details.packageName + "</b> (created for branch "
                + details.parameters.branch + ")")
    }
}