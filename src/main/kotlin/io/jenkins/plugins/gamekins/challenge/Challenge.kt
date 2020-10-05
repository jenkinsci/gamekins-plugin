package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import java.util.*

interface Challenge {

    fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener, workspace: FilePath): Boolean

    fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener, workspace: FilePath): Boolean

    fun getScore(): Int

    fun getCreated(): Long

    fun getSolved(): Long

    fun printToXML(reason: String, indentation: String): String?

    override fun toString(): String

    //TODO: Override equals()
}
