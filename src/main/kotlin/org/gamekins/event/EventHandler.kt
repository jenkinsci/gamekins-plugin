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

package org.gamekins.event

import hudson.model.Run
import hudson.model.User
import kotlinx.coroutines.runBlocking
import org.gamekins.WebSocketServer
import org.gamekins.event.user.*
import org.gamekins.util.MailUtil
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Handler that stores all events happening in the context of Gamekins. Runs the event after adding it to the list.
 *
 * @author Philipp Straubinger
 * @since 0.3
 */
object EventHandler {

    val events: CopyOnWriteArrayList<Event> = CopyOnWriteArrayList()

    /**
     * Deletes old events, adds a new [event] to the list of [events] and runs it.
     */
    @JvmStatic
    fun addEvent(event: Event) {
        events.add(event)
        Thread(event).start()
    }

    /**
     * Generates the text for all [Event]s based on Challenges.
     */
    private fun generateChallengesText(list: ArrayList<UserEvent>): String {
        var text = ""
        if (list.any { it is ChallengeSolvedEvent }) {
            text += "Challenge(s) solved:\n"
            for (event in list.filterIsInstance<ChallengeSolvedEvent>()) {
                text += "- ${event.challenge.toEscapedString()}\n"
                runBlocking {
                    WebSocketServer.sendMessage(
                        "Challenge solved: ${event.challenge.toEscapedString()}", event.sendToUsers)
                }
            }
            text += "\n"
        }

        if (list.any { it is ChallengeUnsolvableEvent }) {
            text += "New unsolvable Challenge(s):\n"
            for (event in list.filterIsInstance<ChallengeUnsolvableEvent>()) {
                text += "- ${event.challenge.toEscapedString()}\n"
                runBlocking {
                    WebSocketServer.sendMessage(
                        "Challenge unsolvable: ${event.challenge.toEscapedString()}", event.sendToUsers)
                }
            }
            text += "\n"
        }

        if (list.any { it is ChallengeGeneratedEvent }) {
            text += "Challenge(s) generated:\n"
            for (event in list.filterIsInstance<ChallengeGeneratedEvent>()) {
                text += "- ${event.challenge.toEscapedString()}\n"
                runBlocking {
                    WebSocketServer.sendMessage(
                        "New Challenge: ${event.challenge.toEscapedString()}", event.sendToUsers)
                }
            }
            text += "\n"
        }

        return text
    }

    /**
     * Generates the mail text based on the current events.
     */
    fun generateMailText(projectName: String, build: Run<*, *>, user: User, list: ArrayList<UserEvent>): String {
        var text = "Hello ${user.fullName},\n\n"
        text += "here are your Gamekins results from run ${build.number} of project $projectName:\n\n"

        text += generateChallengesText(list)

        text += generateQuestTasksText(list)

        if (list.any { it is AchievementSolvedEvent }) {
            text += "Achievement(s) solved:\n"
            for (event in list.filterIsInstance<AchievementSolvedEvent>()) {
                text += "- ${event.achievement}\n"
                runBlocking {
                    WebSocketServer.sendMessage("Achievement solved: ${event.achievement.title}", event.sendToUsers)
                }
            }
            text += "\n"
        }

        if (list.any {it is BadgeEarnedEvent}) {
            text += "New Badge(s) earned in:\n"
            for (event in list.filterIsInstance<BadgeEarnedEvent>()) {
                text += "- ${event.achievement}\n"
                runBlocking {
                    WebSocketServer.sendMessage("Badge earned: ${event.achievement.title}", event.sendToUsers)
                }
            }
            text += "\n"
        }

        if (list.any {it is AchievementProgressedEvent}) {
            text += "Progress made in Achievements:\n"
            for (event in list.filterIsInstance<AchievementProgressedEvent>()) {
                text += "- ${event.achievement}\n"
                runBlocking {
                    WebSocketServer.sendMessage(
                        "Progress in achievement: ${event.achievement.title}", event.sendToUsers)
                }
            }
            text += "\n"
        }

        text += "View the build on ${build.parent.absoluteUrl}${build.number}/\n"
        text += MailUtil.generateViewLeaderboardText(build.parent)

        text += "View your achievements on ${user.absoluteUrl}/achievements/"

        return text
    }

    /**
     * Generates the text for all [Event]s based on QuestTasks.
     */
    private fun generateQuestTasksText(list: ArrayList<UserEvent>): String {
        var text = ""
        if (list.any { it is QuestTaskProgressEvent }) {
            text += "Progress in Quest(s):\n"
            for (event in list.filterIsInstance<QuestTaskProgressEvent>()) {
                text += "- ${event.questTask}: ${event.currentNumber} of ${event.numberGoal} already done\n"
                runBlocking {
                    WebSocketServer.sendMessage(
                        "Progress in Quest ${event.questTask}: " +
                                "${event.currentNumber} of ${event.numberGoal} already done",
                        event.sendToUsers)
                }
            }
            text += "\n"
        }

        if (list.any { it is QuestTaskSolvedEvent }) {
            text += "Quest(s) solved:\n"
            for (event in list.filterIsInstance<QuestTaskSolvedEvent>()) {
                text += "- ${event.questTask}\n"
                runBlocking {
                    WebSocketServer.sendMessage("Quest solved: ${event.questTask}", event.sendToUsers)
                }
            }
            text += "\n"
        }

        if (list.any { it is QuestTaskGeneratedEvent }) {
            text += "Quest(s) generated:\n"
            for (event in list.filterIsInstance<QuestTaskGeneratedEvent>()) {
                text += "- ${event.questTask}\n"
                runBlocking {
                    WebSocketServer.sendMessage("New Quest: ${event.questTask}", event.sendToUsers)
                }
            }
            text += "\n"
        }

        return text
    }
}