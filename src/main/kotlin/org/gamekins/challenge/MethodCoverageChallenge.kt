/*
 * Copyright 2021 Gamekins contributors
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
import org.gamekins.challenge.Challenge.ChallengeGenerationData
import org.gamekins.util.JacocoUtil

/**
 * Specific [Challenge] to motivate the user to cover more lines in a random method of a specific class.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class MethodCoverageChallenge(data: ChallengeGenerationData) : CoverageChallenge(data.selectedClass, data.workspace) {

    private val lines = data.method!!.lines
    private val methodName = data.method!!.methodName
    private val missedLines = data.method!!.missedLines
    private val firstLineID = data.method!!.firstLineID


    init {
        codeSnippet = createCodeSnippet(classDetails, firstLineID, data.workspace)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is MethodCoverageChallenge) return false
        return other.classDetails.packageName == this.classDetails.packageName
                && other.classDetails.className == this.classDetails.className
                && other.methodName == this.methodName
    }

    override fun getSnippet(): String {
        return if (codeSnippet.isNotEmpty()) codeSnippet else "Code snippet is not available"
    }

    override fun getName(): String {
        return "Method Coverage"
    }

    override fun getScore(): Int {
        return if ((lines - missedLines) / lines.toDouble() > 0.8) 3 else 2
    }

    override fun hashCode(): Int {
        var result = lines
        result = 31 * result + methodName.hashCode()
        result = 31 * result + missedLines
        return result
    }

    /**
     * Checks whether the [MethodCoverageChallenge] is solvable if the [run] was in the branch (taken from
     * [constants]), where it has been generated. There must be uncovered or not fully covered lines left in the
     * method. The [workspace] is the folder with the code and execution rights, and the [listener] reports the events
     * to the console output of Jenkins.
     */
    override fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                            workspace: FilePath): Boolean {
        if (classDetails.constants["branch"] != constants["branch"]) return true

        val jacocoMethodFile = JacocoUtil.calculateCurrentFilePath(workspace,
                classDetails.jacocoMethodFile, classDetails.workspace)
        try {
            if (!jacocoMethodFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo method file "
                        + jacocoMethodFile.remote + JacocoUtil.EXISTS + jacocoMethodFile.exists())
                return true
            }

            val methods = JacocoUtil.getMethodEntries(jacocoMethodFile)
            for (method in methods) {
                if (method.methodName == methodName) {
                    return method.missedLines > 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return false
        }

        return false
    }

    /**
     * The [MethodCoverageChallenge] is solved if the number of missed lines, according to the [classDetails] JaCoCo
     * files, is less than during generation. The [workspace] is the folder with the code and execution rights, and
     * the [listener] reports the events to the console output of Jenkins.
     */
    override fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                          workspace: FilePath): Boolean {
        val jacocoMethodFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.jacocoMethodFile,
                        classDetails.workspace), classDetails.constants["branch"]!!)
        val jacocoCSVFile = JacocoUtil.getJacocoFileInMultiBranchProject(run, constants,
                JacocoUtil.calculateCurrentFilePath(workspace, classDetails.jacocoCSVFile,
                        classDetails.workspace), classDetails.constants["branch"]!!)

        try {
            if (!jacocoMethodFile.exists() || !jacocoCSVFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo method file " + jacocoMethodFile.remote
                        + JacocoUtil.EXISTS + jacocoMethodFile.exists())
                listener.logger.println("[Gamekins] JaCoCo csv file " + jacocoCSVFile.remote
                        + JacocoUtil.EXISTS + jacocoCSVFile.exists())
                return false
            }

            val methods = JacocoUtil.getMethodEntries(jacocoMethodFile)
            for (method in methods) {
                if (method.methodName == methodName) {
                    if (method.missedLines < missedLines) {
                        super.setSolved(System.currentTimeMillis())
                        solvedCoverage = JacocoUtil.getCoverageInPercentageFromJacoco(
                                classDetails.className, jacocoCSVFile)
                        return true
                    }
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
        }

        return false
    }

    /**
     * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
     */
    @Suppress("unused", "SENSELESS_COMPARISON")
    private fun readResolve(): Any {
        if (codeSnippet == null) codeSnippet = ""
        return this
    }

    override fun toString(): String {
        return ("Write a test to cover more lines of method <b>" + methodName + "</b> in class <b>"
                + classDetails.className + "</b> in package <b>" + classDetails.packageName
                + "</b> (created for branch " + classDetails.constants["branch"] + ")")
    }
}
