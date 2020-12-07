package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.gamekins.statistics.Statistics
import io.jenkins.plugins.gamekins.LeaderboardAction
import kotlin.collections.HashMap

/**
 * Interface for all Challenges of Gamekins.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
interface Challenge {

    override fun equals(other: Any?): Boolean

    /**
     * Returns the constants provided during creation. Must include entries for "projectName", "branch", "workspace",
     * "jacocoResultsPath" and "jacocoCSVPath".
     */
    fun getConstants(): HashMap<String, String>

    /**
     * Returns the creation time in milliseconds since 01.01.1970.
     */
    fun getCreated(): Long

    /**
     * Returns the score for the specific [Challenge].
     */
    fun getScore(): Int

    /**
     * Returns the time when the [Challenge] has been solved in milliseconds since 01.01.1970.
     */
    fun getSolved(): Long

    /**
     * Returns the tooltip of the Challenge. Currently only available for the [LineCoverageChallenge].
     */
    fun getToolTipText(): String {
        return ""
    }

    /**
     * Checks whether the current [Challenge] is still solvable or not.
     */
    fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener, workspace: FilePath)
            : Boolean

    /**
     * Checks whether the current [Challenge] is solved.
     */
    fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener, workspace: FilePath)
            : Boolean

    /**
     * Returns whether a tooltip should be shown for the Challenge. Currently only available for the
     * [LineCoverageChallenge].
     */
    fun isToolTip(): Boolean {
        return false
    }

    /**
     * Returns the XML representation of the current [Challenge] with the [indentation] in front of the line
     * for the [Statistics]. Adds an additional [reason] in case that the [Challenge] has been rejected.
     */
    fun printToXML(reason: String, indentation: String): String?

    /**
     * Returns the String representation of the [Challenge] for the [LeaderboardAction].
     */
    override fun toString(): String
}
