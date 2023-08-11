package org.gamekins.questtask

import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.GameUserProperty
import org.gamekins.achievement.Achievement
import org.gamekins.util.Constants

class SolveAchievementQuestTask(user: User, project: String): QuestTask(1) {

    private val achievements = arrayListOf<Achievement>()
    private val startNumberOfAchievements = user.getProperty(GameUserProperty::class.java)
        ?.getCompletedAchievements(project)?.size ?: 0

    override fun getScore(): Int {
        return 1
    }

    override fun isSolved(
        parameters: Constants.Parameters,
        run: Run<*, *>,
        listener: TaskListener,
        user: User
    ): Boolean {
        val property = user.getProperty(GameUserProperty::class.java)
        if (property != null) {
            val currentAchievements = property.getCompletedAchievements(parameters.projectName)
            if (currentAchievements.size > startNumberOfAchievements) {
                solved = System.currentTimeMillis()
                achievements.addAll(currentAchievements.takeLast(currentAchievements.size - startNumberOfAchievements))
                return true
            }
        }

        return false
    }

    override fun printToXML(indentation: String): String {
        return "$indentation<${this::class.simpleName} created=\"$created\" solved=\"$solved\" " +
                "startNumberOfAchievements=\"$startNumberOfAchievements\">"
    }

    override fun toString(): String {
        return "Solve one additional achievement"
    }
}