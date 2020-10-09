package io.jenkins.plugins.gamekins.property

import hudson.model.*
import io.jenkins.plugins.gamekins.LeaderboardAction
import io.jenkins.plugins.gamekins.StatisticsAction
import io.jenkins.plugins.gamekins.statistics.Statistics
import io.jenkins.plugins.gamekins.util.PropertyUtil.reconfigure
import net.sf.json.JSONObject
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.StaplerRequest
import java.io.IOException
import java.util.*
import javax.annotation.Nonnull

class GameJobProperty
@DataBoundConstructor constructor(job: AbstractItem, @set:DataBoundSetter var activated: Boolean,
                                  @set:DataBoundSetter var showStatistics: Boolean)
    : JobProperty<Job<*, *>>(), GameProperty {

    private var statistics: Statistics
    private val teams: ArrayList<String> = ArrayList()

    @Throws(IOException::class)
    override fun addTeam(teamName: String) {
        teams.add(teamName)
        owner.save()
    }

    /**
     * [Action]s to be displayed in the job page.
     *
     *
     *
     * Returning actions from this method allows a job property to add them
     * to the left navigation bar in the job page.
     *
     *
     *
     * [Action] can implement additional marker interface to integrate
     * with the UI in different ways.
     *
     * @param job Always the same as [.owner] but passed in anyway for backward compatibility (I guess.)
     * You really need not use this value at all.
     * @return can be empty but never null.
     * @see ProminentProjectAction
     *
     * @see PermalinkProjectAction
     *
     * @since 1.341
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

    override fun reconfigure(req: StaplerRequest, form: JSONObject?): JobProperty<*> {
        if (form != null) activated = form.getBoolean("activated")
        if (form != null) showStatistics = form.getBoolean("showStatistics")
        reconfigure(owner, activated, showStatistics)
        return this
    }

    @Throws(IOException::class)
    override fun removeTeam(teamName: String) {
        teams.remove(teamName)
        owner.save()
    }

    init {
        statistics = Statistics(job)
    }
}
