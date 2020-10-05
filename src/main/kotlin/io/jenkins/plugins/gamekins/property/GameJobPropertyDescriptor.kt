package io.jenkins.plugins.gamekins.property

import hudson.Extension
import hudson.maven.AbstractMavenProject
import hudson.model.*
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import io.jenkins.plugins.gamekins.util.PropertyUtil
import io.jenkins.plugins.gamekins.util.PropertyUtil.doAddTeam
import io.jenkins.plugins.gamekins.util.PropertyUtil.doDeleteTeam
import io.jenkins.plugins.gamekins.util.PropertyUtil.doFillTeamsBoxItems
import io.jenkins.plugins.gamekins.util.PropertyUtil.doFillUsersBoxItems
import net.sf.json.JSONObject
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.QueryParameter
import org.kohsuke.stapler.StaplerRequest
import javax.annotation.Nonnull

@Extension
class GameJobPropertyDescriptor : JobPropertyDescriptor(GameJobProperty::class.java) {

    @Nonnull
    override fun getDisplayName(): String {
        return "Set the activation of the Gamekins plugin."
    }

    override fun isApplicable(jobType: Class<out Job<*, *>?>): Boolean {
        return jobType == FreeStyleProject::class.java || jobType == WorkflowJob::class.java
                || AbstractMavenProject::class.java.isAssignableFrom(jobType)
    }

    override fun newInstance(req: StaplerRequest?, formData: JSONObject): JobProperty<*>? {
        return if (req == null || req.findAncestor(AbstractItem::class.java).getObject() == null) null
        else GameJobProperty(req.findAncestor(AbstractItem::class.java).getObject() as AbstractItem,
                formData.getBoolean("activated"), formData.getBoolean("showStatistics"))
    }

    fun doAddTeam(@AncestorInPath job: Job<*, *>?, @QueryParameter teamName: String): FormValidation {
        if (teamName.isEmpty()) return FormValidation.error("Insert a name for the team")
        val property = if (job == null) null else job.properties[this] as GameJobProperty?
        val validation = doAddTeam(property, teamName)
        save()
        return validation
    }

    fun doFillTeamsBoxItems(@AncestorInPath job: Job<*, *>?): ListBoxModel {
        val property = if (job == null) null else job.properties[this] as GameJobProperty?
        return doFillTeamsBoxItems(property)
    }

    fun doFillUsersBoxItems(@AncestorInPath job: Job<*, *>?): ListBoxModel {
        return if (job == null) ListBoxModel() else doFillUsersBoxItems(job.name)
    }

    fun doAddUserToTeam(@AncestorInPath job: Job<*, *>?, @QueryParameter teamsBox: String?,
                        @QueryParameter usersBox: String?): FormValidation {
        return PropertyUtil.doAddUserToTeam(job, teamsBox!!, usersBox!!)
    }

    fun doRemoveUserFromTeam(@AncestorInPath job: Job<*, *>?, @QueryParameter teamsBox: String?,
                             @QueryParameter usersBox: String?): FormValidation {
        return PropertyUtil.doRemoveUserFromTeam(job, teamsBox!!, usersBox!!)
    }

    fun doDeleteTeam(@AncestorInPath job: Job<*, *>?, @QueryParameter teamsBox: String?): FormValidation {
        if (job == null) return FormValidation.error("Unexpected error: Parent job is null")
        val projectName = job.name
        val property = job.properties[this] as GameJobProperty
        val validation = doDeleteTeam(projectName, property, teamsBox!!)
        save()
        return validation
    }

    init {
        load()
    }
}
