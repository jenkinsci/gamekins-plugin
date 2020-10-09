package io.jenkins.plugins.gamekins.util

import hudson.maven.AbstractMavenProject
import hudson.model.AbstractItem
import hudson.model.Action
import hudson.model.Actionable
import hudson.model.User
import hudson.security.HudsonPrivateSecurityRealm.Details
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import io.jenkins.plugins.gamekins.GameUserProperty
import io.jenkins.plugins.gamekins.LeaderboardAction
import io.jenkins.plugins.gamekins.StatisticsAction
import io.jenkins.plugins.gamekins.property.GameProperty
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import java.io.IOException
import java.util.*
import java.util.function.Consumer

object PropertyUtil {

    @JvmStatic
    fun doAddTeam(property: GameProperty?, teamName: String): FormValidation {
        if (teamName.trim { it <= ' ' }.isEmpty()) return FormValidation.error("No team specified")
        if (property == null) return FormValidation.error("Unexpected Error")
        if (property.getTeams().contains(teamName))
            return FormValidation.error("The team already exists - please use another name for your team")
        try {
            property.addTeam(teamName)
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error("Unexpected Error")
        }
        return FormValidation.ok("Team successfully added")
    }

    @JvmStatic
    fun doAddUserToTeam(job: AbstractItem?, teamsBox: String, usersBox: String): FormValidation {
        if (teamsBox.trim { it <= ' ' }.isEmpty()) return FormValidation.error("No team specified")
        if (job == null) return FormValidation.error("Unexpected error: Parent job is null")
        for (user in User.getAll()) {
            if (user.getProperty(Details::class.java) == null) continue
            if (user.fullName == usersBox) {
                val projectName = job.name
                val property = user.getProperty(GameUserProperty::class.java)
                return if (property != null && !property.isParticipating(projectName)) {
                    property.setParticipating(projectName, teamsBox)
                    try {
                        user.save()
                    } catch (e: IOException) {
                        return FormValidation.error(e, "There was an error with saving")
                    }
                    FormValidation.ok("User successfully added")
                } else {
                    FormValidation.error("The user is already participating in a team")
                }
            }
        }
        return FormValidation.error("No user with the specified name found")
    }

    @JvmStatic
    fun doDeleteTeam(projectName: String, property: GameProperty?, teamsBox: String): FormValidation {
        if (teamsBox.trim { it <= ' ' }.isEmpty()) return FormValidation.error("No team specified")
        if (property == null) return FormValidation.error("Unexpected Error")
        if (!property.getTeams().contains(teamsBox)) return FormValidation.error("The specified team does not exist")
        for (user in User.getAll()) {
            if (user.getProperty(Details::class.java) == null) continue
            val userProperty = user.getProperty(GameUserProperty::class.java)
            if (userProperty != null && userProperty.isParticipating(projectName, teamsBox)) {
                userProperty.removeParticipation(projectName)
                try {
                    user.save()
                } catch (e: IOException) {
                    return FormValidation.error(e, "There was an error with saving")
                }
            }
        }
        try {
            property.removeTeam(teamsBox)
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error("Unexpected Error")
        }
        return FormValidation.ok("Team successfully deleted")
    }

    @JvmStatic
    fun doFillTeamsBoxItems(property: GameProperty?): ListBoxModel {
        val listBoxModel = ListBoxModel()
        property?.getTeams()?.forEach(Consumer { nameAndValue: String? -> listBoxModel.add(nameAndValue) })
        return listBoxModel
    }

    @JvmStatic
    fun doFillUsersBoxItems(projectName: String): ListBoxModel {
        val participatingUser = ArrayList<String>()
        val otherUsers = ArrayList<String>()
        for (user in User.getAll()) {
            if (user.getProperty(Details::class.java) == null) continue
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

    @JvmStatic
    fun doRemoveUserFromTeam(job: AbstractItem?, teamsBox: String, usersBox: String): FormValidation {
        if (teamsBox.trim { it <= ' ' }.isEmpty()) return FormValidation.error("No team specified")
        if (job == null) return FormValidation.error("Unexpected error: Parent job is null")
        for (user in User.getAll()) {
            if (user.getProperty(Details::class.java) == null) continue
            if (user.fullName == usersBox) {
                val projectName = job.name
                val property = user.getProperty(GameUserProperty::class.java)
                return if (property != null && property.isParticipating(projectName, teamsBox)) {
                    property.removeParticipation(projectName)
                    try {
                        user.save()
                    } catch (e: IOException) {
                        return FormValidation.error(e, "There was an error with saving")
                    }
                    FormValidation.ok("User successfully removed")
                } else {
                    FormValidation.error("The user is not in the specified team")
                }
            }
        }
        return FormValidation.error("No user with the specified name found")
    }

    @JvmStatic
    fun reconfigure(owner: AbstractItem, activated: Boolean, showStatistics: Boolean) {
        if (owner is WorkflowJob) {
            if (activated) {
                owner.addOrReplaceAction(LeaderboardAction(owner))
            } else {
                owner.removeAction(LeaderboardAction(owner))
            }
            if (showStatistics) {
                owner.addOrReplaceAction(StatisticsAction(owner))
            } else {
                owner.removeAction(StatisticsAction(owner))
            }
            try {
                owner.save()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else if (owner is WorkflowMultiBranchProject || owner is AbstractMavenProject<*, *>) {
            //TODO: Without reflection (Trigger)
            //TODO: Replace newInstance() with constructor call
            try {
                val actionField = Actionable::class.java.getDeclaredField("actions")
                actionField.isAccessible = true
                if (actionField[owner] == null) actionField[owner] = actionField.type.newInstance()
                if (activated) {
                    (actionField[owner] as MutableList<*>).removeIf { action -> action is LeaderboardAction }
                    (actionField[owner] as MutableList<Action?>).add(LeaderboardAction(owner))
                } else {
                    (actionField[owner] as MutableList<*>).removeIf { action -> action is LeaderboardAction }
                }
                if (showStatistics) {
                    (actionField[owner] as MutableList<*>).removeIf { action -> action is StatisticsAction }
                    (actionField[owner] as MutableList<Action?>).add(StatisticsAction(owner))
                } else {
                    (actionField[owner] as MutableList<*>).removeIf { action -> action is StatisticsAction }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            /*try {
                owner.save();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }
    }
}
