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

package org.gamekins

import hudson.model.*
import org.gamekins.challenge.Challenge
import jenkins.model.Jenkins
import org.gamekins.property.GameJobProperty
import org.gamekins.property.GameMultiBranchProperty
import org.gamekins.questtask.QuestTask
import org.gamekins.util.ActionUtil
import org.gamekins.util.Pair
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.StaplerProxy
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Represents the Challenges and Quests displayed in the side panel of the job. Used to send the data to the Jetty server.
 *
 * @author Philipp Straubinger
 * @since 0.6
 */
class TaskAction(val job: AbstractItem) : ProminentProjectAction, Describable<TaskAction>, StaplerProxy {

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
     * Returns the list of completed QuestTasks of the current project and user.
     */
    fun getCompletedQuestTasks(): CopyOnWriteArrayList<QuestTask> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return CopyOnWriteArrayList()
        return property.getCompletedQuestTasks(job.fullName)
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
     * Returns the list of current QuestTasks of the current project and user.
     */
    fun getCurrentQuestTasks(): CopyOnWriteArrayList<QuestTask> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return CopyOnWriteArrayList()
        return property.getCurrentQuestTasks(job.fullName)
    }

    override fun getDescriptor(): Descriptor<TaskAction> {
        return Jenkins.get().getDescriptorOrDie(javaClass) as Descriptor<TaskAction>
    }

    override fun getDisplayName(): String {
        return "Challenges"
    }

    override fun getIconFileName(): String {
        return "/plugin/gamekins/icons/challenges.png"
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
     * Returns the list of stored Challenges of the current project and user.
     */
    fun getStoredChallenges(): CopyOnWriteArrayList<Challenge> {
        val user: User = User.current() ?: return CopyOnWriteArrayList()
        val property = user.getProperty(GameUserProperty::class.java)
            ?: return CopyOnWriteArrayList()
        return property.getStoredChallenges(job.fullName)
    }

    override fun getTarget(): Any {
        this.job.checkPermission(Job.READ)
        return this
    }

    override fun getUrlName(): String {
        return "challenges"
    }

    /**
     * Returns the details of all users participating in the current project.
     */
    fun getUserDetails(): List<ActionUtil.UserDetails> {
        return ActionUtil.getUserDetails(job)
    }

    /**
     * Returns the details of all users participating in the current project that are eligible for getting sent
     * Challenges from you.
     */
    fun getUserDetailsForSending(): List<ActionUtil.UserDetails> {
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
}
