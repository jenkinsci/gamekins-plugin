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

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.file.SourceFileDetails
import org.gamekins.util.Constants
import org.gamekins.util.JacocoUtil
import org.gamekins.util.MutationUtil

/**
 * Specific [Challenge] to motivate the user to kill artificial mutants.
 *
 * @author Philipp Straubinger
 * @since 0.6
 */
class MutationChallenge(val details: SourceFileDetails, var data: MutationUtil.MutationData) : Challenge {

    private val codeSnippet: String = createCodeSnippet()
    private val created = System.currentTimeMillis()
    private var solved: Long = 0

    private fun createCodeSnippet(): String {
        if (data.lineNumber < 0) return ""
        if (details.jacocoSourceFile.exists()) {
            val javaHtmlPath = JacocoUtil.calculateCurrentFilePath(
                details.parameters.workspace, details.jacocoSourceFile, details.parameters.remote
            )
            val snippetElements = JacocoUtil.getLinesInRange(javaHtmlPath, data.lineNumber, 2)
            if (snippetElements.first == "") return ""

            //TODO: Mutant and Description
            return "<pre class='prettyprint linenums:${data.lineNumber - 1} mt-2'><code class='language-java'>" +
                    snippetElements.first +
                    "</code></pre>"
        }
        return ""
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is MutationChallenge) return false
        return other.details.packageName == this.details.packageName
                && other.details.fileName == this.details.fileName
                && other.data == this.data
    }

    override fun getParameters(): Constants.Parameters {
        return details.parameters
    }

    override fun getCreated(): Long {
        return created
    }

    override fun getName(): String {
        return "Mutation"
    }

    override fun getScore(): Int {
        return if (data.status == MutationUtil.MutationStatus.SURVIVED) 5 else 4
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun hashCode(): Int {
        var result = details.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + codeSnippet.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + solved.hashCode()
        return result
    }

    //TODO: Improve
    override fun isSolvable(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (details.parameters.branch != parameters.branch) return true

        return JacocoUtil.calculateCurrentFilePath(parameters.workspace, details.file).exists()
    }

    override fun isSolved(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        MutationUtil.executePIT(details, parameters, listener)
        val mutationReport = FilePath(parameters.workspace.channel,
            parameters.workspace.remote + "/target/pit-reports/mutations.xml")
        if (!mutationReport.exists()) return false
        val mutants = mutationReport.readToString().split("\n").filter { it.startsWith("<mutation ") }
        if (mutants.isEmpty()) return false
        val mutant = mutants.map { MutationUtil.MutationData(it) }.find { it == data }
        if (mutant != null) {
            data = mutant
            solved = System.currentTimeMillis()
            return true
        }

        return false
    }

    //TODO: Implement
    override fun printToXML(reason: String, indentation: String): String? {
        return ""
    }

    override fun toString(): String {
        return ("Write a test to kill the mutant at line <b>${data.lineNumber}</b> of method " +
                "<b>${data.mutatedMethod}()</b> in class <b>${details.fileName}</b> in package " +
                "<b>${details.packageName}</b> (created for branch ${details.parameters.branch})")
    }
}