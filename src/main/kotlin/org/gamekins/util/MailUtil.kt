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

package org.gamekins.util

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.model.AbstractItem
import hudson.model.User
import hudson.tasks.MailAddressResolver
import hudson.tasks.Mailer
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.gamekins.challenge.Challenge
import org.gamekins.property.GameFolderProperty
import org.gamekins.property.GameJobProperty
import org.gamekins.property.GameMultiBranchProperty
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import java.util.*


/**
 * Util object for sending mails
 *
 * @author Matthias Rainer
 * @since 0.6
 */
object MailUtil {

    /**
     * Generates the mail text for receiving a challenge.
     */
    fun generateMailText(projectName: String, challenge: Challenge, receiver: User, sender: User,
                                 job: AbstractItem): String {
        var text = "Hello ${receiver.fullName},\n\n"
        text += "you have received a new challenge:\n\n"
        text += "Project: $projectName\n"
        text += "Sender: ${sender.fullName}\n"
        text += "Challenge: ${challenge.getName()}\n"

        text += "\nThe challenge is not immediately active and has to be unshelved from storage first.\n\n"
        text += generateViewLeaderboardText(job)

        return text
    }

    /**
     * Generates the leaderboard part of the mail text.
     */
    fun generateViewLeaderboardText(job: AbstractItem): String {
        var text = if (job is WorkflowJob && job.parent is WorkflowMultiBranchProject) {
            "View the leaderboard on ${(job.parent as WorkflowMultiBranchProject).absoluteUrl}leaderboard/\n"
        } else {
            "View the leaderboard on ${job.absoluteUrl}leaderboard/\n"
        }
        val property = PropertyUtil.retrieveGameProperty(job)
        if (property is GameJobProperty || property is GameMultiBranchProperty) {
            if (job.parent is Folder
                && (job.parent as Folder).properties.get(GameFolderProperty::class.java).leaderboard) {
                text += "View the comprehensive leaderboard on " +
                        "${(job.parent as Folder).absoluteUrl}leaderboard/\n"
            }
            if (job.parent is WorkflowMultiBranchProject
                && (job.parent as WorkflowMultiBranchProject).parent is Folder
                && ((job.parent as WorkflowMultiBranchProject).parent as Folder)
                    .properties.get(GameFolderProperty::class.java).leaderboard) {
                text += "View the comprehensive leaderboard on " +
                        "${((job.parent as WorkflowMultiBranchProject).parent as Folder)
                            .absoluteUrl}leaderboard/\n"
            }
        }
        return text
    }

    /**
     * Sends a mail with the credentials provided by Jenkins.
     */
    fun sendMail(user: User, subject: String, addressFrom: String, name: String, text: String) {
        val mailer = Mailer.descriptor()
        val mail = MailAddressResolver.resolve(user)
        val msg = MimeMessage(mailer.createSession())
        msg.subject = subject
        msg.setFrom(InternetAddress(addressFrom, name))
        msg.sentDate = Date()
        msg.addRecipient(Message.RecipientType.TO, Mailer.stringToAddress(mail, mailer.charset))
        msg.setText(text)
        try {
            Transport.send(msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}