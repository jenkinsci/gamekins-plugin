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

package org.gamekins.achievement

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Object to initialize an [Achievement] from its json representation.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
object AchievementInitializer {

    /**
     * Initializes the [Achievement] specified by the json file given via [fileName]. [fileName] has the path to the
     * resources folder as root folder, which means that it has to start with / followed by the folders and ending by
     * the json filename.
     */
    fun initializeAchievement(fileName: String): Achievement {
        val jsonContent = javaClass.getResource(fileName).readText()
        val data = jacksonObjectMapper().readValue(jsonContent, AchievementData::class.java)
        val achievement = Achievement(data.badgePath, data.fullyQualifiedFunctionName,
            data.description, data.title, data.secret, data.additionalParameters)
        return achievement
    }

    /**
     * Data class for mapping the json to an [Achievement]
     *
     * @author Philipp Straubinger
     * @since 1.0
     */
    data class AchievementData(val badgePath: String,
                               val description: String,
                               val title: String,
                               val fullyQualifiedFunctionName: String,
                               val secret: Boolean,
                               val additionalParameters: HashMap<String, String>)
}