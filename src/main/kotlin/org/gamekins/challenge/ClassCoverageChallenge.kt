/*
 * Copyright 2022 Gamekins contributors
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

import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.file.SourceFileDetails
import org.gamekins.util.Constants
import org.gamekins.util.JacocoUtil
import org.jsoup.nodes.Document

/**
 * Specific [Challenge] to motivate the user to cover more lines in a specific class.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class ClassCoverageChallenge(data: Challenge.ChallengeGenerationData)
    : CoverageChallenge(data.selectedFile as SourceFileDetails, data.parameters.workspace) {

    init {
        //TODO: Optimize for Kotlin objects
        codeSnippet = createCodeSnippet(data.selectedFile as SourceFileDetails, "class ${data.selectedFile.fileName}",
            data.parameters.workspace)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is ClassCoverageChallenge) return false
        return other.details.packageName == this.details.packageName
                && other.details.fileName == this.details.fileName
    }

    override fun getName(): String {
        return "Class Coverage"
    }

    override fun getSnippet(): String {
        return codeSnippet
    }

    override fun getScore(): Int {
        return if (coverage >= 0.8) 2 else 1
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    /**
     * Checks whether the [ClassCoverageChallenge] is solvable if the [run] was in the branch (taken from
     * [parameters]), where it has been generated. There must be uncovered or not fully covered lines left in the class.
     * The workspace in [parameters] is the folder with the code and execution rights, and the [listener] reports the
     * events to the console output of Jenkins.
     */
    override fun isSolvable(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (details.parameters.branch != parameters.branch) return true

        val jacocoSourceFile = JacocoUtil.calculateCurrentFilePath(parameters.workspace,
                details.jacocoSourceFile, details.parameters.remote)
        val document: Document = try {
            if (!jacocoSourceFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo source file "
                        + jacocoSourceFile.remote + Constants.EXISTS + jacocoSourceFile.exists())
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
     * The [ClassCoverageChallenge] is solved if the coverage, according to the [details] JaCoCo files, is higher
     * than during generation. The workspace in [parameters] is the folder with the code and execution rights, and the
     * [listener] reports the events to the console output of Jenkins.
     */
    override fun isSolved(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        val jacocoSourceFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, parameters,
                JacocoUtil.calculateCurrentFilePath(parameters.workspace, details.jacocoSourceFile,
                        details.parameters.remote), details.parameters.branch)
        val jacocoCSVFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, parameters,
                JacocoUtil.calculateCurrentFilePath(parameters.workspace, details.jacocoCSVFile,
                        details.parameters.remote), details.parameters.branch)

        val document = JacocoUtil.generateDocument(jacocoSourceFile, jacocoCSVFile, listener) ?: return false

        val fullyCoveredLines = JacocoUtil.calculateCoveredLines(document, "fc")
        if (fullyCoveredLines > this.fullyCoveredLines) {
            super.setSolved(System.currentTimeMillis())
            solvedCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(
                    details.fileName, jacocoCSVFile)
            return true
        }

        return false
    }

    /**
     * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
     */
    @Suppress("unused", "SENSELESS_COMPARISON")
    private fun readResolve(): Any {
        if (details == null && classDetails != null) {
            details = SourceFileDetails.classDetailsToSourceFileDetails(classDetails)
        }

        return this
    }

    override fun toString(): String {
        return ("Write a test to cover more lines in class <b>" + details.fileName
                + "</b> in package <b>" + details.packageName + "</b> (created for branch "
                + details.parameters.branch + ")")
    }
}
