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

package org.gamekins

import hudson.model.*
import org.gamekins.challenge.Challenge
import org.gamekins.util.PropertyUtil
import jenkins.model.Jenkins
import org.kohsuke.stapler.StaplerProxy
import org.kohsuke.stapler.export.Exported
import org.kohsuke.stapler.export.ExportedBean
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Represents the Leaderboard displayed in the side panel of the job. Used to send the data to the Jetty server.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class LeaderboardAction(val job: AbstractItem) : ProminentProjectAction, Describable<LeaderboardAction>, StaplerProxy {

    /**
     * Returns the list of completed Challenges of the current project and user.
     */
    fun getCompletedChallenges(): CopyOnWriteArrayList<Challenge> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return CopyOnWriteArrayList()
        return property.getCompletedChallenges(job.name)
    }

    /**
     * Returns the list of current Challenges of the current project and user.
     */
    fun getCurrentChallenges(): CopyOnWriteArrayList<Challenge> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return CopyOnWriteArrayList()
        return property.getCurrentChallenges(job.name)
    }

    override fun getDescriptor(): Descriptor<LeaderboardAction> {
        return Jenkins.get().getDescriptorOrDie(javaClass) as Descriptor<LeaderboardAction>
    }

    override fun getDisplayName(): String {
        return "Leaderboard"
    }

    override fun getIconFileName(): String {
        return "/plugin/gamekins/icons/leaderboard.png"
    }

    /**
     * Returns the list of rejected Challenges of the current project and user.
     */
    fun getRejectedChallenges(): CopyOnWriteArrayList<Pair<Challenge, String>> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return CopyOnWriteArrayList()
        return property.getRejectedChallenges(job.name)
    }

    override fun getTarget(): Any {
        Jenkins.getInstanceOrNull()?.checkPermission(Item.READ)
        return this
    }

    /**
     * Returns the details of all teams of the current project.
     */
    fun getTeamDetails(): List<TeamDetails> {
        val details = ArrayList<TeamDetails>()
        for (user in User.getAll()) {
            if (!PropertyUtil.realUser(user)) continue
            val property = user.getProperty(GameUserProperty::class.java)
            if (property != null && property.isParticipating(job.name)) {
                var index = -1
                details.indices.forEach { i ->
                    if (details[i].teamName == property.getTeamName(job.name)) {
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

    /**
     * Returns the details of all users participating in the current project.
     */
    fun getUserDetails(): List<UserDetails> {
        val details = ArrayList<UserDetails>()
        for (user in User.getAll()) {
            if (!PropertyUtil.realUser(user)) continue
            val property = user.getProperty(GameUserProperty::class.java)
            if (property != null && property.isParticipating(job.name)) {
                details.add(
                        UserDetails(
                                user.fullName,
                                property.getTeamName(job.name),
                                property.getScore(job.name),
                                property.getCompletedChallenges(job.name).size,
                                user.absoluteUrl
                        )
                )
            }
        }

        details.sortWith(Comparator.comparingInt { obj: UserDetails -> obj.score })
        details.reverse()
        return details
    }

    /**
     * Returns whether the current logged in user is participating in the project. Shows his Challenges in the
     * Leaderboard if true.
     */
    fun isParticipating(): Boolean {
        val user: User = User.current() ?: return false
        val property = user.getProperty(GameUserProperty::class.java) ?: return false
        return property.isParticipating(job.name)
    }

    /**
     * Container for the details of a user displayed on the Leaderboard.
     *
     * @author Philipp Straubinger
     * @since 1.0
     */
    @ExportedBean(defaultVisibility = 999)
    class UserDetails(@get:Exported val userName: String, @get:Exported val teamName: String,
                      @get:Exported val score: Int, @get:Exported val completedChallenges: Int,
                      @get:Exported val url: String)

    /**
     * Container for the details of a team displayed on the Leaderboard.
     *
     * @author Philipp Straubinger
     * @since 1.0
     */
    @ExportedBean(defaultVisibility = 999)
    class TeamDetails(@get:Exported val teamName: String, @get:Exported var score: Int,
                      @get:Exported var completedChallenges: Int) {

        /**
         * Adds one additional completed Challenge to the team.
         */
        @Exported
        fun addCompletedChallenges(completedChallenges: Int) {
            this.completedChallenges += completedChallenges
        }

        /**
         * Adds one additional point to the score of the team.
         */
        @Exported
        fun addScore(score: Int) {
            this.score += score
        }
    }
}
