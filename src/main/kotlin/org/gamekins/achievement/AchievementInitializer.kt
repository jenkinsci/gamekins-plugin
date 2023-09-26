/*
 * Copyright 2023 Gamekins contributors
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
 * @since 0.2
 */
object AchievementInitializer {

    /**
     * Initializes the [Achievement] specified by the json file given via [fileName]. [fileName] has the path to the
     * resources' folder as root folder, which means that it has to start with / followed by the folders and ending by
     * the json filename.
     *
     * Only for more than one [Achievement] per file.
     */
    @JvmStatic
    fun initializeAchievements(fileName: String): List<Achievement> {
        var json = javaClass.classLoader.getResource(fileName)
        if (json == null && fileName.startsWith("/")) {
            json = javaClass.classLoader.getResource(fileName.removePrefix("/"))
        }
        val jsonContent = json.readText()
        return initializeAchievementsWithContent(jsonContent)
    }

    @JvmStatic
    fun initializeProgressAchievements(fileName: String): List<ProgressAchievement> {
        var json = javaClass.classLoader.getResource(fileName)
        if (json == null && fileName.startsWith("/")) {
            json = javaClass.classLoader.getResource(fileName.removePrefix("/"))
        }
        if (json == null) {
            throw Exception("Stuff is still null")
        }
        val jsonContent = json.readText()
        return initializeProgressAchievementsWithContent(jsonContent)
    }

    /**
     * Initializes the [Achievement] specified by the json content given via [fileContent]. [fileContent] must be a
     * valid json String.
     *
     * Only for more than one [Achievement] per file.
     */
    @JvmStatic
    fun initializeAchievementsWithContent(fileContent: String): List<Achievement> {
        val data: List<AchievementData> = jacksonObjectMapper().readValue(
            fileContent,
            jacksonObjectMapper().typeFactory.constructCollectionType(List::class.java, AchievementData::class.java)
        )

        return data.map {
            Achievement(
                it.badgePath, it.unsolvedBadgePath, it.fullyQualifiedFunctionName,
                it.description, it.title, it.secret, it.additionalParameters
            )
        }
    }

    @JvmStatic
    fun initializeProgressAchievementsWithContent(fileContent: String): List<ProgressAchievement> {
        val data: List<ProgressAchievementData> = jacksonObjectMapper().readValue(
            fileContent,
            jacksonObjectMapper().typeFactory.constructCollectionType(List::class.java, ProgressAchievementData::class.java)
        )

        return data.map {
            ProgressAchievement(
                it.badgePath, it.milestones, it.fullyQualifiedFunctionName,
                it.description, it.title,0
            )
        }
    }

    /**
     * Data class for mapping the json to an [Achievement]
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    data class AchievementData(val badgePath: String,
                               val unsolvedBadgePath: String,
                               val description: String,
                               val title: String,
                               val fullyQualifiedFunctionName: String,
                               val secret: Boolean,
                               val additionalParameters: HashMap<String, String>)

    data class ProgressAchievementData(val badgePath: String,
                                       val milestones: List<Int>,
                                       val description: String,
                                       val title: String,
                                       val fullyQualifiedFunctionName: String)
}