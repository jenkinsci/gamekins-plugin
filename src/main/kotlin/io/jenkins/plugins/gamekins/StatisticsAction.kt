package io.jenkins.plugins.gamekins

import hudson.model.AbstractItem
import hudson.model.AbstractProject
import hudson.model.ProminentProjectAction
import io.jenkins.plugins.gamekins.property.GameJobProperty
import io.jenkins.plugins.gamekins.property.GameMultiBranchProperty
import io.jenkins.plugins.gamekins.property.GameProperty
import io.jenkins.plugins.gamekins.statistics.Statistics
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

/**
 * Action to display the [Statistics] XML representation on the left side panel of a job for evaluation purposes.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class StatisticsAction(val job: AbstractItem) : ProminentProjectAction {

    override fun getDisplayName(): String {
        return "Statistics"
    }

    override fun getIconFileName(): String {
        return "document.png"
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

    override fun getUrlName(): String {
        return "statistics"
    }
}
