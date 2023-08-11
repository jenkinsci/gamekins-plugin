package org.gamekins.questtask

import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.GameUserProperty
import org.gamekins.challenge.Challenge
import org.gamekins.util.Constants

abstract class QuestTask(val numberGoal: Int) {

    protected val created = System.currentTimeMillis()
    var currentNumber: Int = 0
    protected var solved: Long = 0

    fun getCompletedPercentage(): Int {
        val percentage = currentNumber.toDouble() / numberGoal.toDouble() * 100
        return if (percentage > 100) 100 else percentage.toInt()
    }

    abstract fun getScore(): Int

    fun getSolvedChallengesOfUserSince(
        user: User,
        project: String,
        since: Long,
        type: Class<out Challenge> = Challenge::class.java
    ): ArrayList<Challenge> {
        val property = user.getProperty(GameUserProperty::class.java)
        if (property != null) {
            val challenges = property.getCompletedChallenges(project)
            challenges.removeIf { it.getSolved() < since }
            return ArrayList(challenges.filterIsInstance(type))
        }

        return arrayListOf()
    }

    fun getSolvedChallengesOfUserSinceLastRejection(user: User, project: String, since: Long
    ): ArrayList<Challenge> {
        val property = user.getProperty(GameUserProperty::class.java)
        if (property != null) {
            val challenges = property.getCompletedChallenges(project)
            challenges.removeIf { it.getSolved() < since }
            val rejectedChallenges = property.getRejectedChallenges(project)
            if (rejectedChallenges.isNotEmpty()) {
                val lastRejectedChallenge = rejectedChallenges.last().first
                challenges.removeIf { it.getSolved() < lastRejectedChallenge.getSolved() }
            }
            return ArrayList(challenges)
        }

        return arrayListOf()
    }

    abstract fun isSolved(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener, user: User): Boolean

    abstract fun printToXML(indentation: String): String
}