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

package org.gamekins.util

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.FilePath
import hudson.model.AbstractItem
import hudson.model.AbstractProject
import hudson.model.User
import hudson.tasks.MailAddressResolver
import hudson.tasks.Mailer
import hudson.util.FormValidation
import org.gamekins.GameUserProperty
import org.gamekins.challenge.Challenge
import org.gamekins.challenge.ChallengeFactory
import org.gamekins.challenge.DummyChallenge
import org.gamekins.challenge.quest.Quest
import org.gamekins.file.FileDetails
import org.gamekins.property.GameFolderProperty
import org.gamekins.property.GameJobProperty
import org.gamekins.property.GameMultiBranchProperty
import org.gamekins.util.Constants.Parameters
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import java.io.IOException
import java.util.*
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Util object for interaction with actions.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
object ActionUtil {

    /**
     * Rejects a [Challenge] with the String representation [reject] and a [reason] and generates a new one
     * if possible.
     */
    fun doRejectChallenge(job: AbstractItem, reject: String, reason: String): FormValidation {
        var rejectReason = reason
        if (rejectReason.isEmpty()) return FormValidation.error(Constants.ERROR_NO_REASON)
        if (rejectReason.matches(Regex("\\s+"))) rejectReason = "No reason provided"

        val user: User = User.current()
                ?: return FormValidation.error(Constants.ERROR_NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return FormValidation.error(Constants.ERROR_RETRIEVING_PROPERTY)

        val projectName = job.fullName
        var challenge: Challenge? = null
        for (chal in property.getCurrentChallenges(projectName)) {
            if (chal.toEscapedString() == reject) {
                challenge = chal
                break
            }
        }

        if (challenge == null) return FormValidation.error(Constants.ERROR_NO_CHALLENGE_EXISTS)
        if (challenge is DummyChallenge) return FormValidation.error(Constants.ERROR_REJECT_DUMMY)
        property.rejectChallenge(projectName, challenge, rejectReason)

        val generatedText = generateChallengeAfterRejection(challenge, user, property, job)

        try {
            user.save()
            job.save()
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error(Constants.ERROR_SAVING)
        }

        return FormValidation.ok("Challenge rejected$generatedText")
    }

    /**
     * Rejects a [Quest] with the String representation [reject] and a [reason].
     */
    fun doRejectQuest(job: AbstractItem, reject: String, reason: String): FormValidation {
        var rejectReason = reason
        if (rejectReason.isEmpty()) return FormValidation.error(Constants.ERROR_NO_REASON)
        if (rejectReason.matches(Regex("\\s+"))) rejectReason = "No reason provided"

        val user: User = User.current()
            ?: return FormValidation.error(Constants.ERROR_NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return FormValidation.error(Constants.ERROR_RETRIEVING_PROPERTY)

        val projectName = job.fullName
        var quest: Quest? = null
        for (ques in property.getCurrentQuests(projectName)) {
            if (ques.toString() == reject) {
                quest = ques
                break
            }
        }

        if (quest == null) return FormValidation.error("The quest does not exist")
        if (quest.name == Constants.NO_QUEST || quest.name == Constants.REJECTED_QUEST) {
            return FormValidation.error(Constants.ERROR_REJECT_DUMMY)
        }
        property.rejectQuest(projectName, quest, rejectReason)
        property.newQuest(projectName, Quest(Constants.REJECTED_QUEST, arrayListOf()))

        try {
            user.save()
            job.save()
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error(Constants.ERROR_SAVING)
        }

        return FormValidation.ok("Quest rejected")
    }

    /**
     * Restores a [Challenge] with the String representation [reject].
     */
    fun doRestoreChallenge(job: AbstractItem, reject: String): FormValidation {
        val user: User = User.current()
            ?: return FormValidation.error(Constants.ERROR_NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return FormValidation.error(Constants.ERROR_RETRIEVING_PROPERTY)

        val projectName = job.fullName
        var challenge: Challenge? = null
        for ((chal, _) in property.getRejectedChallenges(projectName)) {
            if (chal.toEscapedString() == reject) {
                challenge = chal
                break
            }
        }

        if (challenge == null) return FormValidation.error(Constants.ERROR_NO_CHALLENGE_EXISTS)

        property.restoreChallenge(projectName, challenge)

        try {
            user.save()
            job.save()
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error(Constants.ERROR_SAVING)
        }

        return FormValidation.ok("Challenge restored")
    }

    /**
     * Stores a [Challenge] with the String representation [store] and generates a new one
     * if possible.
     */
    fun doStoreChallenge(job: AbstractItem, store: String): FormValidation {
        val user: User = User.current()
            ?: return FormValidation.error(Constants.ERROR_NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return FormValidation.error(Constants.ERROR_RETRIEVING_PROPERTY)

        val projectName = job.fullName
        var challenge: Challenge? = null
        for (chal in property.getCurrentChallenges(projectName)) {
            if (chal.toEscapedString() == store) {
                challenge = chal
                break
            }
        }

        if (challenge == null) return FormValidation.error(Constants.ERROR_NO_CHALLENGE_EXISTS)
        if (challenge is DummyChallenge) return FormValidation.error("Dummies cannot be stored " +
                "- please run another build")

        if (property.getStoredChallenges(projectName).size >=
            (job as AbstractProject<*, *>).getProperty(GameJobProperty::class.java).currentStoredChallengesCount)
            return FormValidation.error(Constants.ERROR_STORAGE_CAPACITY_REACHED)

        property.storeChallenge(projectName, challenge)

        val generatedText = generateChallengeAfterRejection(challenge, user, property, job)

        try {
            user.save()
            job.save()
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error(Constants.ERROR_SAVING)
        }

        return FormValidation.ok("Challenge stored$generatedText")
    }

    /**
     * Unshelves a [Challenge] with the String representation [store].
     */
    fun doUndoStoreChallenge(job: AbstractItem, store: String): FormValidation {
        val user: User = User.current()
            ?: return FormValidation.error(Constants.ERROR_NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return FormValidation.error(Constants.ERROR_RETRIEVING_PROPERTY)

        val projectName = job.fullName
        var challenge: Challenge? = null
        for (chal in property.getStoredChallenges(projectName)) {
            if (chal.toEscapedString() == store) {
                challenge = chal
                break
            }
        }

        if (challenge == null) return FormValidation.error(Constants.ERROR_NO_CHALLENGE_EXISTS)

        property.undoStoreChallenge(projectName, challenge)

        try {
            user.save()
            job.save()
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error(Constants.ERROR_SAVING)
        }

        return FormValidation.ok("Challenge restored")
    }

    /**
     * Sends a [Challenge] with the String representation [send] to [to].
     */
    fun doSendChallenge(job: AbstractItem, send: String, to: String): FormValidation {
        val user: User = User.current()
            ?: return FormValidation.error(Constants.ERROR_NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return FormValidation.error(Constants.ERROR_RETRIEVING_PROPERTY)

        val projectName = job.fullName
        var challenge: Challenge? = null
        for (chal in property.getStoredChallenges(projectName)) {
            if (chal.toEscapedString() == send) {
                challenge = chal
                break
            }
        }

        if (challenge == null) return FormValidation.error(Constants.ERROR_NO_CHALLENGE_EXISTS)

        val other: User = User.get(to, false, Collections.EMPTY_MAP)
            ?: return FormValidation.error(Constants.ERROR_USER_NOT_FOUND)
        val otherProperty = other.getProperty(GameUserProperty::class.java)
            ?: return FormValidation.error(Constants.ERROR_RETRIEVING_PROPERTY)

        if (user == other)
            return FormValidation.error(Constants.ERROR_RECEIVER_IS_SELF)

        if (otherProperty.getStoredChallenges(job.fullName).size >=
            (job as AbstractProject<*, *>).getProperty(GameJobProperty::class.java).currentStoredChallengesCount)
            return FormValidation.error(Constants.ERROR_STORAGE_CAPACITY_REACHED)
        property.removeStoredChallenge(projectName, challenge)
        otherProperty.addStoredChallenge(projectName, challenge)

        try {
            user.save()
            other.save()
            job.save()
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error(Constants.ERROR_SAVING)
        }

        val mailer = Mailer.descriptor()
        if (other.getProperty(GameUserProperty::class.java).getNotifications()) {
            val mail = MailAddressResolver.resolve(other)
            val msg = MimeMessage(mailer.createSession())
            msg.subject = "New Gamekins Challenge"
            msg.setFrom(InternetAddress("challenges@gamekins.org", "Gamekins"))
            msg.sentDate = Date()
            msg.addRecipient(Message.RecipientType.TO, Mailer.stringToAddress(mail, mailer.charset))
            msg.setText(generateMailText(projectName, challenge, other, user, job))
            try {
                Transport.send(msg)
            } catch (e: MessagingException) {
                e.printStackTrace()
            }
        }

        return FormValidation.ok("Challenge sent")
    }

    /**
     * Generates the mail text for receiving a challenge.
     */
    private fun generateMailText(projectName: String, challenge: Challenge, receiver: User, sender: User, job: AbstractProject<*, *>): String {
        var text = "Hello ${receiver.fullName},\n\n"
        text += "you have received a new challenge:\n\n"
        text += "Project: $projectName\n"
        text += "Sender: ${sender.fullName}\n"
        text += "Challenge: ${challenge.getName()}\n"

        text += "\nThe challenge is not immediately active and has to be unshelved from storage first.\n\n"
        text += "View the leaderboard on ${job.absoluteUrl}leaderboard/\n"
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
     * Generates a new Challenge according to the information in [challenge] for a [user] after rejection. Only
     * possible if the constants are set in the [challenge] (may not be after updating the plugin) and the workspace
     * is on the local machine.
     */
    @JvmStatic
    fun generateChallengeAfterRejection(challenge: Challenge, user: User, property: GameUserProperty,
                                        job: AbstractItem): String {

        val parameters = challenge.getParameters()
        var generatedText = ": No additional Challenge generated"
        if (!parameters.workspace.exists()) {
            parameters.workspace = FilePath(null, parameters.remote.replace(parameters.branch, "master"))
        }

        if (parameters.currentChallengesCount <= property.getCurrentChallenges(parameters.projectName).size)
        {
            generatedText += " (Enough Challenges already)"
        }
        else if (parameters.workspace.exists()) {
            val classes = PublisherUtil.retrieveLastChangedSourceAndTestFiles(
                Constants.DEFAULT_SEARCH_COMMIT_COUNT, parameters)
            generatedText = ": New Challenge generated"

            if (classes.isNotEmpty()) {
                generateAndUpdate(user, property, job, parameters, ArrayList(classes))
            } else {
                property.newChallenge(parameters.projectName, DummyChallenge(parameters, Constants.ERROR_GENERATION))
            }
        } else {
            generatedText += " (Workspace deleted or on remote machine)"
        }

        return generatedText
    }

    /**
     * Generates a new [Challenge] and updates the Statistics
     */
    private fun generateAndUpdate(user: User, property: GameUserProperty, job: AbstractItem,
                                  parameters: Parameters, files: ArrayList<FileDetails>) {
        val generated = ChallengeFactory.generateNewChallenges(
            user, property, parameters, files, maxChallenges = parameters.currentChallengesCount
        )
        val branch = if (parameters.remote.contains(parameters.branch.toRegex()))
            parameters.branch else "master"
        PropertyUtil.retrieveGameProperty(job)?.getStatistics()
                ?.addGeneratedAfterRejection(branch, generated)
    }
}
