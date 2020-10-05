package io.jenkins.plugins.gamekins.property

import com.cloudbees.hudson.plugins.folder.AbstractFolder
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor
import hudson.Extension
import hudson.util.ListBoxModel
import jenkins.branch.MultiBranchProject
import jenkins.branch.OrganizationFolder
import net.sf.json.JSONObject
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.StaplerRequest
import java.io.IOException
import javax.annotation.Nonnull

class GameOrganizationFolderProperty private constructor() : AbstractFolderProperty<AbstractFolder<*>?>() {

    @Extension
    class DescriptorFolderImpl : AbstractFolderPropertyDescriptor() {
        @Nonnull
        override fun getDisplayName(): String {
            return "Set the activation of the Gamekins plugin."
        }

        override fun isApplicable(containerType: Class<out AbstractFolder<*>?>): Boolean {
            return containerType == OrganizationFolder::class.java
        }

        override fun newInstance(req: StaplerRequest?, formData: JSONObject): AbstractFolderProperty<*>? {
            if (req == null) return null
            val folder = req.findAncestor(OrganizationFolder::class.java).getObject() as OrganizationFolder
            for (project in folder.items) {
                if (project.name == formData.getString("project")) {
                    try {
                        val property = project.properties.get(GameMultiBranchProperty::class.java)
                        property?.reconfigure(req, formData)
                                ?: project.addProperty(GameMultiBranchProperty(project,
                                        formData.getBoolean("activated"), formData.getBoolean("showStatistics")))
                        folder.save()
                        break
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            return null
        }

        fun doFillProjectItems(@AncestorInPath job: OrganizationFolder?): ListBoxModel {
            if (job == null) return ListBoxModel()
            val listBoxModel = ListBoxModel()
            job.items.stream().map { obj: MultiBranchProject<*, *> -> obj.name }
                    .forEach { nameAndValue: String? -> listBoxModel.add(nameAndValue) }
            return listBoxModel
        }

        init {
            load()
        }
    }
}
