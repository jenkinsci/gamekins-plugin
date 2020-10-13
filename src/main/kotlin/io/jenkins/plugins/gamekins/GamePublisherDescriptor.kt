package io.jenkins.plugins.gamekins

import hudson.Extension
import hudson.model.AbstractProject
import hudson.tasks.BuildStepDescriptor
import hudson.tasks.Publisher
import hudson.util.FormValidation
import io.jenkins.plugins.gamekins.property.GameMultiBranchProperty
import io.jenkins.plugins.gamekins.property.GameOrganizationFolderProperty
import io.jenkins.plugins.gamekins.util.PublisherUtil
import org.jenkinsci.Symbol
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.QueryParameter
import javax.annotation.Nonnull

/**
 * Registers the [GamePublisher] to Jenkins as an extension and also works as an communication
 * point between the Jetty server and the [GamePublisher]. Also serves as a Pipeline identifier.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
@Extension
@Symbol("gamekins")
class GamePublisherDescriptor : BuildStepDescriptor<Publisher?>(GamePublisher::class.java) {

    init {
        load()
    }

    /**
     * Checks whether the path of the JaCoCo csv file [jacocoCSVPath] exists in the [project].
     */
    fun doCheckJacocoCSVPath(@AncestorInPath project: AbstractProject<*, *>?,
                             @QueryParameter jacocoCSVPath: String?): FormValidation {
        if (project == null) {
            return FormValidation.ok()
        }
        return if (PublisherUtil.doCheckJacocoCSVPath(project.getSomeWorkspace()!!, jacocoCSVPath!!))
            FormValidation.ok()
        else FormValidation.error("The file could not be found")
    }

    /**
     * Checks whether the path of the JaCoCo index.html file [jacocoResultsPath] exists in the [project].
     */
    fun doCheckJacocoResultsPath(@AncestorInPath project: AbstractProject<*, *>?,
                                 @QueryParameter jacocoResultsPath: String?): FormValidation {
        if (project == null) {
            return FormValidation.ok()
        }
        return if (PublisherUtil.doCheckJacocoResultsPath(project.getSomeWorkspace()!!, jacocoResultsPath!!))
            FormValidation.ok()
        else FormValidation.error("The folder is not correct")
    }

    @Nonnull
    override fun getDisplayName(): String {
        return "Publisher for Gamekins plugin"
    }

    /**
     * The configuration is only shown in jobs of the type [AbstractProject], since it is otherwise called via a
     * Pipeline script.
     *
     * @see BuildStepDescriptor.isApplicable
     */
    override fun isApplicable(jobType: Class<out AbstractProject<*, *>?>): Boolean {
        return AbstractProject::class.java.isAssignableFrom(jobType)
    }
}
