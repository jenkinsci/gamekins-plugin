/*
 * Copyright 2020 Gamekins contributors
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
import org.gamekins.mutation.MutationInfo
import org.gamekins.mutation.MutationResults
import org.gamekins.util.JacocoUtil
import org.gamekins.util.JacocoUtil.ClassDetails
import org.jsoup.nodes.Element


/**
 * Specific [Challenge] to motivate users to write test cases to kill a generated mutant.
 *
 * @author Tran Phan
 * @since 1.0
 */
class MutationTestChallenge(val mutationInfo: MutationInfo, val classDetails: ClassDetails,
                            val branch: String?, workspace: FilePath
) : Challenge {

    private val created = System.currentTimeMillis()
    private var solved: Long = 0
    private val methodName = mutationInfo.mutationDetails.methodInfo["methodName"]
    private val className = mutationInfo.mutationDetails.methodInfo["className"]
    private val mutationDescription = mutationInfo.mutationDetails.mutationDescription
    private val lineOfCode = mutationInfo.mutationDetails.loc
    val uniqueID = mutationInfo.uniqueID
    private val codeSnippet = getCodeSnippet(classDetails, lineOfCode, workspace)
    private var mutationStillInJson = true
    private var classStillInJson = true

    override fun getCreated(): Long {
        return created
    }

    fun getName(): String {
        return "MutationTestChallenge"
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun printToXML(reason: String, indentation: String): String {
        var print = (indentation + "<" + getName()
                + " created=\"" + created
                + "\" solved=\"" + solved
                + "\" class=\"" + className
                + "\" method=\"" + methodName
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
     * Needed because of automatically generated getter and setter in Kotlin.
     */
    fun setSolved(newSolved: Long) {
        solved = newSolved
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
    override fun getConstants(): HashMap<String, String> {
        return classDetails.constants
    }


    override fun getScore(): Int {
        //fixme: find a way to determine mutation score - could be based on complexity compared to original code
        return 4
    }

    override fun hashCode(): Int {
        return this.mutationInfo.hashCode()
    }

    /**
     * Checks whether the [MutationTestChallenge] is solvable if the [run] was in the [branch] (taken from
     * [constants]), where it has been generated. The [workspace] is the folder with the code and execution rights,
     * and the [listener] reports the events to the console output of Jenkins.
     */
    override fun isSolvable(
        constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
        workspace: FilePath
    ): Boolean {
        // Check if a class is no longer in JSON file, thus unsolvable
        // This assumes that isSolved is always called before isSolvable in checkUser method
        // This logic needs to be changed if the call order is changed or they are invoked somewhere else
        return classStillInJson and mutationStillInJson
    }

    /**
     * The [MutationTestChallenge] is solved if mutation status is killed.
     * The [workspace] is the folder with the code and execution rights, and
     * the [listener] reports the events to the console output of Jenkins.
     */
    override fun isSolved(
        constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
        workspace: FilePath
    ): Boolean {
        val jsonFilePath = JacocoUtil.calculateCurrentFilePath(
            workspace, classDetails.mocoJSONFile, classDetails.workspace
        )
        val mutationResults = MutationResults.retrievedMutationsFromJson(jsonFilePath, listener)
        val filteredByClass = mutationResults?.entries?.filter { it.key == this.className }
        if (!filteredByClass.isNullOrEmpty()) {
            val filteredByDetails =  filteredByClass.filter {
                it.value.any { it1 -> ((it1.uniqueID == uniqueID) ||
                        (it1.mutationDetails == mutationInfo.mutationDetails)) }
            }
            if (filteredByDetails.isNullOrEmpty()) {
                mutationStillInJson  = false
                return false
            } else {
                return filteredByDetails.any { it.value.any { it1 -> it1.result == "killed" } }
            }
        } else {
            classStillInJson = false
        }
        return false
    }

    override fun toString(): String {
        return ("Write a test to kill this mutant at line $lineOfCode of method " +
                "$methodName in class $className in package ${classDetails.packageName}" +
                " (created for branch " + branch + ") code snippet: $codeSnippet")
    }

    fun getCodeSnippet(classDetails: ClassDetails, lineOfCode: Int, workspace: FilePath): String {
        if (lineOfCode < 0) {
            return ""
        }
        if (classDetails.jacocoSourceFile.exists()) {
            val javaHtmlPath = JacocoUtil.calculateCurrentFilePath(
                workspace, classDetails.jacocoSourceFile, classDetails.workspace
            )
            val range = if (lineOfCode > 0) Pair(lineOfCode - 1, lineOfCode + 1) else Pair(lineOfCode, lineOfCode + 2)
            val snippetElements = JacocoUtil.getLinesInRange(javaHtmlPath, range)
            return snippetElements
        }
        return ""
    }
}
