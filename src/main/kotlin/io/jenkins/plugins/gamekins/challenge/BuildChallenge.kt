package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import java.util.*

class BuildChallenge : Challenge {

    private val created = System.currentTimeMillis()
    private var solved: Long = 0

    override fun getCreated(): Long {
        return created
    }

    override fun getScore(): Int {
        return 1
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                            workspace: FilePath): Boolean {
        return true
    }

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

    override fun toString(): String {
        return "Let the Build run successfully"
    }
}
