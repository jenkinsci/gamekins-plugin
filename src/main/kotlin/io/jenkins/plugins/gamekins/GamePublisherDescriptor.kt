package io.jenkins.plugins.gamekins

import hudson.Extension
import hudson.model.AbstractProject
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.Publisher
import hudson.util.FormValidation
import io.jenkins.plugins.gamekins.util.PublisherUtil
import org.jenkinsci.Symbol
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.QueryParameter
import javax.annotation.Nonnull

@Extension
@Symbol("gamekins")
class GamePublisherDescriptor : BuildStepDescriptor<Publisher?>(GamePublisher::class.java) {
    @Nonnull
    override fun getDisplayName(): String {
        return "Publisher for Gamekins plugin."
    }

    /**
     * Returns true if this task is applicable to the given project.
     *
     * @param jobType the type of job
     * @return true to allow user to configure this post-promotion task for the given project.
     * @see AbstractProject.AbstractProjectDescriptor.isApplicable
     */
    override fun isApplicable(jobType: Class<out AbstractProject<*, *>?>): Boolean {
        return AbstractProject::class.java.isAssignableFrom(jobType)
    }

    fun doCheckJacocoResultsPath(@AncestorInPath project: AbstractProject<*, *>?,
                                 @QueryParameter jacocoResultsPath: String?): FormValidation {
        if (project == null) {
            return FormValidation.ok()
        }
        return if (PublisherUtil.doCheckJacocoResultsPath(project.someWorkspace!!, jacocoResultsPath!!))
            FormValidation.ok()
        else FormValidation.error("The folder is not correct")
    }

    fun doCheckJacocoCSVPath(@AncestorInPath project: AbstractProject<*, *>?,
                             @QueryParameter jacocoCSVPath: String?): FormValidation {
        if (project == null) {
            return FormValidation.ok()
        }
        return if (PublisherUtil.doCheckJacocoCSVPath(project.someWorkspace!!, jacocoCSVPath!!)) FormValidation.ok()
               else FormValidation.error("The file could not be found")
    }

    init {
        load()
    }
}
