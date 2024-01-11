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

import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.CustomAPI
import org.gamekins.statistics.Statistics
import org.gamekins.LeaderboardAction
import org.gamekins.file.FileDetails
import org.gamekins.gumTree.JavaParser
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.JacocoUtil.CoverageMethod
import org.jsoup.nodes.Element

/**
 * Interface for all Challenges of Gamekins.
 *
 * Each third party [Challenge] must have a constructor with [ChallengeGenerationData] as only input parameter.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
interface Challenge {

    /**
     * Only for third party Challenges. Override it to get control whether the Challenge has been built correctly. If
     * it returns false, another Challenge will be generated.
     */
    @Suppress("UNUSED_PARAMETER")
    var builtCorrectly: Boolean
        get() = true
        set(v) = Unit

    override fun equals(other: Any?): Boolean

    /**
     * Needed for [CustomAPI]
     */
    var generalReason: String?
        get() = toString()
        set(value) {}

    /**
     * Generates the compilationUnit. The [sourceFile] here is the name of the Java file (e.g., Test.java) and
     * className is the fully qualified name of the class (e.g., org.example.Test).
     */
    fun generateCompilationUnit(parameters: Parameters, className: String, sourceFile: String): String {
        val compilationUnit = JavaParser.parse(sourceFile, className, parameters)
        LexicalPreservingPrinter.setup(compilationUnit)
        return LexicalPreservingPrinter.print(compilationUnit)
    }

    /**
     * Returns the constants provided during creation. Must include entries for "projectName", "branch", "workspace",
     * "jacocoResultsPath" and "jacocoCSVPath".
     */
    fun getParameters(): Parameters

    /**
     * Returns the creation time in milliseconds since 01.01.1970.
     */
    fun getCreated(): Long

    /**
     * Returns the highlighted file content underneath the [Challenge] like the source code.
     */
    fun getHighlightedFileContent(): String {
        return ""
    }

    /**
     * Returns the name of the category of the current [Challenge].
     */
    fun getName(): String

    /**
     * Returns the score for the specific [Challenge].
     */
    fun getScore(): Int

    /**
     * Returns the code snippet to be displayed in the leaderboard.
     */
    fun getSnippet(): String {
        return ""
    }

    /**
     * Returns the time when the [Challenge] has been solved in milliseconds since 01.01.1970.
     */
    fun getSolved(): Long

    /**
     * Returns the tooltip of the Challenge. Currently only available for the [LineCoverageChallenge]
     * and [BranchCoverageChallenge].
     */
    fun getToolTipText(): String {
        return ""
    }

    /**
     * Checks whether the current [Challenge] is still solvable or not.
     */
    fun isSolvable(parameters: Parameters, run: Run<*, *>, listener: TaskListener)
            : Boolean

    /**
     * Checks whether the current [Challenge] is solved.
     */
    fun isSolved(parameters: Parameters, run: Run<*, *>, listener: TaskListener)
            : Boolean

    /**
     * Returns whether a tooltip should be shown for the Challenge. Currently only available for the
     * [LineCoverageChallenge] and [BranchCoverageChallenge].
     */
    fun isToolTip(): Boolean {
        return false
    }

    /**
     * Returns the XML representation of the current [Challenge] with the [indentation] in front of the line
     * for the [Statistics]. Adds an additional [reason] in case that the [Challenge] has been rejected.
     */
    fun printToXML(reason: String, indentation: String): String?

    fun setRejectedTime(time: Long)

    /**
     * Removes all HTML formatting of the [Challenge.toString] method.
     */
    fun toEscapedString(): String {
        return toString().replace(Regex("<[^>]++>"), "")
    }

    /**
     * Returns the String representation of the [Challenge] for the [LeaderboardAction].
     */
    override fun toString(): String

    /**
     * Updates the [Challenge] during a quest if the [Challenge] is not the first one.
     */
    fun update(parameters: Parameters) = Unit

    /**
     * Data class for the initialisation of a Challenge. Every val variable will be non-null with the desired data.
     */
    data class ChallengeGenerationData(val parameters: Parameters, val user: User, val selectedFile: FileDetails?,
                                       val listener: TaskListener, var method: CoverageMethod? = null,
                                       var line: Element? = null, var testCount: Int? = null,
                                       var headCommitHash: String? = null)
}
