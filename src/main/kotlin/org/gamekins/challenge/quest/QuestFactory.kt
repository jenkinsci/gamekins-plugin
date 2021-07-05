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

package org.gamekins.challenge.quest

import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.GameUserProperty
import org.gamekins.challenge.Challenge
import org.gamekins.challenge.LineCoverageChallenge
import org.gamekins.event.EventHandler
import org.gamekins.event.user.QuestGeneratedEvent
import org.gamekins.file.FileDetails
import org.gamekins.file.SourceFileDetails
import org.gamekins.util.Constants
import org.gamekins.util.GitUtil
import org.gamekins.util.JacocoUtil
import org.jsoup.nodes.Element
import kotlin.random.Random

/**
 * Factory for generating [Quest]s.
 *
 * @author Philipp Straubinger
 * @since 0.4
 */
object QuestFactory {

    /**
     * Generates new [Quest]s for a particular [user].
     */
    @JvmStatic
    fun generateNewQuests(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>, maxQuests: Int = Constants.DEFAULT_CURRENT_QUESTS
    ): Int {

        var generated = 0
        if (property.getCurrentQuests(parameters.projectName).size < maxQuests) {
            listener.logger.println("[Gamekins] Start generating quests for user ${user.fullName}")

            val userClasses = ArrayList(classes)
            userClasses.removeIf { details: FileDetails ->
                !details.changedByUsers.contains(GitUtil.GameUser(user))
            }

            listener.logger.println("[Gamekins] Found ${userClasses.size} last changed files of user ${user.fullName}")

            for (i in property.getCurrentQuests(parameters.projectName).size until maxQuests) {
                if (userClasses.size == 0) {
                    property.newQuest(parameters.projectName,
                        Quest(Constants.NO_QUEST, arrayListOf())
                    )
                    EventHandler.addEvent(
                        QuestGeneratedEvent(parameters.projectName, parameters.branch, user,
                            Quest(Constants.NO_QUEST, arrayListOf())
                        )
                    )
                    break
                }

                listener.logger.println("[Gamekins] Started to generate quest")
                val quest = generateQuest(user, property, parameters, listener, userClasses)
                listener.logger.println("[Gamekins] Generated quest $quest")
                property.newQuest(parameters.projectName, quest)
                listener.logger.println("[Gamekins] Added quest $quest")
                EventHandler.addEvent(QuestGeneratedEvent(parameters.projectName, parameters.branch, user, quest))
                if (quest.name == Constants.NO_QUEST) break
                generated++
            }
        }

        return generated
    }

    /**
     * Lines over lines: 3 LineCoverageChallenges in one class
     * More than methods: 3 MethodCoverageChallenges in one class
     * Pack it together: 3 Challenges in one package
     * Incremental: 3 ClassCoverageChallenges in one class
     * Expanding: Line -> Method -> Class
     * Decreasing: Class -> Method -> Line
     * Just test: 3 TestChallenges
     * 100% is not the limit: 3 MutationChallenges in one class
     *
     * Generates a random new [Quest].
     */
    fun generateQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>
    ): Quest {

        for (i in 0..2) {
            val quest = when (Random.nextInt(1)) {
                0 -> generateLinesQuest(user, property, parameters, listener, classes)
                else -> null
            }

            if (property.getRejectedQuests(parameters.projectName).map { it.first }.contains(quest)) continue

            if (quest != null) return quest
        }

        return Quest(Constants.NO_QUEST, arrayListOf())
    }

    /**
     * Generates a new LineQuest with three different [LineCoverageChallenge]s in the same class.
     */
    private fun generateLinesQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>
    ): Quest? {
        val suitableClasses = classes.filterIsInstance<SourceFileDetails>().filter { it.coverage < 1.0 }
        if (suitableClasses.isEmpty()) return null
        val selectedClass = suitableClasses.random()

        if (JacocoUtil.getLines(
                JacocoUtil.calculateCurrentFilePath(
                    parameters.workspace, selectedClass.jacocoSourceFile, selectedClass.parameters.remote
                )
            ).size < 3
        ) return null

        val steps = arrayListOf<QuestStep>()
        val lines = hashSetOf<Element>()
        while (lines.size < 3) JacocoUtil.chooseRandomLine(selectedClass, parameters.workspace)?.let { lines.add(it) }
        for (line in lines) {
            val data = Challenge.ChallengeGenerationData(parameters, user, selectedClass, listener, line = line)
            val challenge = LineCoverageChallenge(data)
            if (property.getRejectedChallenges(parameters.projectName).any { it.first == challenge }) {
                return null
            }
            steps.add(QuestStep("", challenge))
        }

        return Quest("Lines over lines", steps)
    }
}