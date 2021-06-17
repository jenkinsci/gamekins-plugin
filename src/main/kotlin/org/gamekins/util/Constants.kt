package org.gamekins.util

import hudson.FilePath
import java.io.Serializable

object Constants {

    const val DEFAULT_CURRENT_CHALLENGES = 3

    const val DEFAULT_SEARCH_COMMIT_COUNT = 50

    const val UNEXPECTED_ERROR = "Unexpected Error"

    const val ERROR_PARENT = "$UNEXPECTED_ERROR: Parent job is null"

    const val ERROR_SAVING = "There was an error with saving"

    const val EXISTS = " exists "

    const val NO_TEAM = "No team specified"

    const val NOT_ACTIVATED = "[Gamekins] Not activated"

    const val NOT_SOLVED = "Not solved"

    const val RUN_TOTAL_COUNT = 200

    const val TYPE_JSON = "application/json"

    const val TYPE_PLAIN = "text/plain"

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

    class Parameters(
        var branch: String = "",
        var currentChallengesCount: Int = DEFAULT_CURRENT_CHALLENGES,
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
            return this
        }
    }
}