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
import org.gamekins.achievement.Achievement
import org.gamekins.util.Constants

/**
 * A [QuestTask] to solve more achievements in the project.
 *
 * @author Philipp Straubinger
 * @since 0.6
 */
class SolveAchievementQuestTask(user: User, project: String): QuestTask(1) {

    private val achievements = arrayListOf<Achievement>()
    private val startNumberOfAchievements = user.getProperty(GameUserProperty::class.java)
        ?.getCompletedAchievements(project)?.size ?: 0

    override fun getScore(): Int {
        return 1
    }

    override fun isSolved(
        parameters: Constants.Parameters,
        run: Run<*, *>,
        listener: TaskListener,
        user: User
    ): Boolean {
        val property = user.getProperty(GameUserProperty::class.java)
        if (property != null) {
            val currentAchievements = property.getCompletedAchievements(parameters.projectName)
            if (currentAchievements.size > startNumberOfAchievements) {
                solved = System.currentTimeMillis()
                achievements.addAll(currentAchievements.takeLast(currentAchievements.size - startNumberOfAchievements))
                return true
            }
        }

        return false
    }

    override fun printToXML(indentation: String): String {
        return "$indentation<${this::class.simpleName} created=\"$created\" solved=\"$solved\" " +
                "startNumberOfAchievements=\"$startNumberOfAchievements\">"
    }

    override fun toString(): String {
        return "Solve one additional achievement"
    }
}