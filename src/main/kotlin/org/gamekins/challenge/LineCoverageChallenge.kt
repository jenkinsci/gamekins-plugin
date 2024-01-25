/*
 * Copyright 2023 Gamekins contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.file.SourceFileDetails
import org.gamekins.gumTree.GumTree
import org.gamekins.util.Constants
import org.gamekins.util.JacocoUtil
import org.gamekins.util.JacocoUtil.replaceSpecialEntities

/**
 * Specific [Challenge] to motivate the user to cover a random line of a specific class.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class LineCoverageChallenge(data: Challenge.ChallengeGenerationData)
    : CoverageChallenge(data.selectedFile as SourceFileDetails, data.parameters.workspace) {

    private val coverageType: String = data.line!!.attr("class")
    private var lineContent: String = replaceSpecialEntities(data.line!!.text())
    private var lineNumber: Int = data.line!!.attr("id").substring(1).toInt()
    private var sourceCode = generateCompilationUnit(details.parameters,
        "${details.packageName}.${details.fileName}", "${details.fileName}.${details.fileExtension}")


    init {
        codeSnippet = LineCoverageChallenge.createCodeSnippet(details, lineNumber,  data.parameters.workspace)
    }

    companion object {
        fun createCodeSnippet(classDetails: SourceFileDetails,
                                       target: Any, workspace: FilePath): String {
            if (target !is Int) return ""
            else if (target < 0) return ""
            if (classDetails.jacocoSourceFile.exists()) {
                val javaHtmlPath = JacocoUtil.calculateCurrentFilePath(
                    workspace, classDetails.jacocoSourceFile, classDetails.parameters.remote
                )
                val snippetElements = JacocoUtil.getLinesInRange(javaHtmlPath, target, 2)
                if (snippetElements.first == "") return ""

                return "<pre class='prettyprint linenums:${target - 1} mt-2'><code class='language-java'>" +
                        snippetElements.first +
                        "</code></pre>"
            }
            return ""
        }
    }



    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is LineCoverageChallenge) return false
        return other.details.packageName == this.details.packageName
                && other.details.fileName == this.details.fileName
                && other.lineNumber == this.lineNumber
                && other.lineContent == this.lineContent
    }

    override fun getName(): String {
        return "Line Coverage"
    }

    override fun getScore(): Int {
        return if (coverage >= 0.8 || coverageType == "pc") 3 else 2
    }

    override fun getSnippet(): String {
        return codeSnippet.ifEmpty { "Code snippet is not available" }
    }

    override fun getToolTipText(): String {
        return "Line content: ${lineContent.trim()}"
    }

    override fun hashCode(): Int {
        var result = coverageType.hashCode()
        result = 31 * result + lineContent.hashCode()
        result = 31 * result + lineNumber
        return result
    }

    /**
     * Checks whether the [LineCoverageChallenge] is solvable if the [run] was in the branch (taken from
     * [parameters]), where it has been generated. The line must not be covered and still be in the class.
     * The workspace is the folder with the code and execution rights, and the [listener] reports the events to the
     * console output of Jenkins.
     */
    override fun isSolvable(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (details.parameters.branch != parameters.branch) return true
        if (!details.update(parameters).filesExists()) return false

        if (GumTree.findMapping(sourceCode,
                "${details.packageName}.${details.fileName}",
                "${details.fileName}.${details.fileExtension}", lineNumber, parameters) == 0) return false

        val jacocoSourceFile = JacocoUtil.calculateCurrentFilePath(parameters.workspace, details.jacocoSourceFile,
                details.parameters.remote)
        val jacocoCSVFile = JacocoUtil.calculateCurrentFilePath(parameters.workspace, details.jacocoCSVFile,
                details.parameters.remote)
        if (!jacocoSourceFile.exists() || !jacocoCSVFile.exists()) return true

        val document = JacocoUtil.generateDocument(jacocoSourceFile, jacocoCSVFile, listener) ?: return false

        val elements = document.select("span." + "nc")

        return elements.any { replaceSpecialEntities(it.text().trim()) == replaceSpecialEntities(lineContent.trim()) }
    }

    /**
     * The [LineCoverageChallenge] is solved if the line, according to the [details] JaCoCo files, is fully
     * covered or partially covered (only if it was uncovered during generation). The workspace is the folder with
     * the code and execution rights, and the [listener] reports the events to the console output of Jenkins.
     */
    override fun isSolved(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        details.update(parameters)
        updateLine(parameters, parameters.workspace)

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
            if (replaceSpecialEntities(element.text().trim()) == replaceSpecialEntities(lineContent.trim())
                && element.attr("id").substring(1).toInt() == lineNumber) {
                super.setSolved(System.currentTimeMillis())
                solvedCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(details.fileName, jacocoCSVFile)
                return true
            }
        }

        return false
    }

    override fun isToolTip(): Boolean {
        return true
    }

    /**
     * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
     */
    @Suppress("unused")
    private fun readResolve(): Any {
        if (sourceCode.isNullOrEmpty()) {
            sourceCode = generateCompilationUnit(details.parameters,
                "${details.packageName}.${details.fileName}", "${details.fileName}.${details.fileExtension}")
        }

        return this
    }

    override fun toString(): String {
        return ("Write a test to cover line " + "<b>" + lineNumber + "</b> in class <b>" + details.fileName
                + "</b> in package <b>" + details.packageName + "</b> (created for branch "
                + details.parameters.branch + ")")
    }

    /**
     * Updates [lineNumber], [lineContent], [sourceCode] and [codeSnippet] based on the GumTree implementation if
     * there was a change since the last build.
     */
    private fun updateLine(parameters: Constants.Parameters, workspace: FilePath) {
        val newLineNumber = GumTree.findMapping(sourceCode,
            "${details.packageName}.${details.fileName}",
            "${details.fileName}.${details.fileExtension}", lineNumber, parameters)
        if (newLineNumber == 0) return
        lineNumber = newLineNumber
        val javaHtmlPath = JacocoUtil.calculateCurrentFilePath(
            workspace, details.jacocoSourceFile, details.parameters.remote
        )
        if (!javaHtmlPath.exists()) return
        lineContent = JacocoUtil.getLinesInRange(javaHtmlPath, lineNumber, 0).first
        codeSnippet = LineCoverageChallenge.createCodeSnippet(details, lineNumber, workspace)
        sourceCode = generateCompilationUnit(details.parameters,
            "${details.packageName}.${details.fileName}", "${details.fileName}.${details.fileExtension}")
    }
}
