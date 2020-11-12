package io.jenkins.plugins.gamekins.property

import hudson.model.AbstractItem
import hudson.model.AbstractProject
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import io.jenkins.plugins.gamekins.statistics.Statistics
import java.io.IOException
import java.util.*
import kotlin.jvm.Throws

/**
 * Interface for all Properties of Gamekins.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
interface GameProperty {

    /**
     * Method to add a new team with the given [teamName] to the property.
     */
    @Throws(IOException::class)
    fun addTeam(teamName: String)

    /**
     * Returns the owner/parent of the property. Could be of type [AbstractProject], [WorkflowJob]
     * or [WorkflowMultiBranchProject].
     */
    fun getOwner(): AbstractItem

    /**
     * Returns the [Statistics] of the property and therefore the job.
     */
    fun getStatistics(): Statistics

    /**
     * Returns the list of team names of the property/job.
     */
    fun getTeams(): ArrayList<String>

    /**
     * Method to remove a team with the given [teamName] from the property.
     */
    @Throws(IOException::class)
    fun removeTeam(teamName: String)

    /**
     * Removes the current Statistics and initializes a new one
     */
    fun resetStatistics(job: AbstractItem)
}
