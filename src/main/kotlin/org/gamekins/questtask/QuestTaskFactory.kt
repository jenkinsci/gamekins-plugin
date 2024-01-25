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
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.GamePublisherDescriptor
import org.gamekins.GameUserProperty
import org.gamekins.challenge.Challenge
import org.gamekins.challenge.CoverageChallenge
import org.gamekins.challenge.TestChallenge
import org.gamekins.event.EventHandler
import org.gamekins.event.user.QuestTaskGeneratedEvent
import org.gamekins.util.Constants
import kotlin.random.Random

/**
 * Factory for generating [QuestTask]s.
 *
 * @author Philipp Straubinger
 * @since 0.6
 */
object QuestTaskFactory {

    /**
     * Chooses the type of [Challenge] to be generated.
     */
    private fun chooseChallengeType(): Class<out Challenge> {
        val weightList = arrayListOf<Class<out Challenge>>()
        val challengeTypes = GamePublisherDescriptor.challenges
        challengeTypes[CoverageChallenge::class.java] = 10
        challengeTypes[Challenge::class.java] = 10
        challengeTypes.remove(TestChallenge::class.java)
        challengeTypes.forEach { (clazz, weight) ->
            (0 until weight).forEach { _ ->
                weightList.add(clazz)
            }
        }
        return weightList[Random.nextInt(weightList.size)]
    }

    /**
     * Chooses the type of [QuestTask] to be generated.
     */
    private fun chooseQuestTaskType(projectName: String): Class<out QuestTask> {
        val weightList = arrayListOf<Class<out QuestTask>>()
        val questTaskTypes = hashMapOf<Class<out QuestTask>, Int>()
        questTaskTypes[AddMoreTestsQuestTask::class.java] = 5
        questTaskTypes[CoverMoreBranchesQuestTask::class.java] = 5
        questTaskTypes[CoverMoreLinesQuestTask::class.java] = 5
        if (getNumberOfParticipantsInProject(projectName) > 1) {
            questTaskTypes[ReceiveChallengeQuestTask::class.java] = 2
            questTaskTypes[SendChallengeQuestTask::class.java] = 2
        }
        questTaskTypes[SolveAchievementQuestTask::class.java] = 1
        questTaskTypes[SolveChallengesQuestTask::class.java] = 7
        questTaskTypes[SolveChallengesWithoutRejectionQuestTask::class.java] = 2
        questTaskTypes.forEach { (clazz, weight) ->
            (0 until weight).forEach { _ ->
                weightList.add(clazz)
            }
        }
        return weightList[Random.nextInt(weightList.size)]
    }

    /**
     * Generates new [QuestTask]s for a particular [user].
     */
    @JvmStatic
    fun generateNewQuestTasks(
        user: User,
        property: GameUserProperty,
        parameters: Constants.Parameters,
        listener: TaskListener,
        maxQuests: Int = Constants.Default.CURRENT_QUESTS
    ): Int {

        var generated = 0
        if (property.getCurrentQuestTasks(parameters.projectName).size < maxQuests) {
            listener.logger.println("[Gamekins] Start generating quest tasks for user ${user.fullName}")

            for (i in property.getCurrentQuestTasks(parameters.projectName).size until maxQuests) {
                val questTask = generateQuestTask(user, property, parameters)
                listener.logger.println("[Gamekins] Generated quest task $questTask")
                EventHandler.addEvent(
                    QuestTaskGeneratedEvent(parameters.projectName, parameters.branch, user, questTask)
                )
                property.newQuestTask(parameters.projectName, questTask)
                generated++
            }
        }

        return generated
    }

    /**
     * Generates a random new [QuestTask].
     */
    private fun generateQuestTask(
        user: User,
        property: GameUserProperty,
        parameters: Constants.Parameters
    ): QuestTask {

        val number = Random.nextInt(10) + 1

        for (i in 0..2) {
            val questTask = when (chooseQuestTaskType(parameters.projectName)) {
                AddMoreTestsQuestTask::class.java -> AddMoreTestsQuestTask(number, parameters.workspace)
                CoverMoreBranchesQuestTask::class.java -> CoverMoreBranchesQuestTask(number * 5,
                    FilePath(parameters.workspace.channel,
                        parameters.workspace.remote + parameters.jacocoCSVPath.substring(2)))
                CoverMoreLinesQuestTask::class.java -> CoverMoreLinesQuestTask(number * 5,
                    FilePath(parameters.workspace.channel,
                    parameters.workspace.remote + parameters.jacocoCSVPath.substring(2)))
                ReceiveChallengeQuestTask::class.java -> ReceiveChallengeQuestTask()
                SendChallengeQuestTask::class.java -> SendChallengeQuestTask()
                SolveAchievementQuestTask::class.java -> SolveAchievementQuestTask(user, parameters.projectName)
                SolveChallengesQuestTask::class.java -> SolveChallengesQuestTask(number, chooseChallengeType())
                SolveChallengesWithoutRejectionQuestTask::class.java ->
                    SolveChallengesWithoutRejectionQuestTask(number)
                else -> SolveChallengesQuestTask(number, Challenge::class.java)
            }

            if (property.getCurrentQuestTasks(parameters.projectName).contains(questTask)) continue

            return questTask
        }

        return SolveChallengesQuestTask(number, Challenge::class.java)
    }

    /**
     * Retrieves the number of participants in the current project. Needed to determine whether
     * [ReceiveChallengeQuestTask]s and [SendChallengeQuestTask]s should be generated.
     */
    private fun getNumberOfParticipantsInProject(projectName: String): Int {
        var number = 0
        for (user in User.getAll()) {
            val property = user.getProperty(GameUserProperty::class.java)
            if (property != null && property.isParticipating(projectName)) number++
        }

        return number
    }
}