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
import org.gamekins.util.MutationUtil.MutationStatus

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

            var mutatedCode = MutationUtil.getMutatedCode(
                JacocoUtil.getLinesInRange(javaHtmlPath, data.lineNumber, 0).first, data)
            mutatedCode = if (mutatedCode.isEmpty()) {
                "<br><em>No mutated line available</em><br>"
            } else {
                "<pre class='prettyprint linenums:${data.lineNumber} " +
                        "mt-2'><code class='language-java'>$mutatedCode</code></pre>"
            }
            return "Write or update tests so that they fail on the mutant described below.\n" +
                    "Original code snippet\n" +
                    "<pre class='prettyprint linenums:${data.lineNumber - 1} mt-2'><code class='language-java'>" +
                    "${snippetElements.first}</code></pre>" +
                    "Mutated line of code \n" + mutatedCode +
                    "The mutated line is built from information provided by PIT and could be syntactically " +
                    "invalid or wrong. Please use along with the description in that case:<br>" +
                    "<a href=\"https://pitest.org/quickstart/mutators/#${data.mutator.name}\"target=\"_blank\">" +
                    "${data.description}</a> "
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

    override fun getCreated(): Long {
        return created
    }

    fun getKillingTest() : String {
        if (data.killingTest.isEmpty()) return ""
        val cla = "\\[class:(.*)]/".toRegex().find(data.killingTest)?.groupValues?.get(1)
        val method = "\\[method:(.*)]".toRegex().find(data.killingTest)?.groupValues?.get(1)
        return "$cla.$method"
    }

    override fun getName(): String {
        return "Mutation"
    }

    override fun getParameters(): Constants.Parameters {
        return details.parameters
    }

    override fun getScore(): Int {
        return if (data.status == MutationStatus.SURVIVED) 5 else 4
    }

    override fun getSnippet(): String {
        return codeSnippet
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

    override fun isSolvable(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (details.parameters.branch != parameters.branch) return true
        val mutationReport = FilePath(parameters.workspace.channel,
            parameters.workspace.remote + "/target/pit-reports/mutations.xml")
        if (!mutationReport.exists()) return true

        val mutant = MutationUtil.getMutant(this.data, parameters)
        return (mutant != null && !mutant.detected)
    }

    override fun isSolved(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        MutationUtil.executePIT(details, parameters, listener)

        val mutant = MutationUtil.getMutant(this.data, parameters)
        if (mutant != null && mutant.detected && mutant.status == MutationStatus.KILLED) {
            data = mutant
            solved = System.currentTimeMillis()
            return true
        }

        return false
    }

    //TODO: Implement
    override fun printToXML(reason: String, indentation: String): String {
        return ""
    }

    override fun toString(): String {
        val method = if (data.mutatedMethod == "&lt;init&gt;") data.mutatedClass.split(".").last() else data.mutatedMethod
        return ("Write a test to kill the mutant at line <b>${data.lineNumber}</b> of method " +
                "<b>$method()</b> in class <b>${details.fileName}</b> in package " +
                "<b>${details.packageName}</b> (created for branch ${details.parameters.branch})")
    }
}