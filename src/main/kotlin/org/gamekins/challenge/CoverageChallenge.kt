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
import org.gamekins.file.SourceFileDetails
import org.gamekins.util.JacocoUtil
import org.gamekins.util.Constants.Parameters
import java.io.File

/**
 * Abstract class to generate basic information about the class used for generating a [CoverageChallenge].
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
abstract class CoverageChallenge(var details: SourceFileDetails, workspace: FilePath?)
    : Challenge {

    @Deprecated("Use implementation of new file structure", replaceWith = ReplaceWith("details"))
    val classDetails: JacocoUtil.ClassDetails? = null
    var coverage: Double
    protected var fullyCoveredLines: Int
    private var notCoveredLines: Int
    private var partiallyCoveredLines: Int
    var solvedCoverage = 0.0
    private val created = System.currentTimeMillis()
    private var solved: Long = 0
    protected var codeSnippet = ""


    /**
     * Calculates the number of fully, partially and not covered lines, and the coverage of the class itself.
     */
    init {
        val document = JacocoUtil.generateDocument(JacocoUtil.calculateCurrentFilePath(workspace!!,
                details.jacocoSourceFile, details.parameters.remote))
        fullyCoveredLines = JacocoUtil.calculateCoveredLines(document, "fc")
        partiallyCoveredLines = JacocoUtil.calculateCoveredLines(document, "pc")
        notCoveredLines = JacocoUtil.calculateCoveredLines(document, "nc")
        coverage = details.coverage
    }

    /**
     * Creates the code snippet to be displayed in the leaderboard for each [Challenge]. [target] is either the
     * line number or the line content.
     */
    open fun createCodeSnippet(classDetails: SourceFileDetails, target: Any, workspace: FilePath): String {
        if (classDetails.jacocoSourceFile.exists()) {
            val javaHtmlPath = JacocoUtil.calculateCurrentFilePath(
                workspace, classDetails.jacocoSourceFile, classDetails.parameters.remote
            )
            val snippetElements = JacocoUtil.getLinesInRange(javaHtmlPath, target, 4)
            if (snippetElements.first == "") {
                return ""
            }
            val loc = (target as String).substring(1)
            val linenums = if (loc.toIntOrNull() is Int) "linenums:${loc.toInt() - 2}" else
                "linenums:${
                    File("${workspace.remote}${classDetails.filePath}")
                    .readLines().indexOfFirst { it.contains(target) } - 1}"

            return "<pre class='prettyprint mt-2 ${linenums}'><code class='language-java'>" +
                    snippetElements.first +
                    "</code></pre>"
        }

        return ""
    }

    override fun getCreated(): Long {
        return created
    }

    override fun getHighlightedFileContent(): String {
        return "<pre class='prettyprint mt-2 linenums:1'><code class='language-java'>" +
                details.contents() + "</code></pre>"
    }

    override fun getParameters(): Parameters {
        return details.parameters
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun printToXML(reason: String, indentation: String): String {
        var print = (indentation + "<" + this::class.simpleName + " created=\"" + created + "\" solved=\"" + solved
                + "\" class=\"" + details.fileName + "\" coverage=\"" + coverage
                + "\" coverageAtSolved=\"" + solvedCoverage)
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

    override fun update(parameters: Parameters) {
        val document = JacocoUtil.generateDocument(JacocoUtil.calculateCurrentFilePath(parameters.workspace,
            details.jacocoSourceFile, details.parameters.remote))
        fullyCoveredLines = JacocoUtil.calculateCoveredLines(document, "fc")
        partiallyCoveredLines = JacocoUtil.calculateCoveredLines(document, "pc")
        notCoveredLines = JacocoUtil.calculateCoveredLines(document, "nc")
        coverage = details.coverage
    }
}
