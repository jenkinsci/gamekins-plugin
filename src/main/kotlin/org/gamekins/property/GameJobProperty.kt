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

import hudson.model.*
import org.gamekins.LeaderboardAction
import org.gamekins.util.PropertyUtil
import org.gamekins.StatisticsAction
import org.gamekins.statistics.Statistics
import net.sf.json.JSONObject
import org.gamekins.util.Constants
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.StaplerProxy
import org.kohsuke.stapler.StaplerRequest
import java.io.IOException
import javax.annotation.Nonnull
import kotlin.jvm.Throws

/**
 * Adds the configuration for Gamekins to the configuration page of a [FreeStyleProject].
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class GameJobProperty
@DataBoundConstructor constructor(job: AbstractItem,
                                  @set:DataBoundSetter var activated: Boolean,
                                  @set:DataBoundSetter var showLeaderboard: Boolean,
                                  @set:DataBoundSetter var showStatistics: Boolean,
                                  @set:DataBoundSetter var currentChallengesCount: Int,
                                  @set:DataBoundSetter var currentQuestsCount: Int,
                                  @set:DataBoundSetter var currentStoredChallengesCount: Int)
    : JobProperty<Job<*, *>>(), GameProperty, StaplerProxy {

    private var statistics: Statistics
    private val teams: ArrayList<String> = ArrayList()

    init {
        statistics = Statistics(job)
        if (currentChallengesCount <= 0) currentChallengesCount = Constants.DEFAULT_CURRENT_CHALLENGES
        if (currentQuestsCount <= 0) currentQuestsCount = Constants.DEFAULT_CURRENT_QUESTS
        if (currentStoredChallengesCount < 0) currentStoredChallengesCount = Constants.DEFAULT_STORED_CHALLENGES
    }

    @Throws(IOException::class)
    override fun addTeam(teamName: String) {
        teams.add(teamName)
        owner.save()
    }

    /**
     * Adds the [LeaderboardAction] and the [StatisticsAction] to the left panel if the corresponding checkboxes
     * in the configuration are activated. Does only return a new action if there is no one already in the list of
     * actions of the job. Only works for the addition of actions in a [FreeStyleProject], everything else has to be
     * done with the help of [PropertyUtil.reconfigure].
     *
     * @see [JobProperty.getJobActions]
     */
    @Nonnull
    override fun getJobActions(job: Job<*, *>): Collection<Action> {
        val newActions: MutableList<Action> = ArrayList()
        if (showLeaderboard && (job.getAction(LeaderboardAction::class.java) == null || job is FreeStyleProject)) {
            newActions.add(LeaderboardAction(job))
        }
        if (showStatistics && (job.getAction(StatisticsAction::class.java) == null || job is FreeStyleProject)) {
            newActions.add(StatisticsAction(job))
        }
        return newActions
    }

    override fun getOwner(): AbstractItem {
        return owner
    }

    override fun getStatistics(): Statistics {
        if (statistics.isNotFullyInitialized()) {
            statistics = Statistics(owner)
        }
        return statistics
    }

    override fun getTarget(): Any {
        this.owner.checkPermission(Item.CONFIGURE)
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
     * @see [JobProperty.reconfigure]
     */
    override fun reconfigure(req: StaplerRequest, form: JSONObject?): JobProperty<*> {
        if (form != null) {
            activated = form.getBoolean("activated")
            showStatistics = form.getBoolean("showStatistics")
            showLeaderboard = form.getBoolean("showLeaderboard")
            if (form.getValue("currentChallengesCount") is String)
                currentChallengesCount = form.getInt("currentChallengesCount")
            if (form.getValue("currentQuestsCount") is String)
                currentQuestsCount = form.getInt("currentQuestsCount")
            if (form.getValue("currentStoredChallengesCount") is String)
                currentStoredChallengesCount = form.getInt("currentStoredChallengesCount")
        }

        PropertyUtil.reconfigure(owner, showLeaderboard, showStatistics)
        return this
    }

    @Throws(IOException::class)
    override fun removeTeam(teamName: String) {
        teams.remove(teamName)
        owner.save()
    }

    override fun resetStatistics(job: AbstractItem) {
        statistics = Statistics(job)
        owner.save()
    }
}
