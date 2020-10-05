package io.jenkins.plugins.gamekins.property

import com.cloudbees.hudson.plugins.folder.AbstractFolder
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor
import hudson.Extension
import hudson.model.AbstractItem
import hudson.util.FormValidation
import hudson.util.ListBoxModel
import io.jenkins.plugins.gamekins.statistics.Statistics
import io.jenkins.plugins.gamekins.util.PropertyUtil
import io.jenkins.plugins.gamekins.util.PropertyUtil.doAddTeam
import io.jenkins.plugins.gamekins.util.PropertyUtil.doDeleteTeam
import io.jenkins.plugins.gamekins.util.PropertyUtil.doFillTeamsBoxItems
import io.jenkins.plugins.gamekins.util.PropertyUtil.doFillUsersBoxItems
import io.jenkins.plugins.gamekins.util.PropertyUtil.reconfigure
import net.sf.json.JSONObject
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.*
import java.io.IOException
import java.util.*
import javax.annotation.Nonnull

class GameMultiBranchProperty
@DataBoundConstructor constructor(job: AbstractItem?, @set:DataBoundSetter var activated: Boolean,
                                  @set:DataBoundSetter var showStatistics: Boolean)
    : AbstractFolderProperty<AbstractFolder<*>?>(), GameProperty {

    private val teams: ArrayList<String> = ArrayList()
    private var statistics: Statistics

    override fun getTeams(): ArrayList<String> {
        return teams
    }

    override fun getStatistics(): Statistics {
        if (statistics.isNotFullyInitialized) {
            statistics = Statistics(owner!!)
        }
        return statistics
    }

    @Throws(IOException::class)
    override fun addTeam(teamName: String) {
        teams.add(teamName)
        owner!!.save()
    }

    @Throws(IOException::class)
    override fun removeTeam(teamName: String) {
        teams.remove(teamName)
        owner!!.save()
    }

    override fun reconfigure(req: StaplerRequest, form: JSONObject?): AbstractFolderProperty<*> {
        if (form != null) activated = form.getBoolean("activated")
        if (form != null) showStatistics = form.getBoolean("showStatistics")
        reconfigure(owner!!, activated, showStatistics)
        return this
    }

    @Extension
    class DescriptorImpl : AbstractFolderPropertyDescriptor() {
        @Nonnull
        override fun getDisplayName(): String {
            return "Set the activation of the Gamekins plugin."
        }

        override fun isApplicable(containerType: Class<out AbstractFolder<*>?>): Boolean {
            return containerType == WorkflowMultiBranchProject::class.java
        }

        override fun newInstance(req: StaplerRequest?, formData: JSONObject): AbstractFolderProperty<*>? {
            return if (req == null || req.findAncestor(AbstractItem::class.java).getObject() == null) null
            else GameMultiBranchProperty(req.findAncestor(AbstractItem::class.java).getObject() as AbstractItem,
                    formData.getBoolean("activated"), formData.getBoolean("showStatistics"))
        }

        fun doAddTeam(@AncestorInPath job: WorkflowMultiBranchProject?,
                      @QueryParameter teamName: String): FormValidation {
            if (job == null) return FormValidation.error("Unexpected error: Parent job is null")
            if (teamName.isEmpty()) return FormValidation.error("Insert a name for the team")
            val property = job.properties[this] as GameMultiBranchProperty
            val validation = doAddTeam(property, teamName)
            save()
            return validation
        }

        fun doFillTeamsBoxItems(@AncestorInPath job: WorkflowMultiBranchProject?): ListBoxModel {
            val property =
                    if (job == null) null
                    else job.properties[this] as GameMultiBranchProperty
            return doFillTeamsBoxItems(property)
        }

        fun doFillUsersBoxItems(@AncestorInPath job: WorkflowMultiBranchProject): ListBoxModel {
            return doFillUsersBoxItems(job.name)
        }

        fun doAddUserToTeam(@AncestorInPath job: WorkflowMultiBranchProject?,
                            @QueryParameter teamsBox: String?, @QueryParameter usersBox: String?): FormValidation {
            return PropertyUtil.doAddUserToTeam(job, teamsBox!!, usersBox!!)
        }

        fun doRemoveUserFromTeam(@AncestorInPath job: WorkflowMultiBranchProject?, @QueryParameter teamsBox: String?,
                                 @QueryParameter usersBox: String?): FormValidation {
            return PropertyUtil.doRemoveUserFromTeam(job, teamsBox!!, usersBox!!)
        }

        fun doDeleteTeam(@AncestorInPath job: WorkflowMultiBranchProject?,
                         @QueryParameter teamsBox: String?): FormValidation {
            if (job == null) return FormValidation.error("Unexpected error: Parent job is null")
            val projectName = job.name
            val property = job.properties[this] as GameMultiBranchProperty
            val validation = doDeleteTeam(projectName, property, teamsBox!!)
            save()
            return validation
        }

        init {
            load()
        }
    }

    init {
        statistics = Statistics(job!!)
        reconfigure(job, activated, showStatistics)
    }
}
