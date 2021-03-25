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

package org.gamekins.event.build

import hudson.model.Run
import hudson.model.User
import hudson.tasks.MailAddressResolver
import hudson.tasks.Mailer
import org.gamekins.event.EventHandler
import org.gamekins.event.user.*
import java.util.*
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.collections.ArrayList

/**
 * Created when a build is finished. Sends notification mails to the participants.
 *
 * @author Philipp Straubinger
 * @since 0.3
 */
class BuildFinishedEvent(projectName: String, branch: String, build: Run<*, *>)
    : BuildEvent(projectName, branch, build) {

    override fun run() {
        val events = EventHandler.getEvents()
            .filterIsInstance<UserEvent>()
            .filter { it.projectName == this.projectName && it.branch == this.branch }
        val userEvents = hashMapOf<User, ArrayList<UserEvent>>()
        events.forEach {
            var list = userEvents[it.user]
            if (list == null) list = arrayListOf()
            list.add(it)
            userEvents[it.user] = list
        }

        val mailer = Mailer.descriptor()
        for ((user, list) in userEvents) {
            if (user.getProperty(org.gamekins.GameUserProperty::class.java).getNotifications()) {
                val mail = MailAddressResolver.resolve(user)
                val msg = MimeMessage(mailer.createSession())
                msg.subject = "Gamekins results"
                msg.setFrom(InternetAddress("results@gamekins.org", "Gamekins"))
                msg.sentDate = Date()
                msg.addRecipient(Message.RecipientType.TO, Mailer.stringToAddress(mail, mailer.charset))
                msg.setText(generateMailText(user, list))
                try {
                    Transport.send(msg)
                } catch (e: MessagingException) {
                    e.printStackTrace()
                }
            }
        }

        this.delete = true
    }

    /**
     * Generates the mail text based on the current events.
     */
    private fun generateMailText(user: User, list: ArrayList<UserEvent>): String {
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