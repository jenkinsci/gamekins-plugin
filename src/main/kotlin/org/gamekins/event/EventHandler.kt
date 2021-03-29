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

package org.gamekins.event

import hudson.model.Run
import hudson.model.User
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.gamekins.event.user.*
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
        GlobalScope.launch { event.run() }
    }

    /**
     * Generates the mail text based on the current events.
     */
    fun generateMailText(projectName: String, build: Run<*, *>, user: User, list: ArrayList<UserEvent>): String {
        var text = "Hello ${user.fullName},\n\n"
        text += "here are your Gamekins results from run ${build.number} of project $projectName:\n\n"

        if (list.find { it is ChallengeSolvedEvent } != null) {
            text += "Challenges solved:\n"
            for (event in list.filterIsInstance<ChallengeSolvedEvent>()) {
                text += "- ${event.challenge.toEscapedString()}\n"
            }
            text += "\n"
        }

        if (list.find { it is ChallengeUnsolvableEvent } != null) {
            text += "New unsolvable Challenges:\n"
            for (event in list.filterIsInstance<ChallengeUnsolvableEvent>()) {
                text += "- ${event.challenge.toEscapedString()}\n"
            }
            text += "\n"
        }

        if (list.find { it is ChallengeGeneratedEvent } != null) {
            text += "Challenges generated:\n"
            for (event in list.filterIsInstance<ChallengeGeneratedEvent>()) {
                text += "- ${event.challenge.toEscapedString()}\n"
            }
            text += "\n"
        }

        if (list.find { it is AchievementSolvedEvent } != null) {
            text += "Achievements solved:\n"
            for (event in list.filterIsInstance<AchievementSolvedEvent>()) {
                text += "- ${event.achievement}\n"
            }
            text += "\n"
        }

        text += "View the build on ${build.absoluteUrl}\n"
        text += "View the leaderboard on ${build.parent.absoluteUrl}leaderboard/\n"
        text += "View your achievements on ${user.absoluteUrl}/achievements/"

        return text
    }
}