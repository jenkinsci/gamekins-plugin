package org.gamekins.questtask

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.util.Constants
import org.gamekins.util.JUnitUtil

class AddMoreTestsQuestTask(testsNumber: Int, workspace: FilePath): QuestTask(testsNumber) {

    private val startNumberOfTests = JUnitUtil.getTestCount(workspace)

    override fun getScore(): Int {
        return numberGoal
    }

    override fun isSolved(
        parameters: Constants.Parameters,
        run: Run<*, *>,
        listener: TaskListener,
        user: User
    ): Boolean {
        val currentTests = JUnitUtil.getTestCount(parameters.workspace)
        currentNumber = currentTests - startNumberOfTests
        if (currentNumber >= numberGoal) {
            solved = System.currentTimeMillis()
            return true
        }

        return false
    }

    override fun printToXML(indentation: String): String {
        return "$indentation<${this::class.simpleName} created=\"$created\" solved=\"$solved\" " +
                "currentNumber=\"$currentNumber\" numberGoal=\"$numberGoal\" " +
                "startNumberOfTests=\"$startNumberOfTests\">"
    }

    override fun toString(): String {
        return if (numberGoal == 1) "Add one test to your project" else "Add $numberGoal tests to your project"
    }
}