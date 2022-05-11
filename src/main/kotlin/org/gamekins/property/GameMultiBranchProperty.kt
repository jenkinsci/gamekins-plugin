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

package org.gamekins.property

import com.cloudbees.hudson.plugins.folder.AbstractFolder
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor
import hudson.Extension
import hudson.model.AbstractItem
import hudson.model.Item
import hudson.model.JobPropertyDescriptor
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import jenkins.branch.OrganizationFolder
import org.gamekins.LeaderboardAction
import org.gamekins.StatisticsAction
import org.gamekins.statistics.Statistics
import org.gamekins.util.PropertyUtil
import net.sf.json.JSONObject
import org.gamekins.util.Constants
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.*
import java.io.IOException
import javax.annotation.Nonnull
import kotlin.jvm.Throws

/**
 * Adds the configuration for Gamekins to the configuration page of a [WorkflowMultiBranchProject].
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class GameMultiBranchProperty
@DataBoundConstructor constructor(job: AbstractItem?,
                                  @set:DataBoundSetter var activated: Boolean,
                                  @set:DataBoundSetter var showLeaderboard: Boolean,
                                  @set:DataBoundSetter var showStatistics: Boolean,
                                  @set:DataBoundSetter var currentChallengesCount: Int,
                                  @set:DataBoundSetter var currentQuestsCount: Int,
                                  @set:DataBoundSetter var currentStoredChallengesCount: Int,
                                  @set:DataBoundSetter var canSendChallenge: Boolean)
    : AbstractFolderProperty<AbstractFolder<*>?>(), GameProperty, StaplerProxy {

    private var statistics: Statistics
    private val teams: ArrayList<String> = ArrayList()

    /**
     * Call the reconfigure needed to add the actions to the left panel.
     */
    init {
        statistics = Statistics(job!!)
        PropertyUtil.reconfigure(job, showLeaderboard, showStatistics)
        if (currentChallengesCount <= 0) currentChallengesCount = Constants.DEFAULT_CURRENT_CHALLENGES
        if (currentQuestsCount <= 0) currentQuestsCount = Constants.DEFAULT_CURRENT_QUESTS
        if (currentStoredChallengesCount < 0) currentStoredChallengesCount = Constants.DEFAULT_STORED_CHALLENGES
    }

    @Throws(IOException::class)
    override fun addTeam(teamName: String) {
        teams.add(teamName)
        owner!!.save()
    }

    override fun getStatistics(): Statistics {
        if (statistics.isNotFullyInitialized()) {
            statistics = Statistics(owner!!)
        }
        return statistics
    }

    override fun getTarget(): Any {
        if (this.owner?.parent is OrganizationFolder) {
            this.owner?.checkPermission(Item.READ)
        } else {
            this.owner?.checkPermission(Item.CONFIGURE)
        }
        return this
    }

    override fun getTeams(): ArrayList<String> {
        return teams
    }

    /**
     * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
     */
    @Suppress("unused", "SENSELESS_COMPARISON")
    private fun readResolve(): Any {
        if (currentChallengesCount == 0) currentChallengesCount = Constants.DEFAULT_CURRENT_CHALLENGES
        if (currentQuestsCount <= 0) currentQuestsCount = Constants.DEFAULT_CURRENT_QUESTS
        if (currentStoredChallengesCount < 0) currentStoredChallengesCount = Constants.DEFAULT_STORED_CHALLENGES

        return this
    }

    /**
     * Sets the new values of [activated], [showLeaderboard], [showStatistics] and [currentChallengesCount], if the
     * job configuration has been saved. Also calls [PropertyUtil.reconfigure] to update the [LeaderboardAction]
     * and [StatisticsAction].
     *
     * @see [AbstractFolderProperty.reconfigure]
     */
    override fun reconfigure(req: StaplerRequest, form: JSONObject?): AbstractFolderProperty<*> {
        if (form != null) {
            activated = form.getBoolean("activated")
            showStatistics = form.getBoolean("showStatistics")
            showLeaderboard = form.getBoolean("showLeaderboard")
            if (form.getValue("currentChallengesCount") is String)
                currentChallengesCount = form.getInt("currentChallengesCount")
            if (form.getValue("currentQuestsCount") is String)
                currentQuestsCount = form.getInt("currentQuestsCount")
            if (form.getValue("storedChallengesCount") is String)
                currentQuestsCount = form.getInt("storedChallengesCount")
            canSendChallenge = form.getBoolean("canSendChallenge")
        }
        
        PropertyUtil.reconfigure(owner!!, showLeaderboard, showStatistics)
        return this
    }

    @Throws(IOException::class)
    override fun removeTeam(teamName: String) {
        teams.remove(teamName)
        owner!!.save()
    }

    override fun resetStatistics(job: AbstractItem) {
        statistics = Statistics(job)
        owner?.save()
    }

    /**
     * Registers the [GameMultiBranchProperty] to Jenkins as an extension and also works as an communication point
     * between the Jetty server and the [GameMultiBranchProperty].
     *
     * Cannot be outsourced in separate class, because the constructor of [AbstractFolderPropertyDescriptor]
     * does not take the base class like the [JobPropertyDescriptor].
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    @Extension
    class GameMultiBranchPropertyDescriptor : AbstractFolderPropertyDescriptor() {

        /**
         * Called from the Jetty server if the button to add a new team is pressed. Only allows a non-empty [teamName]
         * and adds them to the [job], from which the button has been clicked, via the method [PropertyUtil.doAddTeam].
         */
        fun doAddTeam(@AncestorInPath job: WorkflowMultiBranchProject?,
                      @QueryParameter teamName: String): FormValidation {
            if (job == null) return FormValidation.error("Unexpected error: Parent job is null")
            if (teamName.isEmpty()) return FormValidation.error("Insert a name for the team")
            val property = job.properties[this] as GameMultiBranchProperty
            val validation = PropertyUtil.doAddTeam(property, teamName)
            save()
            return validation
        }

        /**
         * Called from the Jetty server if the button to add a new participant to a team is pressed. Adds the
         * participant [usersBox] to the team [teamsBox] via the method [PropertyUtil.doAddUserToTeam].
         */
        fun doAddUserToTeam(@AncestorInPath job: WorkflowMultiBranchProject?,
                            @QueryParameter teamsBox: String?, @QueryParameter usersBox: String?): FormValidation {
            return PropertyUtil.doAddUserToTeam(job, teamsBox!!, usersBox!!)
        }

        /**
         * Called from the Jetty server if the button to delete a team is pressed. Deletes the team [teamsBox] of the
         * [job] via the method [PropertyUtil.doDeleteTeam].
         */
        fun doDeleteTeam(@AncestorInPath job: WorkflowMultiBranchProject?,
                         @QueryParameter teamsBox: String?): FormValidation {
            if (job == null) return FormValidation.error("Unexpected error: Parent job is null")
            val projectName = job.fullName
            val property = job.properties[this] as GameMultiBranchProperty
            val validation = PropertyUtil.doDeleteTeam(projectName, property, teamsBox!!)
            save()
            return validation
        }

        /**
         * Called from the Jetty server when the configuration page is displayed. Fills the combo box with the names of
         * all teams of the [job].
         */
        fun doFillTeamsBoxItems(@AncestorInPath job: WorkflowMultiBranchProject?): ListBoxModel {
            val property =
                    if (job == null || job.properties[this] == null) null
                    else job.properties[this] as GameMultiBranchProperty
            return PropertyUtil.doFillTeamsBoxItems(property)
        }

        /**
         * Called from the Jetty server when the configuration page is displayed. Fills the combo box with the names of
         * all users of the [job].
         */
        fun doFillUsersBoxItems(@AncestorInPath job: WorkflowMultiBranchProject): ListBoxModel {
            return PropertyUtil.doFillUsersBoxItems(job.fullName)
        }

        /**
         * Called from the Jetty server if the button to remove a participant from a team is pressed. Removes the
         * participant [usersBox] from the team [teamsBox] of the [job] via the method
         * [PropertyUtil.doRemoveUserFromTeam].
         */
        fun doRemoveUserFromTeam(@AncestorInPath job: WorkflowMultiBranchProject?, @QueryParameter teamsBox: String?,
                                 @QueryParameter usersBox: String?): FormValidation {
            return PropertyUtil.doRemoveUserFromTeam(job, teamsBox!!, usersBox!!)
        }
        /**
         * Called from the Jetty server if the button to reset Gamekins in the specific [job] is called. Deletes all
         * Challenges from all users for the current project and resets the statistics.
         */
        fun doReset(@AncestorInPath job: WorkflowMultiBranchProject?): FormValidation {
            val property =
                    if (job == null || job.properties[this] == null) null
                    else job.properties[this] as GameMultiBranchProperty
            return PropertyUtil.doReset(job, property)
        }

        fun doShowTeamMemberships(@AncestorInPath job: WorkflowMultiBranchProject?): String {
            val property =
                if (job == null || job.properties[this] == null) return ""
                else job.properties[this] as GameMultiBranchProperty
            return PropertyUtil.doShowTeamMemberships(job, property)
        }

        @Nonnull
        override fun getDisplayName(): String {
            return "Activate Gamekins"
        }

        /**
         * The [GameMultiBranchProperty] can only be added to jobs with the [containerType]
         * [WorkflowMultiBranchProject]. For other [containerType]s have a look at [GameJobProperty] and
         * [GameOrganizationFolderProperty].
         *
         * @see AbstractFolderPropertyDescriptor.isApplicable
         */
        override fun isApplicable(containerType: Class<out AbstractFolder<*>?>): Boolean {
            return containerType == WorkflowMultiBranchProject::class.java
        }

        /**
         * Returns a new instance of a [GameMultiBranchProperty] during creation and saving of a job.
         *
         * @see AbstractFolderPropertyDescriptor.newInstance
         */
        override fun newInstance(req: StaplerRequest?, formData: JSONObject): AbstractFolderProperty<*>? {
            return if (req == null || req.findAncestor(AbstractItem::class.java).getObject() == null) null
            else GameMultiBranchProperty(
                req.findAncestor(AbstractItem::class.java).getObject() as AbstractItem,
                formData.getBoolean("activated"),
                formData.getBoolean("showLeaderboard"),
                formData.getBoolean("showStatistics"),
                if (formData.getValue("currentChallengesCount") is Int)
                    formData.getInt("currentChallengesCount") else Constants.DEFAULT_CURRENT_CHALLENGES,
                if (formData.getValue("currentQuestsCount") is Int)
                    formData.getInt("currentQuestsCount") else Constants.DEFAULT_CURRENT_QUESTS,
                if (formData.getValue("storedChallengesCount") is Int)
                    formData.getInt("storedChallengesCount") else Constants.DEFAULT_STORED_CHALLENGES,
                formData.getBoolean("canSendChallenge")
            )
        }
    }
}
