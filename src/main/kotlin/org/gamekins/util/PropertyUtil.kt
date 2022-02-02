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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import hudson.maven.AbstractMavenProject
import hudson.model.*
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import org.gamekins.GameUserProperty
import org.gamekins.LeaderboardAction
import org.gamekins.StatisticsAction
import org.gamekins.property.GameJobProperty
import org.gamekins.property.GameMultiBranchProperty
import org.gamekins.property.GameProperty
import org.acegisecurity.userdetails.UserDetails
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import java.io.IOException
import java.util.function.Consumer
import hudson.security.HudsonPrivateSecurityRealm.Details as Details

/**
 * Util object for interaction with properties.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
object PropertyUtil {

    private const val NO_TEAM = "No team specified"
    private const val UNEXPECTED_ERROR = "Unexpected Error"
    private const val ERROR_PARENT = "$UNEXPECTED_ERROR: Parent job is null"
    private const val ERROR_SAVING = "There was an error with saving"

    /**
     * Adds a new team [teamName]] to the [property]. Returns errors if the [teamName] is empty, the property is null,
     * the [teamName] already exists or an Exception is thrown.
     */
    @JvmStatic
    fun doAddTeam(property: GameProperty?, teamName: String): FormValidation {
        if (teamName.trim { it <= ' ' }.isEmpty()) return FormValidation.error(NO_TEAM)
        if (property == null) return FormValidation.error(UNEXPECTED_ERROR)
        if (property.getTeams().contains(teamName))
            return FormValidation.error("The team already exists - please use another name for your team")
        try {
            property.addTeam(teamName)
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error(UNEXPECTED_ERROR)
        }
        return FormValidation.ok("Team successfully added")
    }

    /**
     * Adds a user [usersBox] to a team [teamsBox] for a [job]. Returns errors if the [job] is null, the user is
     * already in a team, the user was not found or an Exception is thrown.
     */
    @JvmStatic
    fun doAddUserToTeam(job: AbstractItem?, teamsBox: String, usersBox: String): FormValidation {
        if (teamsBox.trim { it <= ' ' }.isEmpty()) return FormValidation.error(NO_TEAM)
        if (job == null) return FormValidation.error(ERROR_PARENT)

        val user = retrieveUser(usersBox)
        if (user != null) {
            val projectName = job.fullName
            val property = user.getProperty(GameUserProperty::class.java)
            return if (property != null && !property.isParticipating(projectName)) {
                property.setParticipating(projectName, teamsBox)
                try {
                    user.save()
                } catch (e: IOException) {
                    return FormValidation.error(e, ERROR_SAVING)
                }
                FormValidation.ok("User successfully added")
            } else {
                FormValidation.error("The user is already participating in a team")
            }
        }

        return FormValidation.error("No user with the specified name found")
    }

    /**
     * Deletes a team [teamsBox] in the [property] for the prject [projectName]. Throws an error if the [property] is
     * null, the team does not exist or an Exception is thrown.
     */
    @JvmStatic
    fun doDeleteTeam(projectName: String, property: GameProperty?, teamsBox: String): FormValidation {
        if (teamsBox.trim { it <= ' ' }.isEmpty()) return FormValidation.error(NO_TEAM)
        if (property == null) return FormValidation.error(UNEXPECTED_ERROR)
        if (!property.getTeams().contains(teamsBox)) return FormValidation.error("The specified team does not exist")
        for (user in User.getAll()) {
            if (!realUser(user)) continue
            val userProperty = user.getProperty(GameUserProperty::class.java)
            if (userProperty != null && userProperty.isParticipating(projectName, teamsBox)) {
                userProperty.removeParticipation(projectName)
                try {
                    user.save()
                } catch (e: IOException) {
                    return FormValidation.error(e, ERROR_SAVING)
                }
            }
        }
        try {
            property.removeTeam(teamsBox)
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error(UNEXPECTED_ERROR)
        }
        return FormValidation.ok("Team successfully deleted")
    }

    /**
     * Returns the list of teams of a [property].
     */
    @JvmStatic
    fun doFillTeamsBoxItems(property: GameProperty?): ListBoxModel {
        val listBoxModel = ListBoxModel()
        property?.getTeams()?.forEach(Consumer { nameAndValue: String? -> listBoxModel.add(nameAndValue) })
        return listBoxModel
    }

    /**
     * Returns the list of users that can sign into Jenkins. Already participating users in the project [projectName]
     * are listed first. System users are excluded.
     */
    @JvmStatic
    fun doFillUsersBoxItems(projectName: String): ListBoxModel {
        val participatingUser = ArrayList<String>()
        val otherUsers = ArrayList<String>()
        for (user in User.getAll()) {
            if (!realUser(user)) continue
            val property = user.getProperty(GameUserProperty::class.java)
            if (property != null) {
                if (property.isParticipating(projectName)) {
                    participatingUser.add(user.fullName)
                } else {
                    otherUsers.add(user.fullName)
                }
            }
        }

        participatingUser.addAll(otherUsers)
        participatingUser.remove("unknown")
        participatingUser.remove("root")
        participatingUser.remove("SYSTEM")
        val listBoxModel = ListBoxModel()
        participatingUser.forEach(Consumer { nameAndValue: String? -> listBoxModel.add(nameAndValue) })
        return listBoxModel
    }

    /**
     * Removes a participant [usersBox] from a team [teamsBox]. Returns an error if the [job] is null, the user was
     * not found or the suer is not in the team.
     */
    @JvmStatic
    fun doRemoveUserFromTeam(job: AbstractItem?, teamsBox: String, usersBox: String): FormValidation {
        if (teamsBox.trim { it <= ' ' }.isEmpty()) return FormValidation.error(NO_TEAM)
        if (job == null) return FormValidation.error(ERROR_PARENT)

        val user = retrieveUser(usersBox)
        if (user != null) {
            val projectName = job.fullName
            val property = user.getProperty(GameUserProperty::class.java)
            return if (property != null && property.isParticipating(projectName, teamsBox)) {
                property.removeParticipation(projectName)
                try {
                    user.save()
                } catch (e: IOException) {
                    return FormValidation.error(e, ERROR_SAVING)
                }
                FormValidation.ok("User successfully removed")
            } else {
                FormValidation.error("The user is not in the specified team")
            }
        }

        return FormValidation.error("No user with the specified name found")
    }

    /**
     * Removes all Challenges made via the current [job] and reinitializes he statistics Does not remove teams or
     * participations.
     */
    fun doReset(job: AbstractItem?, property: GameProperty?): FormValidation {
        if (job == null) return FormValidation.error(ERROR_PARENT)
        if (property == null) return FormValidation.error("Unexpected error: Parent job has no property")
        property.resetStatistics(job)
        for (user in User.getAll()) {
            if (!realUser(user)) continue
            val userProperty = user.getProperty(GameUserProperty::class.java)
            if (userProperty != null && userProperty.isParticipating(job.fullName)) {
                userProperty.reset(job.fullName)
                try {
                    user.save()
                } catch (e: IOException) {
                    return FormValidation.error(e, ERROR_SAVING)
                }
            }
        }
        return FormValidation.ok("Project Challenges successfully reset")
    }

    /**
     * Returns a map of teams and their members of the [job] as json.
     */
    fun doShowTeamMemberships(job: AbstractItem, property: GameProperty): String {
        val map = hashMapOf<String, ArrayList<String>>()
        property.getTeams().forEach { map[it] = arrayListOf() }

        for (user in User.getAll()) {
            if (!realUser(user)) continue
            val userProperty = user.getProperty(GameUserProperty::class.java)
            if (userProperty != null && userProperty.isParticipating(job.fullName)) {
                val teamName = userProperty.getTeamName(job.fullName)
                val list = map[teamName]
                if (list != null) {
                    list.add(user.fullName)
                    map[teamName] = list
                }
            }
        }

        return jacksonObjectMapper().writeValueAsString(map)
    }

    /**
     * Checks whether the provided user contains login information or not. Compatible with Jenkins < 2.277.1
     * with [UserDetails] and with Jenkins >= 2.2771 with [Details].
     */
    @JvmStatic
    fun realUser(user: User): Boolean {
        return user.properties.values.any { it is UserDetails || it is Details }
    }

    /**
     * Adds or removes a [LeaderboardAction] or [StatisticsAction] with the help of the according methods of the
     * [owner]. [WorkflowMultiBranchProject]s and [AbstractMavenProject]s do not support this methods and the actions
     * have to be added via reflection.
     */
    @JvmStatic
    fun reconfigure(owner: AbstractItem, showLeaderboard: Boolean, showStatistics: Boolean) {
        if (owner is WorkflowJob) {
            reconfigureWorkFlowJob(owner, showLeaderboard, showStatistics)
        } else if (owner is WorkflowMultiBranchProject || owner is AbstractMavenProject<*, *>) {
            reconfigureAbstractItem(owner, showLeaderboard, showStatistics)
        }
    }

    /**
     * Adds or removes a [LeaderboardAction] or [StatisticsAction] with the help of the according methods of the
     * [job].
     */
    private fun reconfigureWorkFlowJob(job: WorkflowJob, showLeaderboard: Boolean, showStatistics: Boolean) {

        if (showLeaderboard) {
            job.addOrReplaceAction(LeaderboardAction(job))
        } else {
            job.removeAction(LeaderboardAction(job))
        }
        if (showStatistics) {
            job.addOrReplaceAction(StatisticsAction(job))
        } else {
            job.removeAction(StatisticsAction(job))
        }
        try {
            job.save()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Adds or removes a [LeaderboardAction] or [StatisticsAction] with the help of the according methods of the
     * [job].
     */
    private fun reconfigureAbstractItem(job: AbstractItem, showLeaderboard: Boolean, showStatistics: Boolean) {

        try {
            val actionField = Actionable::class.java.getDeclaredField("actions")
            actionField.isAccessible = true
            if (actionField[job] == null) actionField[job] = actionField.type.newInstance()
            if (showLeaderboard) {
                (actionField[job] as MutableList<*>).removeIf { action -> action is LeaderboardAction }
                (actionField[job] as MutableList<Action?>).add(LeaderboardAction(job))
            } else {
                (actionField[job] as MutableList<*>).removeIf { action -> action is LeaderboardAction }
            }
            if (showStatistics) {
                (actionField[job] as MutableList<*>).removeIf { action -> action is StatisticsAction }
                (actionField[job] as MutableList<Action?>).add(StatisticsAction(job))
            } else {
                (actionField[job] as MutableList<*>).removeIf { action -> action is StatisticsAction }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieves the corresponding [GameProperty] of the [job] depending on its type.
     */
    fun retrieveGameProperty(job: AbstractItem): GameProperty? {
        val property = if (job is WorkflowMultiBranchProject) {
            job.properties.get(GameMultiBranchProperty::class.java)
        } else {
            (job as Job<*, *>).getProperty(GameJobProperty::class.java.name)
        }

        return if (property == null) null else property as GameProperty
    }

    /**
     * Retrieves the corresponding [GameProperty] of the [run] depending on its type.
     */
    fun retrieveGamePropertyFromRun(run: Run<*, *>): GameProperty? {
        return if (run.parent.parent is WorkflowMultiBranchProject) {
            retrieveGameProperty(run.parent.parent as WorkflowMultiBranchProject)
        } else {
            retrieveGameProperty(run.parent)
        }
    }

    /**
     * Retrieves the corresponding [User] according to the [fullName].
     */
    private fun retrieveUser(fullName: String): User? {
        for (user in User.getAll()) {
            if (!realUser(user)) continue
            if (user.fullName == fullName) {
                return user
            }
        }

        return null
    }
}
