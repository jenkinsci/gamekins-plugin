package io.jenkins.plugins.gamekins

import hudson.model.*
import hudson.security.HudsonPrivateSecurityRealm.Details
import io.jenkins.plugins.gamekins.challenge.Challenge
import jenkins.model.Jenkins
import org.kohsuke.stapler.export.Exported
import org.kohsuke.stapler.export.ExportedBean
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class LeaderboardAction(val job: AbstractItem) : ProminentProjectAction, Describable<LeaderboardAction> {

    fun getCompletedChallenges(): CopyOnWriteArrayList<Challenge> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return CopyOnWriteArrayList()
        return property.getCompletedChallenges(job.name)
    }

    fun getCurrentChallenges(): CopyOnWriteArrayList<Challenge> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return CopyOnWriteArrayList()
        return property.getCurrentChallenges(job.name)
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
    override fun getDescriptor(): Descriptor<LeaderboardAction>? {
        return Jenkins.get().getDescriptorOrDie(javaClass) as Descriptor<LeaderboardAction>
    }

    override fun getDisplayName(): String {
        return "Leaderboard"
    }

    override fun getIconFileName(): String {
        return "document.png"
    }

    fun getRejectedChallenges(): CopyOnWriteArrayList<Challenge> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return CopyOnWriteArrayList()
        return property.getRejectedChallenges(job.name)
    }

    fun getTeamDetails(): List<TeamDetails> {
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

    override fun getUrlName(): String {
        return "leaderboard"
    }

    fun getUserDetails(): List<UserDetails> {
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

    fun isParticipating(): Boolean {
        val user: User = User.current() ?: return false
        val property = user.getProperty(GameUserProperty::class.java) ?: return false
        return property.isParticipating(job.name)
    }

    @ExportedBean(defaultVisibility = 999)
    class UserDetails(@get:Exported val userName: String, @get:Exported val teamName: String,
                      @get:Exported val score: Int, @get:Exported val completedChallenges: Int)

    @ExportedBean(defaultVisibility = 999)
    class TeamDetails(@get:Exported val teamName: String, @get:Exported var score: Int,
                      @get:Exported var completedChallenges: Int) {

        @Exported
        fun addCompletedChallenges(completedChallenges: Int) {
            this.completedChallenges += completedChallenges
        }

        @Exported
        fun addScore(score: Int) {
            this.score += score
        }
    }
}
