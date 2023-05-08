package org.gamekins.questtask

import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.challenge.Challenge
import org.gamekins.util.Constants

class SolveChallengesWithoutRejectionQuestTask(challengeNumber: Int): QuestTask(challengeNumber) {

    val challenges: ArrayList<Challenge> = arrayListOf()

    override fun getScore(): Int {
        return numberGoal
    }

    override fun isSolved(
        parameters: Constants.Parameters,
        run: Run<*, *>,
        listener: TaskListener,
        user: User
    ): Boolean {
        val solvedChallenges = getSolvedChallengesOfUserSinceLastRejection(user, parameters.projectName, created)
        currentNumber = solvedChallenges.size
        if (solvedChallenges.size >= numberGoal) {
            challenges.addAll(solvedChallenges)
            solved = System.currentTimeMillis()
            return true
        }

        return false
    }

    override fun printToXML(indentation: String): String {
        return "$indentation < ${this::class.simpleName} created=\"$created\" solved=\"$solved\" " +
                "currentNumber=\"$currentNumber\" numberGoal=\"$numberGoal\">"
    }

    override fun toString(): String {
        return "Solve $numberGoal challenge(s) without rejecting one"
    }
}