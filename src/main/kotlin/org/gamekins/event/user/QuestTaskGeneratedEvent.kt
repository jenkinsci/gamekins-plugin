package org.gamekins.event.user

import hudson.model.User
import org.gamekins.questtask.QuestTask

/**
 * Created when a new QuestTask is generated for a participant.
 *
 * @author Philipp Straubinger
 * @since 0.6
 */
class QuestTaskGeneratedEvent(projectName: String, branch: String, user: User, val questTask: QuestTask)
    : UserEvent(projectName, branch, user) {

    override fun run() = Unit
}