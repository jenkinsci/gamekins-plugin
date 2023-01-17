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
import org.gamekins.file.FileDetails
import org.gamekins.file.SourceFileDetails
import org.gamekins.file.TestFileDetails
import org.gamekins.util.Constants
import org.gamekins.util.SmellUtil
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue
import org.sonarsource.sonarlint.core.commons.IssueSeverity

/**
 * Specific [Challenge] to motivate the user to remove code and test smells in their code.
 *
 * @author Philipp Straubinger
 * @since 0.5
 */
class SmellChallenge(val details: FileDetails, val issue: Issue): Challenge {

    private val codeSnippet: String = createCodeSnippet()
    private val created = System.currentTimeMillis()
    private val lineContent = SmellUtil.getLineContent(details, issue.startLine, issue.endLine)
    private var solved: Long = 0

    /**
     * Creates the code snippet to be displayed in the leaderboard for each [Challenge].
     */
    private fun createCodeSnippet(): String {
        return "<pre class='prettyprint mt-2 linenums:${issue.startLine?.minus(2)}'><code class='language-java'>" +
                SmellUtil.getLineContent(details, issue.startLine?.minus(2), issue.endLine?.plus(2)) +
                "</code></pre><br><em>" + issue.message +
                " <a href=\"https://rules.sonarsource.com/java/RSPEC-" +
                "${issue.ruleKey.takeLastWhile { it.isDigit() }}\" " +
                "target=\"_blank\">More Information</a> " + "</em>"
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is SmellChallenge) return false
        return other.details.packageName == this.details.packageName
                && other.details.fileName == this.details.fileName
                && other.issue.severity == this.issue.severity
                && other.issue.type == this.issue.type
                && other.issue.ruleKey == this.issue.ruleKey
                && other.lineContent == this.lineContent
    }

    override fun getCreated(): Long {
        return created
    }

    override fun getHighlightedFileContent(): String {
        return "<pre class='prettyprint mt-2 linenums:1'><code class='language-java'>" +
                details.contents() + "</code></pre>"
    }

    override fun getName(): String {
        return when (details) {
            is SourceFileDetails -> "Code Smell"
            is TestFileDetails -> "Test Smell"
            else -> "Smell"
        }
    }

    override fun getParameters(): Constants.Parameters {
        return details.parameters
    }

    override fun getScore(): Int {
        return when (issue.severity) {
            IssueSeverity.BLOCKER -> 4
            IssueSeverity.CRITICAL -> 3
            IssueSeverity.MAJOR -> 2
            else -> 1
        }
    }

    override fun getSnippet(): String {
        return codeSnippet
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun hashCode(): Int {
        var result = details.hashCode()
        result = 31 * result + issue.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + solved.hashCode()
        return result
    }

    override fun isSolvable(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (details.parameters.branch != parameters.branch) return true
        if (issue.textRange == null) return true
        if (!details.update(parameters).filesExists()) return false

        val issues = SmellUtil.getSmellsOfFile(details, listener)
        if (issues.contains(issue)) return true
        issues.forEach {
            if (it.ruleKey == issue.ruleKey && it.type == issue.type && it.severity == issue.severity) {
                if (it.textRange == issue.textRange) {
                    return true
                } else if (this.lineContent == SmellUtil.getLineContent(details, it.startLine, it.endLine)) {
                    return true
                }
            }
        }

        return false
    }

    override fun isSolved(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (!details.update(parameters).filesExists()) return false
        val issues = SmellUtil.getSmellsOfFile(details, listener)
        if (issues.contains(issue)) return false
        issues.forEach {
            if (it.ruleKey == issue.ruleKey && it.type == issue.type && it.severity == issue.severity) {
                if (it.textRange == issue.textRange) {
                    return false
                } else if (this.lineContent == SmellUtil.getLineContent(details, it.startLine, it.endLine)) {
                    return false
                }
            }
        }

        solved = System.currentTimeMillis()
        return true
    }

    override fun printToXML(reason: String, indentation: String): String {
        var print = (indentation + "<" + this::class.simpleName + " created=\"" + created + "\" solved=\"" + solved
                + "\" class=\"" + details.fileName + "\" type=\"" + issue.type + "\" severity=\"" + issue.severity
                + "\" line=\"" + issue.startLine + "\" rule=\"" + issue.ruleKey)
        if (reason.isNotEmpty()) {
            print += "\" reason=\"$reason"
        }
        print += "\"/>"
        return print
    }

    override fun toString(): String {
        val line = if (issue.startLine == null) "concerning the whole"
        else "starting from line <b>${issue.startLine}</b> in"
        return "Improve your code by removing the smell $line class <b>${details.fileName}</b> in package " +
                "<b>${details.packageName}</b> (created for branch ${details.parameters.branch})"
    }
}