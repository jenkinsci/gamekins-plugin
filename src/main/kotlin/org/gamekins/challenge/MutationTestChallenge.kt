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
import org.gamekins.mutation.MutationInfo
import org.gamekins.mutation.MutationResults
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.GitUtil
import org.gamekins.util.JacocoUtil


/**
 * Specific [Challenge] to motivate users to write test cases to kill a generated mutant.
 *
 * @author Tran Phan
 * @since 0.3
 */
class MutationTestChallenge(
    val mutationInfo: MutationInfo, var details: SourceFileDetails,
    val branch: String?, val commitID: String, snippet: String, mutatedLoc: String
) : Challenge {

    @Deprecated("Use implementation of new file structure", replaceWith = ReplaceWith("details"))
    val classDetails: JacocoUtil.ClassDetails? = null
    private val created = System.currentTimeMillis()
    private var solved: Long = 0
    private var mutationDetails = mutationInfo.mutationDetails
    val methodName = mutationDetails.methodInfo["methodName"]
    val className = mutationDetails.methodInfo["className"]?.replace("/", ".")
    private val mutationDescription = mutationDetails.mutationDescription
    val lineOfCode = mutationDetails.loc
    private val fileName = mutationDetails.fileName
    val uniqueID = mutationInfo.uniqueID
    private val codeSnippet = snippet
    private val mutatedLine = mutatedLoc
    private var killedByTest = mutationInfo.killedByTest

    companion object {
        fun createCodeSnippet(details: SourceFileDetails, lineOfCode: Int, workspace: FilePath): Pair<String, String> {
            if (lineOfCode < 0) {
                return Pair("", "")
            }
            if (details.jacocoSourceFile.exists()) {
                val javaHtmlPath = JacocoUtil.calculateCurrentFilePath(
                    workspace, details.jacocoSourceFile, details.parameters.remote
                )
                val snippetElements = JacocoUtil.getLinesInRange(javaHtmlPath, lineOfCode, 4)
                if (snippetElements.first == "") {
                    return Pair("", "")
                }
                return Pair(
                    "<pre class='prettyprint linenums:${lineOfCode - 2} mt-2'><code class='language-java'>" +
                            snippetElements.first + "</code></pre>", snippetElements.second
                )
            }
            return Pair("", "")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is MutationTestChallenge) return false
        return other.mutationInfo == this.mutationInfo
    }

    /**
     * Returns the constants provided during creation. Must include entries for "projectName", "branch", "workspace",
     * "jacocoResultsPath" and "jacocoCSVPath".
     */
    override fun getParameters(): Parameters {
        return details.parameters
    }

    override fun getCreated(): Long {
        return created
    }

    fun getFileName(): String {
        return if (codeSnippet.isNotEmpty()) fileName else ""
    }

    override fun getHighlightedFileContent(): String {
        return "<pre class='prettyprint mt-2 linenums:1'><code class='language-java'>" +
                details.contents() + "</code></pre>"
    }

    fun getKilledByTest(): String {
        return killedByTest
    }

    fun getMutatedLine(): String {
        return mutatedLine
    }

    fun getMutationDescription(): String {
        return mutationDescription
    }

    override fun getName(): String {
        return "Mutation"
    }

    override fun getScore(): Int {
        return when {
            details.coverage < 0.8 -> 3
            details.coverage < 1.0 -> 4
            else -> 5
        }
    }

    override fun getSnippet(): String {
        return codeSnippet
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun hashCode(): Int {
        return this.mutationInfo.hashCode()
    }

    /**
     * Checks whether the [MutationTestChallenge] is solvable if the [run] was in the [branch] (taken from
     * [parameters]), where it has been generated. The workspace is the folder with the code and execution rights,
     * and the [listener] reports the events to the console output of Jenkins.
     *
     * A mutation challenge is discarded if its source file has changed (using git info)
     */
    override fun isSolvable(
        parameters: Parameters, run: Run<*, *>, listener: TaskListener
    ): Boolean {
        val changedClasses = parameters.workspace.act(
            GitUtil.DiffFromHeadCallable(
                parameters.workspace, commitID,
                details.packageName
            )
        )
        return changedClasses?.any {
            it.replace("/", ".") ==
                    "${details.packageName}.${details.fileName}"
        } == false
    }

    /**
     * The [MutationTestChallenge] is solved if mutation status is killed.
     * The workspace is the folder with the code and execution rights, and
     * the [listener] reports the events to the console output of Jenkins.
     *
     * A mutation challenge is solved if it exists in MoCo JSON file and has result as "killed"
     *
     */
    override fun isSolved(
        parameters: Parameters, run: Run<*, *>, listener: TaskListener
    ): Boolean {
        if (details.mocoJSONFile == null) return false
        val jsonFilePath = JacocoUtil.calculateCurrentFilePath(
            parameters.workspace, details.mocoJSONFile!!, details.parameters.remote
        )
        val mutationResults = MutationResults.retrievedMutationsFromJson(jsonFilePath, listener)
        val filteredByClass = mutationResults?.entries?.filter { it.key == this.className }
        if (!filteredByClass.isNullOrEmpty()) {

            val foundInfo = filteredByClass[this.className]?.find {
                (it.uniqueID == uniqueID || it.mutationDetails == mutationDetails) && it.result == "killed"
            }

            var solved = false
            if (foundInfo != null) {
                mutationInfo.killedByTest = foundInfo.killedByTest
                killedByTest = foundInfo.killedByTest
                solved = true
            }

            return solved
        }
        return false
    }

    override fun printToXML(reason: String, indentation: String): String {
        val mName = if (methodName == "<init>") "init" else methodName
        var print = (indentation + "<" + this::class.simpleName
                + " created=\"" + created
                + "\" solved=\"" + solved
                + "\" class=\"" + className
                + "\" method=\"" + mName
                + "\" lineOfCode=\"" + lineOfCode
                + "\" mutationDescription=\"" + mutationDescription
                + "\" result=\"" + mutationInfo.result)

        if (reason.isNotEmpty()) {
            print += "\" reason=\"$reason"
        }
        print += "\"/>"
        return print
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

    /**
     * Needed because of automatically generated getter and setter in Kotlin.
     */
    fun setSolved(newSolved: Long) {
        solved = newSolved
    }

    override fun toString(): String {
        val mName = if (methodName == "<init>") "init" else methodName
        return ("Write a test to kill the mutant \"<b>$mutationDescription</b>\" at line <b>$lineOfCode</b> " +
                "of method <b>$mName</b> in class <b>${details.fileName}</b> in package " +
                "<b>${details.packageName}</b> (created for branch " + branch + ")")
    }
}
