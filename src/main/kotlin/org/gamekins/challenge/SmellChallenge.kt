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

/**
 * Specific [Challenge] to motivate the user to remove code and test smells in their code.
 *
 * @author Philipp Straubinger
 * @since 0.5
 */
class SmellChallenge(val file: FileDetails, val issue: Issue): Challenge {

    private var codeSnippet: String = createCodeSnippet()
    private val created = System.currentTimeMillis()
    private val lineContent = SmellUtil.getLineContent(file, issue.startLine, issue.endLine)
    private var solved: Long = 0

    /**
     * Creates the code snippet to be displayed in the leaderboard for each [Challenge].
     */
    fun createCodeSnippet(): String {
        return "<pre class='prettyprint mt-2 linenums:${issue.startLine?.minus(2)}'><code class='language-java'>" +
                SmellUtil.getLineContent(file, issue.startLine?.minus(2), issue.endLine?.plus(2)) +
                "</code></pre><br><em>" + issue.message + "</em>"
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is SmellChallenge) return false
        return other.file.packageName == this.file.packageName
                && other.file.fileName == this.file.fileName
                && other.issue.severity == this.issue.severity
                && other.issue.type == this.issue.type
                && other.issue.ruleKey == this.issue.ruleKey
                && other.lineContent == this.lineContent
    }

    override fun getParameters(): Constants.Parameters {
        return file.parameters
    }

    override fun getCreated(): Long {
        return created
    }

    override fun getName(): String {
        return when (file) {
            is SourceFileDetails -> "Code Smell"
            is TestFileDetails -> "Test Smell"
            else -> "Smell"
        }
    }

    override fun getScore(): Int {
        return when (issue.severity) {
            "BLOCKER" -> 4
            "CRITICAL" -> 3
            "MAJOR" -> 2
            else -> 1
        }
    }

    fun getSnippet():String {
        return codeSnippet
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun hashCode(): Int {
        var result = file.hashCode()
        result = 31 * result + issue.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + solved.hashCode()
        return result
    }

    override fun isSolvable(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (file.parameters.branch != parameters.branch) return true
        if (issue.textRange == null) return true

        val issues = SmellUtil.getSmellsOfFile(file.update(parameters), listener)
        if (issues.contains(issue)) return true
        issues.forEach {
            if (it.ruleKey == issue.ruleKey && it.type == issue.type && it.severity == issue.severity) {
                if (it.textRange == issue.textRange) {
                    return true
                } else if (this.lineContent == SmellUtil.getLineContent(file, it.startLine, it.endLine)) {
                    return true
                }
            }
        }

        return false
    }

    override fun isSolved(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        val issues = SmellUtil.getSmellsOfFile(file.update(parameters), listener)
        if (issues.contains(issue)) return false
        issues.forEach {
            if (it.ruleKey == issue.ruleKey && it.type == issue.type && it.severity == issue.severity) {
                if (it.textRange == issue.textRange) {
                    return false
                } else if (this.lineContent == SmellUtil.getLineContent(file, it.startLine, it.endLine)) {
                    return false
                }
            }
        }

        return true
    }

    override fun printToXML(reason: String, indentation: String): String {
        var print = (indentation + "<" + this::class.simpleName + " created=\"" + created + "\" solved=\"" + solved
                + "\" class=\"" + file.fileName + "\" type=\"" + issue.type + "\" severity=\"" + issue.severity
                + "\" line=\"" + issue.startLine + "\" rule=\"" + issue.ruleKey)
        if (reason.isNotEmpty()) {
            print += "\" reason=\"$reason"
        }
        print += "\"/>"
        return print
    }

    override fun toEscapedString(): String {
        return toString().replace(Regex("<.+?>"), "")
    }

    override fun toString(): String {
        val line = if (issue.startLine == null) "concerning the whole"
        else "starting from line <b>${issue.startLine}</b> in"
        return "Improve your code by removing the smell $line class <b>${file.fileName}</b> in package " +
                "<b>${file.packageName}</b> (created for branch ${file.parameters.branch})"
    }
}