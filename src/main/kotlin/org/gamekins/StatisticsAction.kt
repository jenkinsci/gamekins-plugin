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

import hudson.model.AbstractItem
import hudson.model.AbstractProject
import hudson.model.Item
import hudson.model.ProminentProjectAction
import jenkins.model.Jenkins
import org.gamekins.property.GameJobProperty
import org.gamekins.property.GameMultiBranchProperty
import org.gamekins.property.GameProperty
import org.gamekins.statistics.Statistics
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.accmod.Restricted
import org.kohsuke.accmod.restrictions.NoExternalUse
import org.kohsuke.stapler.StaplerProxy

/**
 * Action to display the [Statistics] XML representation on the left side panel of a job for evaluation purposes.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class StatisticsAction(val job: AbstractItem) : ProminentProjectAction, StaplerProxy {

    override fun getDisplayName(): String {
        return "Statistics"
    }

    override fun getIconFileName(): String {
        return "graph.png"
    }

    /**
     * Returns the XML representation of the project.
     */
    fun getStatistics(): String {
        val property: GameProperty = when (job) {
            is WorkflowMultiBranchProject -> {
                job.properties.get(GameMultiBranchProperty::class.java)
            }
            is WorkflowJob -> {
                job.getProperty(GameJobProperty::class.java)
            }
            else -> {
                (job as AbstractProject<*, *>).getProperty(GameJobProperty::class.java)
            }
        }
        return property.getStatistics().printToXML()
    }

    @Restricted(NoExternalUse::class)
    override fun getTarget(): Any {
        Jenkins.getInstanceOrNull()?.checkPermission(Item.CONFIGURE)
        return this
    }

    override fun getUrlName(): String {
        return "statistics"
    }
}
