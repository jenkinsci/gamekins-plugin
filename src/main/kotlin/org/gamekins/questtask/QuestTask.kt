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
import org.gamekins.GameUserProperty
import org.gamekins.challenge.Challenge
import org.gamekins.CustomAPI
import org.gamekins.util.Constants

/**
 * A [QuestTask] is a task with progress based on different gamification elements that Gamekins offers.
 *
 * @author Philipp Straubinger
 * @since 0.6
 */
abstract class QuestTask(val numberGoal: Int) {

    val created = System.currentTimeMillis()
    var currentNumber: Int = 0
    var solved: Long = 0

    /**
     * Needed for [CustomAPI]
     */
    var title: String?
        get() = toString()
        set(value) {}

    /**
     * Gets completed percentage of the progress of the [QuestTask].
     */
    fun getCompletedPercentage(): Int {
        val percentage = currentNumber.toDouble() / numberGoal.toDouble() * 100
        return if (percentage > 100) 100 else percentage.toInt()
    }

    /**
     * Returns the score of the [QuestTask].
     */
    abstract fun getScore(): Int

    /**
     * Returns the number of solved challenges since a specific time (normally the creation time of the [QuestTask].
     */
    fun getSolvedChallengesOfUserSince(
        user: User,
        project: String,
        since: Long,
        type: Class<out Challenge> = Challenge::class.java
    ): ArrayList<Challenge> {
        val property = user.getProperty(GameUserProperty::class.java)
        if (property != null) {
            val challenges = property.getCompletedChallenges(project)
            challenges.removeIf { it.getSolved() < since }
            return ArrayList(challenges.filterIsInstance(type))
        }

        return arrayListOf()
    }

    /**
     * Returns the number of solved challenges since the last rejection.
     */
    fun getSolvedChallengesOfUserSinceLastRejection(user: User, project: String, since: Long): ArrayList<Challenge> {
        val property = user.getProperty(GameUserProperty::class.java)
        if (property != null) {
            val challenges = property.getCompletedChallenges(project)
            challenges.removeIf { it.getSolved() < since }
            val rejectedChallenges = property.getRejectedChallenges(project)
            if (rejectedChallenges.isNotEmpty()) {
                val lastRejectedChallenge = rejectedChallenges.last().first
                challenges.removeIf { it.getSolved() < lastRejectedChallenge.getSolved() }
            }
            return ArrayList(challenges)
        }

        return arrayListOf()
    }

    /**
     * Checks whether the [QuestTask] is solved.
     */
    abstract fun isSolved(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener, user: User): Boolean

    /**
     * Returns the XML representation of the quest.
     */
    abstract fun printToXML(indentation: String): String
}