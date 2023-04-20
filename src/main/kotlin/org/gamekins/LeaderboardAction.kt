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

package org.gamekins

import hudson.model.*
import org.gamekins.challenge.Challenge
import org.gamekins.util.PropertyUtil
import jenkins.model.Jenkins
import org.gamekins.challenge.quest.Quest
import org.gamekins.property.GameJobProperty
import org.gamekins.property.GameMultiBranchProperty
import org.gamekins.util.Constants
import org.gamekins.util.Pair
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.StaplerProxy
import org.kohsuke.stapler.export.Exported
import org.kohsuke.stapler.export.ExportedBean
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Represents the Leaderboard displayed in the side panel of the job. Used to send the data to the Jetty server.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class LeaderboardAction(val job: AbstractItem) : ProminentProjectAction, Describable<LeaderboardAction>, StaplerProxy {

    /**
     * Returns the list of completed Challenges of the current project and user.
     */
    fun getCompletedChallenges(): CopyOnWriteArrayList<Challenge> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return CopyOnWriteArrayList()
        return property.getCompletedChallenges(job.fullName)
    }

    /**
     * Returns the list of completed Quests of the current project and user.
     */
    fun getCompletedQuests(): CopyOnWriteArrayList<Quest> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return CopyOnWriteArrayList()
        return property.getCompletedQuests(job.fullName)
    }

    /**
     * Returns the list of current Challenges of the current project and user.
     */
    fun getCurrentChallenges(): CopyOnWriteArrayList<Challenge> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return CopyOnWriteArrayList()
        return property.getCurrentChallenges(job.fullName)
    }

    /**
     * Returns the list of stored Challenges of the current project and user.
     */
    fun getStoredChallenges(): CopyOnWriteArrayList<Challenge> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return CopyOnWriteArrayList()
        return property.getStoredChallenges(job.fullName)
    }

    /**
     * Returns the list of current Quests of the current project and user.
     */
    fun getCurrentQuests(): CopyOnWriteArrayList<Quest> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return CopyOnWriteArrayList()
        return property.getCurrentQuests(job.fullName)
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
        return property.getRejectedChallenges(job.fullName)
    }

    /**
     * Returns the list of rejected Quests of the current project and user.
     */
    fun getRejectedQuests(): CopyOnWriteArrayList<Pair<Quest, String>> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return CopyOnWriteArrayList()
        return property.getRejectedQuests(job.fullName)
    }

    override fun getTarget(): Any {
        this.job.checkPermission(Job.READ)
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
            if (property != null && property.isParticipating(job.fullName)) {
                var index = -1
                details.indices.forEach { i ->
                    if (details[i].teamName == property.getTeamName(job.fullName)) {
                        index = i
                    }
                }

                if (index != -1) {
                    details[index].addCompletedAchievements(property.getCompletedAchievements(job.fullName).size)
                    details[index].addCompletedChallenges(property.getCompletedChallenges(job.fullName).size)
                    details[index].addCompletedQuests(property.getCompletedQuests(job.fullName).size)
                    details[index].addUnfinishedQuests(property.getUnfinishedQuests(job.fullName).size)
                    details[index].addScore(property.getScore(job.fullName))
                } else {
                    details.add(
                        TeamDetails(
                            property.getTeamName(job.fullName),
                            property.getScore(job.fullName),
                            property.getCompletedChallenges(job.fullName).size,
                            property.getCompletedQuests(job.fullName).size,
                            property.getUnfinishedQuests(job.fullName).size,
                            property.getCompletedAchievements(job.fullName).size
                        )
                    )
                }
            }
        }

        details.removeIf { it.teamName == Constants.NO_TEAM_TEAM_NAME }
        return details
            .sortedWith(
                compareBy({it.score}, {it.completedChallenges}, {it.completedQuests}, {it.completedAchievements}))
            .reversed()
    }

    /**
     * Returns the list of unfinished Quests of the current project and user.
     */
    fun getUnfinishedQuests(): CopyOnWriteArrayList<Quest> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return CopyOnWriteArrayList()
        return property.getUnfinishedQuests(job.fullName)
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
            if (property != null && property.isParticipating(job.fullName)) {
                details.add(
                    UserDetails(
                        user.fullName,
                        property.getTeamName(job.fullName),
                        property.getScore(job.fullName),
                        property.getCompletedChallenges(job.fullName).size,
                        property.getCompletedQuests(job.fullName).size,
                        property.getUnfinishedQuests(job.fullName).size,
                        property.getCompletedAchievements(job.fullName).size,
                        user.absoluteUrl,
                        property.getCurrentAvatar()
                    )
                )
            }
        }

        return details
            .sortedWith(
                compareBy({it.score}, {it.completedChallenges}, {it.completedQuests}, {it.completedAchievements}))
            .reversed()
    }

    /**
     * Returns the details of all users participating in the current project that are eligible for getting sent
     * Challenges from you.
     */
    fun getUserDetailsForSending(): List<UserDetails> {
        val details = CopyOnWriteArrayList(getUserDetails())

        details.removeIf { ud -> User.current()?.fullName.equals(ud.userName) }

        return details
    }

    /**
     * Returns whether the current logged in user is participating in the project. Shows his Challenges in the
     * Leaderboard if true.
     */
    fun isParticipating(): Boolean {
        val user: User = User.current() ?: return false
        val property = user.getProperty(GameUserProperty::class.java) ?: return false
        return property.isParticipating(job.fullName)
    }
    /**
     * Returns the maximal amount of stored Challenges
     */
    fun getStoredChallengesLimit(): Int {
        return when (job) {
            is WorkflowMultiBranchProject -> {
                job.properties.get(GameMultiBranchProperty::class.java).currentStoredChallengesCount
            }
            is WorkflowJob -> {
                job.getProperty(GameJobProperty::class.java).currentStoredChallengesCount
            }
            else -> {
                (job as AbstractProject<*, *>).getProperty(GameJobProperty::class.java).currentStoredChallengesCount
            }
        }
    }

    /**
     * Returns the current amount of stored Challenges
     */
    fun getStoredChallengesCount(): Int {
        return getStoredChallenges().size
    }

    /**
     * Returns whether challenges can be sent
     */
    fun getCanSend(): Boolean {
        return when (job) {
            is WorkflowMultiBranchProject -> {
                job.properties.get(GameMultiBranchProperty::class.java).canSendChallenge
            }
            is WorkflowJob -> {
                job.getProperty(GameJobProperty::class.java).canSendChallenge
            }
            else -> {
                (job as AbstractProject<*, *>).getProperty(GameJobProperty::class.java).canSendChallenge
            }
        }
    }

    /**
     * Container for the details of a user displayed on the Leaderboard.
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    @ExportedBean(defaultVisibility = 999)
    class UserDetails(@get:Exported val userName: String, @get:Exported val teamName: String,
                      @get:Exported val score: Int, @get:Exported val completedChallenges: Int,
                      @get:Exported val completedQuests: Int, @get:Exported val unfinishedQuests: Int,
                      @get:Exported val completedAchievements: Int, @get:Exported val url: String,
                      @get:Exported val image: String)

    /**
     * Container for the details of a team displayed on the Leaderboard.
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    @ExportedBean(defaultVisibility = 999)
    class TeamDetails(@get:Exported val teamName: String, @get:Exported var score: Int,
                      @get:Exported var completedChallenges: Int, @get:Exported var completedQuests: Int,
                      @get:Exported var unfinishedQuests: Int, @get:Exported var completedAchievements: Int) {

        /**
         * Adds additional completed Achievements to the team.
         */
        @Exported
        fun addCompletedAchievements(completedAchievements: Int) {
            this.completedAchievements += completedAchievements
        }

        /**
         * Adds additional completed Challenges to the team.
         */
        @Exported
        fun addCompletedChallenges(completedChallenges: Int) {
            this.completedChallenges += completedChallenges
        }

        /**
         * Adds additional completed Quests to the team.
         */
        @Exported
        fun addCompletedQuests(completedQuests: Int) {
            this.completedQuests += completedQuests
        }

        /**
         * Adds additional completed Quests to the team.
         */
        @Exported
        fun addUnfinishedQuests(unfinishedQuests: Int) {
            this.unfinishedQuests += unfinishedQuests
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
