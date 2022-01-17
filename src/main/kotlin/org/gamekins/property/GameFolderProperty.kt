/*
 * Copyright 2021 Gamekins contributors
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
import com.cloudbees.hudson.plugins.folder.Folder
import hudson.Extension
import hudson.model.Action
import hudson.model.Actionable
import hudson.model.Item
import hudson.model.JobPropertyDescriptor
import net.sf.json.JSONObject
import org.gamekins.FolderLeaderboardAction
import org.kohsuke.stapler.*
import javax.annotation.Nonnull

/**
 * Adds the configuration for Gamekins to the configuration page of a [Folder].
 *
 * @author Philipp Straubinger
 * @since 0.4
 */
class GameFolderProperty
@DataBoundConstructor constructor(@set:DataBoundSetter var leaderboard: Boolean)
    : AbstractFolderProperty<AbstractFolder<*>?>(), StaplerProxy {

    override fun getTarget(): Any {
        this.owner?.checkPermission(Item.READ)
        return this
    }

    /**
     * Registers the [GameFolderProperty] to Jenkins as an extension and also works as an communication
     * point between the Jetty server and the [GameFolderProperty]. Only to add the
     * [FolderLeaderboardAction] to the folder.
     *
     * Cannot be outsourced in separate class, because the constructor of [AbstractFolderPropertyDescriptor]
     * does not take the base class like the [JobPropertyDescriptor].
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    @Extension
    class GameFolderPropertyDescriptor : AbstractFolderPropertyDescriptor() {

        @Nonnull
        override fun getDisplayName(): String {
            return "Activate Gamekins"
        }

        /**
         * The [GameFolderProperty] can only be added to jobs with the [containerType]
         * [AbstractFolder]. For other [containerType]s have a look at [GameJobProperty],
         * [GameMultiBranchProperty] and [GameOrganizationFolderProperty].
         *
         * @see AbstractFolderPropertyDescriptor.isApplicable
         */
        override fun isApplicable(containerType: Class<out AbstractFolder<*>?>): Boolean {
            return containerType == Folder::class.java
        }

        override fun newInstance(req: StaplerRequest?, formData: JSONObject): AbstractFolderProperty<*>? {
            if (req == null)  return null
            val leaderboard = formData.getBoolean("leaderboard")

            val folder = req.findAncestor(Folder::class.java).`object` as Folder
            try {
                val actionField = Actionable::class.java.getDeclaredField("actions")
                actionField.isAccessible = true
                if (actionField[folder] == null) actionField[folder] = actionField.type.newInstance()
                if (leaderboard) {
                    (actionField[folder] as MutableList<*>).removeIf { action -> action is FolderLeaderboardAction }
                    (actionField[folder] as MutableList<Action?>).add(FolderLeaderboardAction(folder))
                } else {
                    (actionField[folder] as MutableList<*>).removeIf { action -> action is FolderLeaderboardAction }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return GameFolderProperty(leaderboard)
        }
    }
}
