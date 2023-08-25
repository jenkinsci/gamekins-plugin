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

package org.gamekins.property

import hudson.model.AbstractItem
import hudson.model.AbstractProject
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.gamekins.statistics.Statistics
import java.io.IOException
import kotlin.jvm.Throws

/**
 * Interface for all Properties of Gamekins.
 *
 * @author Philipp Straubinger
 * @since 0.1
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
