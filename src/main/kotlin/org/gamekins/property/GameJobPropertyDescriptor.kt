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

package org.gamekins.property

import hudson.Extension
import hudson.maven.AbstractMavenProject
import hudson.model.*
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.gamekins.util.PropertyUtil
import net.sf.json.JSONObject
import org.gamekins.util.Constants
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.QueryParameter
import org.kohsuke.stapler.StaplerRequest
import javax.annotation.Nonnull

/**
 * Registers the [GameJobProperty] to Jenkins as an extension and also works as an communication point between the
 * Jetty server and the [GameJobProperty].
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
@Extension
class GameJobPropertyDescriptor : JobPropertyDescriptor(GameJobProperty::class.java) {

    /**
     * Called from the Jetty server if the button to add a new team is pressed. Only allows a non-empty [teamName] and
     * adds them to the [job], from which the button has been clicked, via the method [PropertyUtil.doAddTeam].
     */
    fun doAddTeam(@AncestorInPath job: Job<*, *>?, @QueryParameter teamName: String): FormValidation {
        if (teamName.isEmpty()) return FormValidation.error(Constants.Error.NO_TEAM_NAME)
        val property = if (job == null) null else job.properties[this] as GameJobProperty?
        val validation = PropertyUtil.doAddTeam(property, teamName)
        save()
        return validation
    }

    /**
     * Called from the Jetty server if the button to add a new participant to a team is pressed. Adds the participant
     * [usersBox] to the team [teamsBox] via the method [PropertyUtil.doAddUserToTeam].
     */
    fun doAddUserToTeam(@AncestorInPath job: Job<*, *>?, @QueryParameter teamsBox: String?,
                        @QueryParameter usersBox: String?): FormValidation {
        return PropertyUtil.doAddUserToTeam(job, teamsBox!!, usersBox!!)
    }

    /**
     * Called from the Jetty server if the button to delete a team is pressed. Deletes the team [teamsBox] of the
     * [job] via the method [PropertyUtil.doDeleteTeam].
     */
    fun doDeleteTeam(@AncestorInPath job: Job<*, *>?, @QueryParameter teamsBox: String?): FormValidation {
        if (job == null) return FormValidation.error(Constants.Error.PARENT)
        val projectName = job.fullName
        val property = job.properties[this] as GameJobProperty
        val validation = PropertyUtil.doDeleteTeam(projectName, property, teamsBox!!)
        save()
        return validation
    }

    /**
     * Called from the Jetty server when the configuration page is displayed. Fills the combo box with the names of
     * all teams of the [job].
     */
    fun doFillTeamsBoxItems(@AncestorInPath job: Job<*, *>?, @QueryParameter includeNoTeam: Boolean): ListBoxModel {
        val property =
                if (job == null || job.properties[this] == null) null
                else job.properties[this] as GameJobProperty?
        return PropertyUtil.doFillTeamsBoxItems(property, includeNoTeam)
    }

    /**
     * Called from the Jetty server when the configuration page is displayed. Fills the combo box with the names of
     * all users of the [job].
     */
    fun doFillUsersBoxItems(@AncestorInPath job: Job<*, *>?): ListBoxModel {
        return if (job == null) ListBoxModel() else PropertyUtil.doFillUsersBoxItems(job.fullName)
    }

    /**
     * Called from the Jetty server if the button to remove a participant from a team is pressed. Removes the
     * participant [usersBox] from the project of the [job] via the method [PropertyUtil.doRemoveUserFromProject].
     */
    fun doRemoveUserFromProject(@AncestorInPath job: Job<*, *>?, @QueryParameter usersBox: String?): FormValidation {
        return PropertyUtil.doRemoveUserFromProject(job, usersBox!!)
    }

    /**
     * Called from the Jetty server if the button to participate alone is pressed. Adds the participant
     * [usersBox] to a Pseudo-Team via the method [PropertyUtil.doAddUserToTeam].
     */
    fun doParticipateAlone(@AncestorInPath job: Job<*, *>?, @QueryParameter usersBox: String?): FormValidation {
        return PropertyUtil.doAddUserToTeam(job, Constants.NO_TEAM_TEAM_NAME, usersBox!!)
    }

    /**
     * Called from the Jetty server if the button to reset Gamekins in the specific [job] is pressed. Deletes all
     * Challenges from all users for the current project and resets the statistics.
     */
    fun doReset(@AncestorInPath job: Job<*, *>?): FormValidation {
        val property =
                if (job == null || job.properties[this] == null) null
                else job.properties[this] as GameJobProperty?
        return PropertyUtil.doReset(job, property)
    }

    /**
     * Called from the Jetty server if the button to show the team memberships in the specific [job] is pressed.
     * Returns a map of teams and their members as json.
     */
    fun doShowTeamMemberships(@AncestorInPath job: Job<*, *>?): String {
        val property =
            if (job == null || job.properties[this] == null) return ""
            else job.properties[this] as GameJobProperty
        return PropertyUtil.doShowTeamMemberships(job, property)
    }

    @Nonnull
    override fun getDisplayName(): String {
        return "Set the activation of the Gamekins plugin."
    }

    /**
     * The [GameJobProperty] can only be added to jobs with the [jobType] [FreeStyleProject], [WorkflowJob] and
     * [AbstractMavenProject]. For other [jobType]s have a look at [GameMultiBranchProperty] and
     * [GameOrganizationFolderProperty].
     *
     * @see JobPropertyDescriptor.isApplicable
     */
    override fun isApplicable(jobType: Class<out Job<*, *>?>): Boolean {
        return jobType == FreeStyleProject::class.java || jobType == WorkflowJob::class.java
                || AbstractMavenProject::class.java.isAssignableFrom(jobType)
    }

    /**
     * Returns a new instance of a [GameJobProperty] during creation and saving of a job.
     *
     * @see JobPropertyDescriptor.newInstance
     */
    override fun newInstance(req: StaplerRequest?, formData: JSONObject): JobProperty<*>? {
        return if (req == null || req.findAncestor(AbstractItem::class.java).getObject() == null) null
        else GameJobProperty(
            req.findAncestor(AbstractItem::class.java).getObject() as AbstractItem,
            formData.getBoolean(Constants.FormKeys.ACTIVATED),
            formData.getBoolean(Constants.FormKeys.SHOW_LEADERBOARD),
            formData.getBoolean(Constants.FormKeys.SHOW_STATISTICS),
            if (formData.getValue(Constants.FormKeys.CHALLENGES_COUNT) is Int)
                formData.getInt(Constants.FormKeys.CHALLENGES_COUNT) else Constants.Default.CURRENT_CHALLENGES,
            if (formData.getValue(Constants.FormKeys.QUEST_COUNT) is Int)
                formData.getInt(Constants.FormKeys.QUEST_COUNT) else Constants.Default.CURRENT_QUESTS,
            if (formData.getValue(Constants.FormKeys.STORED_CHALLENGES_COUNT) is Int)
                formData.getInt(Constants.FormKeys.STORED_CHALLENGES_COUNT) else Constants.Default.STORED_CHALLENGES,
            formData.getBoolean(Constants.FormKeys.CAN_SEND_CHALLENGE),
            if (formData.getValue(Constants.FormKeys.SEARCH_COMMIT_COUNT) is Int)
                formData.getInt(Constants.FormKeys.SEARCH_COMMIT_COUNT) else Constants.Default.SEARCH_COMMIT_COUNT,
            formData.getString(Constants.FormKeys.PIT_CONFIGURATION),
            formData.getBoolean(Constants.FormKeys.SHOW_PIT_OUTPUT)
        )
    }
}
