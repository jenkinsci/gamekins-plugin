package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import java.util.*
import io.jenkins.plugins.gamekins.statistics.Statistics
import io.jenkins.plugins.gamekins.LeaderboardAction

/**
 * Interface for all Challenges of Gamekins.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
interface Challenge {

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

    //TODO: Override equals()
}
