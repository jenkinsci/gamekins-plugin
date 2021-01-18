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
            data.description, data.title)
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
                               val fullyQualifiedFunctionName: String)
}