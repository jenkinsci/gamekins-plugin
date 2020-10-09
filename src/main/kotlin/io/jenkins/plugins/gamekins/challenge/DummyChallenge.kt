package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import java.util.*

//TODO: Add text why the DummyChallenge has been created
class DummyChallenge : Challenge {

    override fun getCreated(): Long {
        return 0
    }

    override fun getScore(): Int {
        return 0
    }

    override fun getSolved(): Long {
        return 0
    }

    override fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                            workspace: FilePath): Boolean {
        return true
    }

    override fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                          workspace: FilePath): Boolean {
        return true
    }

    override fun printToXML(reason: String, indentation: String): String {
        return "$indentation<DummyChallenge>"
    }

    override fun toString(): String {
        return "You have nothing developed recently"
    }
}
