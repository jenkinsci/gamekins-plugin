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
import org.gamekins.util.ActionUtil
import org.gamekins.util.PropertyUtil
import org.kohsuke.stapler.StaplerProxy

/**
 * Represents the Leaderboard displayed in the side panel of a folder. Used to send the data to the Jetty server.
 *
 * @author Philipp Straubinger
 * @since 0.4
 */
class FolderLeaderboardAction(val job: AbstractItem) : ProminentProjectAction, StaplerProxy {

    override fun getDisplayName(): String {
        return "Leaderboard"
    }

    override fun getIconFileName(): String {
        return "/plugin/gamekins/icons/leaderboard.png"
    }

    override fun getTarget(): Any {
        this.job.checkPermission(Job.READ)
        return this
    }

    override fun getUrlName(): String {
        return "leaderboard"
    }

    /**
     * Returns the details of all users participating in all subprojects.
     */
    fun getUserDetails(): List<ActionUtil.UserDetails> {
        val details = ArrayList<ActionUtil.UserDetails>()
        for (user in User.getAll()) {
            if (!PropertyUtil.realUser(user)) continue
            val property = user.getProperty(GameUserProperty::class.java)
            val projects = property.isParticipatingInSubProjects(job.fullName)
            if (projects.isNotEmpty()) {
                var score = 0
                var challenges = 0
                var quests = 0
                var questTasks = 0
                var unfinishedQuests = 0
                var achievements = 0

                projects.forEach { project ->
                    score += property.getScore(project)
                    challenges += property.getCompletedChallenges(project).size
                    quests += property.getCompletedQuests(project).size
                    questTasks += property.getCompletedQuestTasks(project).size
                    unfinishedQuests += property.getUnfinishedQuests(project).size
                    achievements += property.getCompletedAchievements(project).size
                }

                details.add(
                    ActionUtil.UserDetails(
                        user.fullName,
                        property.getTeamName(job.fullName),
                        score,
                        challenges,
                        quests,
                        questTasks,
                        unfinishedQuests,
                        achievements,
                        user.absoluteUrl,
                        property.getCurrentAvatar()
                    )
                )
            }
        }

        return details
            .sortedWith(compareBy({it.score}, {it.completedChallenges}, {it.completedQuestTasks},
                {it.completedAchievements}))
            .reversed()
    }
}
