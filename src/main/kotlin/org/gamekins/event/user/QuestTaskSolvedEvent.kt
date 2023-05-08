package org.gamekins.event.user

import hudson.model.User
import org.gamekins.questtask.QuestTask

/**
 * Created when a participant solves a QuestTask.
 *
 * @author Philipp Straubinger
 * @since 0.6
 */
class QuestTaskSolvedEvent(projectName: String, branch: String, user: User, val questTask: QuestTask)
    : UserEvent(projectName, branch, user) {

    override fun run() = Unit
}