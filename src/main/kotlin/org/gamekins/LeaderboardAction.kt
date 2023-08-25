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
import org.kohsuke.stapler.StaplerProxy

/**
 * Represents the Leaderboard displayed in the side panel of the job. Used to send the data to the Jetty server.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class LeaderboardAction(val job: AbstractItem) : ProminentProjectAction, StaplerProxy {

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

    /**
     * Returns the details of all teams of the current project.
     */
    fun getTeamDetails(): List<ActionUtil.TeamDetails> {
        return ActionUtil.getTeamDetails(job)
    }

    override fun getUrlName(): String {
        return "leaderboard"
    }

    /**
     * Returns the details of all users participating in the current project.
     */
    fun getUserDetails(): List<ActionUtil.UserDetails> {
        return ActionUtil.getUserDetails(job)
    }

    fun getHasTeams(): Boolean {
        return getTeamDetails().isNotEmpty()
    }
}
