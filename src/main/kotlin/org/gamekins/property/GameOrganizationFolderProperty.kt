/*
 * Copyright 2022 Gamekins contributors
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

import com.cloudbees.hudson.plugins.folder.AbstractFolder
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty
import com.cloudbees.hudson.plugins.folder.AbstractFolderPropertyDescriptor
import hudson.Extension
import hudson.model.Item
import hudson.model.JobPropertyDescriptor
import hudson.util.ListBoxModel
import jenkins.branch.MultiBranchProject
import jenkins.branch.OrganizationFolder
import net.sf.json.JSONObject
import org.gamekins.util.Constants
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.StaplerProxy
import org.kohsuke.stapler.StaplerRequest
import java.io.IOException
import javax.annotation.Nonnull

/**
 * Adds the configuration for Gamekins to the configuration page of a [OrganizationFolder]. Since the
 * [OrganizationFolder] contains multiple [WorkflowMultiBranchProject]s, there is no functionality in the class.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class GameOrganizationFolderProperty private constructor()
    : AbstractFolderProperty<AbstractFolder<*>?>(), StaplerProxy {

    override fun getTarget(): Any {
        this.owner?.checkPermission(Item.READ)
        return this
    }

    /**
     * Registers the [GameOrganizationFolderProperty] to Jenkins as an extension and also works as an communication
     * point between the Jetty server and the [GameOrganizationFolderProperty]. Only to add the
     * [GameMultiBranchProperty] to the subprojects.
     *
     * Cannot be outsourced in separate class, because the constructor of [AbstractFolderPropertyDescriptor]
     * does not take the base class like the [JobPropertyDescriptor].
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    @Extension
    class GameOrganizationFolderPropertyDescriptor : AbstractFolderPropertyDescriptor() {

        /**
         * Called from the Jetty server when the configuration page is displayed. Fills the combo box of subprojects
         * of the [job].
         */
        fun doFillProjectItems(@AncestorInPath job: OrganizationFolder?): ListBoxModel {
            if (job == null) return ListBoxModel()
            val listBoxModel = ListBoxModel()
            job.items.stream().map { obj: MultiBranchProject<*, *> -> obj.name }
                    .forEach { nameAndValue: String? -> listBoxModel.add(nameAndValue) }
            return listBoxModel
        }

        @Nonnull
        override fun getDisplayName(): String {
            return "Activate Gamekins"
        }

        /**
         * The [GameOrganizationFolderProperty] can only be added to jobs with the [containerType]
         * [OrganizationFolder]. For other [containerType]s have a look at [GameJobProperty] and
         * [GameMultiBranchProperty].
         *
         * @see AbstractFolderPropertyDescriptor.isApplicable
         */
        override fun isApplicable(containerType: Class<out AbstractFolder<*>?>): Boolean {
            return containerType == OrganizationFolder::class.java
        }

        /**
         * Returns a new instance of a [GameMultiBranchProperty] after saving the configuration page of the job.
         * Adds the [GameMultiBranchProperty] to the specified subproject.
         *
         * @see AbstractFolderPropertyDescriptor.newInstance
         */
        override fun newInstance(req: StaplerRequest?, formData: JSONObject): AbstractFolderProperty<*>? {
            if (req == null) return null
            val folder = req.findAncestor(OrganizationFolder::class.java).getObject() as OrganizationFolder
            for (project in folder.items) {
                if (project.fullName == formData.getString(Constants.FormKeys.PROJECT_NAME)) {
                    try {
                        val property = project.properties.get(GameMultiBranchProperty::class.java)
                        property?.reconfigure(req, formData)
                                ?: project.addProperty(GameMultiBranchProperty(project,
                                    formData.getBoolean(Constants.FormKeys.ACTIVATED),
                                    formData.getBoolean(Constants.FormKeys.SHOW_LEADERBOARD),
                                    formData.getBoolean(Constants.FormKeys.SHOW_STATISTICS),
                                    formData.getInt(Constants.FormKeys.CHALLENGES_COUNT),
                                    formData.getInt(Constants.FormKeys.QUEST_COUNT),
                                    formData.getInt(Constants.FormKeys.STORED_CHALLENGES_COUNT),
                                    formData.getBoolean(Constants.FormKeys.CAN_SEND_CHALLENGE)))
                        folder.save()
                        break
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            return null
        }
    }
}
