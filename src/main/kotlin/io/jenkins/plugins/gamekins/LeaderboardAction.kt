package io.jenkins.plugins.gamekins

import hudson.Extension
import hudson.model.*
import hudson.security.HudsonPrivateSecurityRealm.Details
import hudson.util.FormValidation
import io.jenkins.plugins.gamekins.challenge.Challenge
import jenkins.model.Jenkins
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.QueryParameter
import org.kohsuke.stapler.export.Exported
import org.kohsuke.stapler.export.ExportedBean
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.annotation.Nonnull

class LeaderboardAction(val job: AbstractItem) : ProminentProjectAction, Describable<LeaderboardAction> {
    override fun getIconFileName(): String {
        return "document.png"
    }

    override fun getDisplayName(): String {
        return "Leaderboard"
    }

    override fun getUrlName(): String {
        return "leaderboard"
    }

    val userDetails: List<UserDetails>
        get() {
            val details = ArrayList<UserDetails>()
            for (user in User.getAll()) {
                if (user.getProperty(Details::class.java) == null) continue
                val property = user.getProperty(GameUserProperty::class.java)
                if (property != null && property.isParticipating(job.name)) {
                    details.add(
                            UserDetails(
                                    user.fullName,
                                    property.getTeamName(job.name),
                                    property.getScore(job.name),
                                    property.getCompletedChallenges(job.name).size
                            )
                    )
                }
            }
            details.sortWith(Comparator.comparingInt { obj: UserDetails -> obj.score })
            details.reverse()
            return details
        }

    val teamDetails: List<TeamDetails>
        get() {
            val details = ArrayList<TeamDetails>()
            for (user in User.getAll()) {
                if (user.getProperty(Details::class.java) == null) continue
                val property = user.getProperty(GameUserProperty::class.java)
                if (property != null && property.isParticipating(job.name)) {
                    var index = -1
                    for (i in details.indices) {
                        val teamDetail = details[i]
                        if (teamDetail.teamName == property.getTeamName(job.name)) {
                            index = i
                        }
                    }
                    if (index != -1) {
                        details[index].addCompletedChallenges(property.getCompletedChallenges(job.name).size)
                        details[index].addScore(property.getScore(job.name))
                    } else {
                        details.add(
                                TeamDetails(
                                        property.getTeamName(job.name),
                                        property.getScore(job.name),
                                        property.getCompletedChallenges(job.name).size
                                )
                        )
                    }
                }
            }
            details.sortWith(Comparator.comparingInt { obj: TeamDetails -> obj.score })
            details.reverse()
            return details
        }

    val completedChallenges: CopyOnWriteArrayList<Challenge>
        get() {
            val user: User = User.current() ?: return CopyOnWriteArrayList()
            val property = user.getProperty(GameUserProperty::class.java)
                    ?: return CopyOnWriteArrayList()
            return property.getCompletedChallenges(job.name)
        }

    val currentChallenges: CopyOnWriteArrayList<Challenge>
        get() {
            val user: User = User.current() ?: return CopyOnWriteArrayList()
            val property = user.getProperty(GameUserProperty::class.java)
                    ?: return CopyOnWriteArrayList()
            return property.getCurrentChallenges(job.name)
        }

    val rejectedChallenges: CopyOnWriteArrayList<Challenge>
        get() {
            val user: User = User.current() ?: return CopyOnWriteArrayList()
            val property = user.getProperty(GameUserProperty::class.java)
                    ?: return CopyOnWriteArrayList()
            return property.getRejectedChallenges(job.name)
        }

    val isParticipating: Boolean
        get() {
            val user: User = User.current() ?: return false
            val property = user.getProperty(GameUserProperty::class.java) ?: return false
            return property.isParticipating(job.name)
        }

    /**
     * Gets the descriptor for this instance.
     *
     *
     *
     * [Descriptor] is a singleton for every concrete [Describable]
     * implementation, so if `a.getClass() == b.getClass()` then by default
     * `a.getDescriptor() == b.getDescriptor()` as well.
     * (In rare cases a single implementation class may be used for instances with distinct descriptors.)
     */
    override fun getDescriptor(): Descriptor<LeaderboardAction> {
        val jenkins = Jenkins.get()
        return jenkins.getDescriptorOrDie(javaClass) as Descriptor<LeaderboardAction>
    }

    @ExportedBean(defaultVisibility = 999)
    class UserDetails(@get:Exported val userName: String, @get:Exported val teamName: String,
                      @get:Exported val score: Int, @get:Exported val completedChallenges: Int)

    @ExportedBean(defaultVisibility = 999)
    class TeamDetails(@get:Exported val teamName: String, @get:Exported var score: Int,
                      @get:Exported var completedChallenges: Int) {

        @Exported
        fun addScore(score: Int) {
            this.score += score
        }

        @Exported
        fun addCompletedChallenges(completedChallenges: Int) {
            this.completedChallenges += completedChallenges
        }
    }

    @Extension
    class DescriptorImpl : Descriptor<LeaderboardAction>(LeaderboardAction::class.java) {
        /**
         * Human readable name of this kind of configurable object.
         * Should be overridden for most descriptors, if the display name is visible somehow.
         * As a fallback it uses [Class.getSimpleName] on [.clazz], so for example `MyThing`
         * from `some.pkg.MyThing.DescriptorImpl`.
         * Historically some implementations returned null as a way of hiding the descriptor from the UI,
         * but this is generally managed by an explicit method such as `isEnabled` or `isApplicable`.
         */
        @Nonnull
        override fun getDisplayName(): String {
            return super.getDisplayName()
        }

        fun doRejectChallenge(@AncestorInPath job: AbstractItem, @QueryParameter reject: String,
                              @QueryParameter reason: String): FormValidation {
            var reason = reason
            if (reason.isEmpty()) return FormValidation.error("Please insert your reason for rejection")
            if (reason.matches(Regex("\\s+"))) reason = "No reason provided"
            val user: User = User.current()
                    ?: return FormValidation.error("There is no user signed in")
            val property = user.getProperty(GameUserProperty::class.java)
                    ?: return FormValidation.error("Unexpected error")
            val projectName = job.name
            var challenge: Challenge? = null
            for (chal in property.getCurrentChallenges(projectName)) {
                if (chal.toString() == reject) {
                    challenge = chal
                    break
                }
            }
            if (challenge == null) return FormValidation.error("The challenge does not exist")
            property.rejectChallenge(projectName, challenge, reason)
            try {
                user.save()
            } catch (e: IOException) {
                e.printStackTrace()
                return FormValidation.error("Unexpected error")
            }
            return FormValidation.ok("Challenge rejected")
        }

        init {
            load()
        }
    }
}
