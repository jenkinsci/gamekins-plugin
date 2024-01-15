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

package org.gamekins.questtask

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.util.Constants
import org.gamekins.util.JacocoUtil

/**
 * A [QuestTask] to cover more branches in the project.
 *
 * @author Philipp Straubinger
 * @since 0.6
 */
class CoverMoreBranchesQuestTask(branchesNumber: Int, csvFile: FilePath): QuestTask(branchesNumber) {

    private val startNumberOfBranches = JacocoUtil.getCoveredBranches(csvFile)

    override fun getScore(): Int {
        return numberGoal / 5
    }

    override fun isSolved(
        parameters: Constants.Parameters,
        run: Run<*, *>,
        listener: TaskListener,
        user: User
    ): Boolean {
        val currentCoveredLines = JacocoUtil.getCoveredBranches(
            FilePath(parameters.workspace.channel,
                parameters.workspace.remote + "/" + parameters.jacocoCSVPath.replace("**/", "")))
        currentNumber = currentCoveredLines - startNumberOfBranches
        if (currentNumber >= numberGoal) {
            solved = System.currentTimeMillis()
            return true
        }

        return false
    }

    override fun printToXML(indentation: String): String {
        return "$indentation<${this::class.simpleName} created=\"$created\" solved=\"$solved\" " +
                "currentNumber=\"$currentNumber\" numberGoal=\"$numberGoal\" " +
                "startNumberOfBranches=\"$startNumberOfBranches\">"
    }

    override fun toString(): String {
        return "Cover $numberGoal more branches in your project by tests"
    }
}