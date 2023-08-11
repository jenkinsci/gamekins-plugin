package org.gamekins.questtask

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.util.Constants
import org.gamekins.util.JacocoUtil

class CoverMoreLinesQuestTask(linesNumber: Int, csvFile: FilePath): QuestTask(linesNumber) {

    private val startNumberOfLines = JacocoUtil.getCoveredLines(csvFile)

    override fun getScore(): Int {
        return numberGoal / 5
    }

    override fun isSolved(
        parameters: Constants.Parameters,
        run: Run<*, *>,
        listener: TaskListener,
        user: User
    ): Boolean {
        val currentCoveredLines = JacocoUtil.getCoveredLines(
            FilePath(parameters.workspace.channel, parameters.workspace.remote + "/" + parameters.jacocoCSVPath))
        currentNumber = currentCoveredLines - startNumberOfLines
        if (currentNumber >= numberGoal) {
            solved = System.currentTimeMillis()
            return true
        }

        return false
    }

    override fun printToXML(indentation: String): String {
        return "$indentation<${this::class.simpleName} created=\"$created\" solved=\"$solved\" " +
                "currentNumber=\"$currentNumber\" numberGoal=\"$numberGoal\" " +
                "startNumberOfLines=\"$startNumberOfLines\">"
    }

    override fun toString(): String {
        return "Cover $numberGoal more lines in your project by tests"
    }
}