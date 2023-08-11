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

import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.challenge.*
import org.gamekins.util.Constants

/**
 * A [QuestTask] to solve more challenges in the project.
 *
 * @author Philipp Straubinger
 * @since 0.6
 */
class SolveChallengesQuestTask(challengeNumber: Int, private val challengeType: Class<out Challenge>)
    : QuestTask(challengeNumber) {

    val challenges: ArrayList<Challenge> = arrayListOf()

    override fun getScore(): Int {
        return numberGoal
    }

    override fun isSolved(
        parameters: Constants.Parameters,
        run: Run<*, *>,
        listener: TaskListener,
        user: User
    ): Boolean {
        val solvedChallenges = getSolvedChallengesOfUserSince(user, parameters.projectName, created, challengeType)
        currentNumber = solvedChallenges.size
        if (solvedChallenges.size >= numberGoal) {
            challenges.addAll(solvedChallenges)
            solved = System.currentTimeMillis()
            return true
        }

        return false
    }

    override fun printToXML(indentation: String): String {
        return "$indentation<${this::class.simpleName} created=\"$created\" solved=\"$solved\" " +
                "currentNumber=\"$currentNumber\" numberGoal=\"$numberGoal\">"
    }

    override fun toString(): String {
        val type =  when (challengeType) {
            BranchCoverageChallenge::class.java -> "Branch Coverage "
            LineCoverageChallenge::class.java -> "Line Coverage "
            MethodCoverageChallenge::class.java -> "Method Coverage "
            ClassCoverageChallenge::class.java -> "Class Coverage "
            CoverageChallenge::class.java -> "Coverage "
            MutationChallenge::class.java -> "Mutation "
            SmellChallenge::class.java -> "Smell "
            else -> ""
        }
        return if (numberGoal == 1) "Solve one ${type}Challenge" else "Solve $numberGoal ${type}Challenges"
    }
}