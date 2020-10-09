package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import java.util.*

interface Challenge {

    fun getCreated(): Long

    fun getScore(): Int

    fun getSolved(): Long

    fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener, workspace: FilePath): Boolean

    fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener, workspace: FilePath): Boolean

    fun printToXML(reason: String, indentation: String): String?

    override fun toString(): String

    //TODO: Override equals()
}
