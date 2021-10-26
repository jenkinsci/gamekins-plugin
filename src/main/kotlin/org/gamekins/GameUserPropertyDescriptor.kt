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

package org.gamekins

import com.fasterxml.jackson.databind.ObjectMapper
import hudson.Extension
import hudson.model.*
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.QueryParameter
import java.io.File
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.annotation.Nonnull

/**
 * Registers the [GameUserProperty] to Jenkins as an extension and also works as an communication point between the
 * Jetty server and the [GameUserProperty].
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
@Extension
class GameUserPropertyDescriptor : UserPropertyDescriptor(GameUserProperty::class.java) {

    /**
     * Returns the list of avatars available in Gamekins. [job] is only needed so that Jenkins does recognize this
     * method to be available from the frontend.
     */
    fun doGetAvatars(@AncestorInPath job: Job<*, *>?): String {
        val resource = javaClass.getResource("/avatars/avatars.json")
        val mapper = ObjectMapper()

        val list = mapper.readValue(resource?.readText(), Array<String>::class.java)

        return list.asList().toString()
    }

    /**
     * Returns the relative path to the current avatar of a [user].
     */
    fun doGetCurrentAvatar(@AncestorInPath user: User?): String {
        val property = user?.getProperty(GameUserProperty::class.java)
        return "/plugin/gamekins/avatars/${property?.getCurrentAvatar()}"
    }

    /**
     * Sets a new avatar based on the chosen one from the [user] with [name].
     */
    fun doSetCurrentAvatar(@AncestorInPath user: User?, @QueryParameter name: String) {
        val property = user?.getProperty(GameUserProperty::class.java)
        property?.setCurrentAvatar(name)
    }

    @Nonnull
    override fun getDisplayName(): String {
        return "Gamekins"
    }

    override fun newInstance(user: User): UserProperty {
        return GameUserProperty()
    }
}
