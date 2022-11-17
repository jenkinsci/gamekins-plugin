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

package org.gamekins.util

import hudson.FilePath
import java.io.File
import java.io.Serializable
import jenkins.model.Jenkins
import java.nio.file.Path

/**
 * Object with constants for Gamekins and [Parameters] for generation.
 *
 * @author Philipp Straubinger
 * @since 0.4
 */
object Constants {

    /**
     * Object with key constants for configuration forms
     *
     * @author Matthias Rainer
     */
    object FormKeys
    {
        const val PROJECT_NAME = "project"

        const val ACTIVATED = "activated"

        const val SHOW_STATISTICS = "showStatistics"

        const val SHOW_LEADERBOARD = "showLeaderboard"

        const val SHOW_FOLDER_LEADERBOARD = "leaderboard"

        const val CHALLENGES_COUNT = "currentChallengesCount"

        const val QUEST_COUNT = "currentQuestsCount"

        const val STORED_CHALLENGES_COUNT = "currentStoredChallengesCount"

        const val  CAN_SEND_CHALLENGE = "canSendChallenge"

        const val SEARCH_COMMIT_COUNT = "searchCommitCount"
    }

    /**
     * Object with default values
     *
     * @author Matthias Rainer
     */
    object Default
    {
        const val CURRENT_CHALLENGES = 3

        const val CURRENT_QUESTS = 1

        const val STORED_CHALLENGES = 2

        const val SEARCH_COMMIT_COUNT = 50
    }

    /**
     * Object with Error messages
     *
     * @author Matthias Rainer
     */
    object Error
    {
        const val UNEXPECTED = "Unexpected Error"

        const val GENERATION = "There was an error with generating a new challenge"

        const val NO_CHALLENGE_EXISTS = "The challenge does not exist"

        const val NO_USER_SIGNED_IN = "There is no user signed in"

        const val PARENT = "$UNEXPECTED: Parent job is null"

        const val RETRIEVING_PROPERTY = "Unexpected error while retrieving the property"

        const val SAVING = "There was an error with saving"

        const val NO_REASON = "Please insert your reason for rejection"

        const val REJECT_DUMMY = "Dummies cannot be rejected - please run another build"

        const val STORE_DUMMY = "Dummies cannot be stored - please run another build"

        const val NO_TEAM_NAME = "Insert a name for the team"

        const val STORAGE_LIMIT = "Storage Limit reached"

        const val RECEIVER_IS_SELF = "Cannot send challenges to yourself"

        const val USER_NOT_FOUND = "User not found"

        const val UNKNOWN_GAME_PROPERTY = "Unknown Game Property"

        const val NO_TEAM = "No team specified"
        
        const val UNKNOWN_TEAM = "The specified team does not exist"
        
        const val USER_NOT_IN_TEAM = "The user is not in the specified team"

        const val UNKNOWN_USER = "No user with the specified name found"

        const val PARENT_WITHOUT_PROPERTY = "$UNEXPECTED: Parent job has no property"

        const val USER_ALREADY_IN_TEAM = "The user is already participating in a team"

        const val TEAM_NAME_TAKEN = "The team already exists - please use another name for your team"
    }

    const val EXISTS = " exists "

    const val NO_QUEST = "No quest could be generated. This could mean that none of the prerequisites was met, " +
            "please try again later."

    const val NOT_ACTIVATED = "[Gamekins] Not activated"

    const val NOT_SOLVED = "Not solved"

    const val NOTHING_DEVELOPED = "You haven't developed anything lately"

    const val REJECTED_QUEST = "Previous quest was rejected, please run a new build to generate a new quest"

    const val RUN_TOTAL_COUNT = 200

    val SONAR_JAVA_PLUGIN = pathToSonarJavaPlugin()

    const val TYPE_JSON = "application/json"

    const val TYPE_PLAIN = "text/plain"

    /**
     * Migrates a [HashMap] of constants to the class [Parameters].
     */
    fun constantsToParameters(constants: HashMap<String, String>): Parameters {
        val parameters = Parameters()
        if (constants["branch"] != null) parameters.branch = constants["branch"]!!
        if (constants["currentChallengesCount"] != null) parameters.currentChallengesCount =
            constants["currentChallengesCount"]!!.toInt()
        if (constants["generated"] != null) parameters.generated = constants["generated"]!!.toInt()
        if (constants["jacocoCSVPath"] != null) parameters.jacocoCSVPath = constants["jacocoCSVPath"]!!
        if (constants["jacocoResultsPath"] != null) parameters.jacocoResultsPath = constants["jacocoResultsPath"]!!
        if (constants["mocoJSONPath"] != null) parameters.mocoJSONPath = constants["mocoJSONPath"]!!
        if (constants["projectCoverage"] != null) parameters.projectCoverage = constants["projectCoverage"]!!.toDouble()
        if (constants["projectName"] != null) parameters.projectName = constants["projectName"]!!
        if (constants["projectTests"] != null) parameters.projectTests = constants["projectTests"]!!.toInt()
        if (constants["solved"] != null) parameters.solved = constants["solved"]!!.toInt()
        if (constants["workspace"] != null) parameters.workspace = FilePath(null, constants["workspace"]!!)
        return parameters
    }

    /**
     * Returns the path to the most recent jar file of the Sonar-Java-Plugin for SonarLint.
     */
    private fun pathToSonarJavaPlugin(): Path {
        val projectPath = System.getProperty("user.dir")
        var libFolder = File("$projectPath/target/lib")
        if (!libFolder.exists()) libFolder =
            File("${Jenkins.getInstanceOrNull()?.root?.absolutePath}/plugins/gamekins/WEB-INF/lib")
        val jars = libFolder.listFiles()!!.filter { it.nameWithoutExtension.contains("sonar-java-plugin") }
        return jars.last().toPath()
    }

    /**
     * The class representation of parameters during challenge generation.
     *
     * @author Philipp Straubinger
     * @since 0.4
     */
    class Parameters(
        var branch: String = "",
        var currentChallengesCount: Int = Default.CURRENT_CHALLENGES,
        var currentQuestsCount: Int = Default.CURRENT_QUESTS,
        var storedChallengesCount: Int = Default.STORED_CHALLENGES,
        var searchCommitCount: Int = Default.SEARCH_COMMIT_COUNT,
        var generated: Int = 0,
        var jacocoCSVPath: String = "",
        var jacocoResultsPath: String = "",
        var mocoJSONPath: String = "",
        var projectCoverage: Double = 0.0,
        var projectName: String = "",
        var projectTests: Int = 0,
        var solved: Int = 0,
        workspace: FilePath = FilePath(null, "")
    ) : Serializable {

        var remote: String = workspace.remote

        @Transient var workspace: FilePath = workspace
            set(value) {
                remote = value.remote
                field = value
            }

        /**
         * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
         */
        @Suppress("unused", "SENSELESS_COMPARISON")
        private fun readResolve(): Any {
            if (remote == null) remote = ""
            if (workspace == null) workspace = FilePath(null, remote)
            if (currentQuestsCount == null) currentQuestsCount = Default.CURRENT_QUESTS
            return this
        }
    }
}
