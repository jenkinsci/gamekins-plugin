package org.gamekins.event.user

import hudson.model.User
import org.gamekins.questtask.QuestTask

/**
 * Created when a participant makes progress in a QuestTask.
 *
 * @author Philipp Straubinger
 * @since 0.6
 */
class QuestTaskProgressEvent(projectName: String, branch: String, user: User, val questTask: QuestTask,
                             val currentNumber: Int, val numberGoal: Int)
    : UserEvent(projectName, branch, user) {

    override fun run() = Unit
}