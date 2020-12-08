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

package org.gamekins.util

import hudson.FilePath
import hudson.model.AbstractItem
import hudson.model.User
import hudson.util.FormValidation
import org.gamekins.GameUserProperty
import org.gamekins.challenge.Challenge
import org.gamekins.challenge.ChallengeFactory
import org.gamekins.challenge.DummyChallenge
import java.io.IOException

/**
 * Util object for interaction with actions.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
object ActionUtil {

    /**
     * Rejects a [Challenge] with the String representation [reject] and a [reason] and generates a new one
     * if possible.
     */
    fun doRejectChallenge(job: AbstractItem, reject: String, reason: String): FormValidation {
        var rejectReason = reason
        if (rejectReason.isEmpty()) return FormValidation.error("Please insert your reason for rejection")
        if (rejectReason.matches(Regex("\\s+"))) rejectReason = "No reason provided"

        val user: User = User.current()
                ?: return FormValidation.error("There is no user signed in")
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return FormValidation.error("Unexpected error while retrieving the property")

        val projectName = job.name
        var challenge: Challenge? = null
        for (chal in property.getCurrentChallenges(projectName)) {
            if (chal.toString() == reject) {
                challenge = chal
                break
            }
        }

        if (challenge == null) return FormValidation.error("The challenge does not exist")
        property.rejectChallenge(projectName, challenge, rejectReason)

        val generatedText = generateChallengeAfterRejection(challenge, user, property, job)

        try {
            user.save()
            job.save()
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error("Unexpected error while saving")
        }

        return FormValidation.ok("Challenge rejected$generatedText")
    }

    /**
     * Generates a new Challenge according to the information in [challenge] for a [user] after rejection. Only
     * possible if the constants are set in the [challenge] (may not be after updating the plugin) and the workspace
     * is on the local machine.
     */
    @JvmStatic
    fun generateChallengeAfterRejection(challenge: Challenge, user: User, property: GameUserProperty,
                                        job: AbstractItem): String {

        val constants = challenge.getConstants()
        var generatedText = ": No additional Challenge generated"
        if (constants["workspace"] != null) {
            var workspace = FilePath(null, constants["workspace"]!!)
            if (!workspace.exists() && constants["branch"] != null) {
                workspace = FilePath(null, constants["workspace"]!!.replace(constants["branch"]!!, "master"))
            }

            if (workspace.exists()) {
                val classes = PublisherUtil.retrieveLastChangedClasses(
                        workspace, GitUtil.DEFAULT_SEARCH_COMMIT_COUNT, constants)
                generatedText = ": New Challenge generated"

                if (classes.isNotEmpty()) {
                    generateAndUpdate(user, property, job, constants, classes, workspace)
                } else {
                    property.newChallenge(constants["projectName"]!!, DummyChallenge(constants))
                }
            } else {
                generatedText += " (Workspace deleted or on remote machine)"
            }
        }

        return generatedText
    }

    /**
     * Generates a new [Challenge] and updates the Statistics
     */
    private fun generateAndUpdate(user: User, property: GameUserProperty, job: AbstractItem,
                                  constants: HashMap<String, String>, classes: ArrayList<JacocoUtil.ClassDetails>,
                                  workspace: FilePath) {
        val generated = ChallengeFactory.generateNewChallenges(
                user, property, constants, classes, workspace)
        val branch = if (constants["branch"] != null
                && workspace.remote.contains(constants["branch"]!!.toRegex()))
            constants["branch"]!! else "master"
        PropertyUtil.retrieveGameProperty(job)?.getStatistics()
                ?.addGeneratedAfterRejection(branch, generated)
    }
}