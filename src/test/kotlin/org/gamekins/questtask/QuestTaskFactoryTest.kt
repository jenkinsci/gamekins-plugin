package org.gamekins.questtask

import hudson.model.TaskListener
import hudson.model.User
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.gamekins.GameUserProperty
import org.gamekins.util.Constants
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

class QuestTaskFactoryTest : FeatureSpec({

    beforeContainer {
        mockkStatic(User::class)
        mockkObject(Random)
    }

    afterSpec {
        unmockkAll()
    }

    feature("generateNewQuestTasks") {
        val user = mockkClass(User::class)
        val property = mockkClass(GameUserProperty::class)
        val parameters = Constants.Parameters(projectName = "test")
        val listener = TaskListener.NULL
        val questTask = mockkClass(AddMoreTestsQuestTask::class)
        val questTasks = CopyOnWriteArrayList<QuestTask>()
        questTasks.add(questTask)
        every { property.getCurrentQuestTasks(any()) } returns questTasks
        scenario("Max QuestTasks reached") {
            QuestTaskFactory.generateNewQuestTasks(user, property, parameters, listener) shouldBe 0
        }

        every { property.isParticipating(any()) } returns true
        every { property.newQuestTask(any(), any()) } returns Unit
        every { user.fullName } returns "Dummy"
        every { user.getProperty(GameUserProperty::class.java) } returns property
        every { User.getAll() } returns listOf(user)
        every { Random.nextInt(any()) } returns 11
        //Somehow, QuestTaskFactory.chooseQuestTaskType() is not determinstic in building the weightList
        xscenario("Generate one QuestTask") {
            QuestTaskFactory.generateNewQuestTasks(user, property, parameters, listener, 2) shouldBe 1
        }
    }
})