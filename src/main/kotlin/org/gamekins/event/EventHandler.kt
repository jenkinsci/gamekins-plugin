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

package org.gamekins.event

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.model.Run
import hudson.model.User
import org.gamekins.event.user.*
import org.gamekins.property.GameFolderProperty
import org.gamekins.property.GameJobProperty
import org.gamekins.property.GameMultiBranchProperty
import org.gamekins.util.PropertyUtil
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
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
    fun generateChallengesText(list: ArrayList<UserEvent>): String {
        var text = ""
        if (list.find { it is ChallengeSolvedEvent } != null) {
            text += "Challenge(s) solved:\n"
            for (event in list.filterIsInstance<ChallengeSolvedEvent>()) {
                text += "- ${event.challenge.toEscapedString()}\n"
            }
            text += "\n"
        }

        if (list.find { it is ChallengeUnsolvableEvent } != null) {
            text += "New unsolvable Challenge(s):\n"
            for (event in list.filterIsInstance<ChallengeUnsolvableEvent>()) {
                text += "- ${event.challenge.toEscapedString()}\n"
            }
            text += "\n"
        }

        if (list.find { it is ChallengeGeneratedEvent } != null) {
            text += "Challenge(s) generated:\n"
            for (event in list.filterIsInstance<ChallengeGeneratedEvent>()) {
                text += "- ${event.challenge.toEscapedString()}\n"
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

        text += generateQuestsText(list)

        if (list.find { it is AchievementSolvedEvent } != null) {
            text += "Achievement(s) solved:\n"
            for (event in list.filterIsInstance<AchievementSolvedEvent>()) {
                text += "- ${event.achievement}\n"
            }
            text += "\n"
        }

        text += "View the build on ${build.parent.absoluteUrl}${build.number}/\n"
        text += "View the leaderboard on ${build.parent.absoluteUrl}leaderboard/\n"
        val property = PropertyUtil.retrieveGamePropertyFromRun(build)
        if (property is GameJobProperty || property is GameMultiBranchProperty) {
            if (build.parent.parent is Folder
                && (build.parent.parent as Folder).properties.get(GameFolderProperty::class.java).leaderboard) {
                text += "View the comprehensive leaderboard on " +
                        "${(build.parent.parent as Folder).absoluteUrl}leaderboard/\n"
            }
            if (build.parent.parent is WorkflowMultiBranchProject
                && (build.parent.parent as WorkflowMultiBranchProject).parent is Folder
                && ((build.parent.parent as WorkflowMultiBranchProject).parent as Folder)
                    .properties.get(GameFolderProperty::class.java).leaderboard) {
                text += "View the comprehensive leaderboard on " +
                        "${((build.parent.parent as WorkflowMultiBranchProject).parent as Folder)
                            .absoluteUrl}leaderboard/\n"
            }
        }

        text += "View your achievements on ${user.absoluteUrl}/achievements/"

        return text
    }

    /**
     * Generates the text for all [Event]s based on Quests.
     */
    fun generateQuestsText(list: ArrayList<UserEvent>): String {
        var text = ""
        if (list.find { it is QuestStepSolvedEvent } != null) {
            text += "Quest step(s) solved:\n"
            for (event in list.filterIsInstance<QuestStepSolvedEvent>()) {
                text += "- ${event.quest}: ${event.questStep}\n"
            }
            text += "\n"
        }

        if (list.find { it is QuestSolvedEvent } != null) {
            text += "Quest(s) solved:\n"
            for (event in list.filterIsInstance<QuestSolvedEvent>()) {
                text += "- ${event.quest}\n"
            }
            text += "\n"
        }

        if (list.find { it is QuestUnsolvableEvent } != null) {
            text += "New unsolvable Quest(s):\n"
            for (event in list.filterIsInstance<QuestUnsolvableEvent>()) {
                text += "- ${event.quest}\n"
            }
            text += "\n"
        }

        if (list.find { it is QuestGeneratedEvent } != null) {
            text += "Quest(s) generated:\n"
            for (event in list.filterIsInstance<QuestGeneratedEvent>()) {
                text += "- ${event.quest}\n"
            }
            text += "\n"
        }

        return text
    }
}