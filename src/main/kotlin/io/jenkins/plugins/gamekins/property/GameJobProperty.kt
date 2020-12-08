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

package io.jenkins.plugins.gamekins.property

import hudson.model.*
import io.jenkins.plugins.gamekins.LeaderboardAction
import io.jenkins.plugins.gamekins.util.PropertyUtil
import io.jenkins.plugins.gamekins.StatisticsAction
import io.jenkins.plugins.gamekins.statistics.Statistics
import net.sf.json.JSONObject
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.StaplerRequest
import java.io.IOException
import java.util.*
import javax.annotation.Nonnull
import kotlin.jvm.Throws

/**
 * Adds the configuration for Gamekins to the configuration page of a [FreeStyleProject].
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class GameJobProperty
@DataBoundConstructor constructor(job: AbstractItem, @set:DataBoundSetter var activated: Boolean,
                                  @set:DataBoundSetter var showStatistics: Boolean)
    : JobProperty<Job<*, *>>(), GameProperty {

    private var statistics: Statistics
    private val teams: ArrayList<String> = ArrayList()

    init {
        statistics = Statistics(job)
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
        if (activated && (job.getAction(LeaderboardAction::class.java) == null || job is FreeStyleProject)) {
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

    override fun getTeams(): ArrayList<String> {
        return teams
    }

    /**
     * Sets the new values of [activated] and [showStatistics], if the job configuration has been saved.
     * Also calls [PropertyUtil.reconfigure] to update the [LeaderboardAction] and [StatisticsAction].
     *
     * @see [JobProperty.reconfigure]
     */
    override fun reconfigure(req: StaplerRequest, form: JSONObject?): JobProperty<*> {
        if (form != null) activated = form.getBoolean("activated")
        if (form != null) showStatistics = form.getBoolean("showStatistics")
        PropertyUtil.reconfigure(owner, activated, showStatistics)
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
