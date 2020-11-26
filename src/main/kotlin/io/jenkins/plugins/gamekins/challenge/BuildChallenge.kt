package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import kotlin.collections.HashMap

/**
 * Specific [Challenge] to motivate the user to fix a failing build in Jenkins.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class BuildChallenge(private var constants: HashMap<String, String>) : Challenge {

    private val created = System.currentTimeMillis()
    private var solved: Long = 0

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is BuildChallenge) return false
        return true
    }

    override fun getCreated(): Long {
        return created
    }

    override fun getScore(): Int {
        return 1
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun getConstants(): HashMap<String, String> {
        return constants
    }

    override fun hashCode(): Int {
        var result = constants.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + solved.hashCode()
        return result
    }

    /**
     * A [BuildChallenge] is always solvable since it depends on the status of the Jenkins [run].
     */
    override fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                            workspace: FilePath): Boolean {
        return true
    }

    /**
     * A [BuildChallenge] is only solved if the status of the current [run] is [Result.SUCCESS].
     */
    override fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                          workspace: FilePath): Boolean {
        if (run.result == Result.SUCCESS) {
            solved = System.currentTimeMillis()
            return true
        }
        return false
    }

    override fun printToXML(reason: String, indentation: String): String {
        var print = "$indentation<BuildChallenge created=\"$created\" solved=\"$solved"
        if (reason.isNotEmpty()) {
            print += "\" reason=\"$reason"
        }
        print += "\"/>"
        return print
    }

    /**
     * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
     */
    @Suppress("unused", "SENSELESS_COMPARISON")
    private fun readResolve(): Any {
        if (constants == null) constants = hashMapOf()
        return this
    }

    override fun toString(): String {
        return "Let the Build run successfully"
    }
}
