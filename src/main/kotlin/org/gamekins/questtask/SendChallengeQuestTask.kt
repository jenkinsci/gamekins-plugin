package org.gamekins.questtask

import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.challenge.Challenge
import org.gamekins.util.Constants

class SendChallengeQuestTask(): QuestTask(1) {

    val challenges: ArrayList<Challenge> = arrayListOf()

    fun challengeSent(challenge: Challenge) {
        challenges.add(challenge)
        currentNumber++
    }
    
    override fun getScore(): Int {
        return numberGoal
    }

    override fun isSolved(
        parameters: Constants.Parameters,
        run: Run<*, *>,
        listener: TaskListener,
        user: User
    ): Boolean {
        if (currentNumber >= numberGoal) {
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
        return "Send one challenge to your colleagues"
    }
}