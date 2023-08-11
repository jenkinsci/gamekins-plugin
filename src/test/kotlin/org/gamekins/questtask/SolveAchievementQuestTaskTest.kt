package org.gamekins.questtask

import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.unmockkAll
import org.gamekins.GameUserProperty
import org.gamekins.achievement.Achievement
import org.gamekins.util.Constants.Parameters
import java.util.concurrent.CopyOnWriteArrayList

class SolveAchievementQuestTaskTest : FeatureSpec({

    val user = mockkClass(User::class)
    val achievement = mockkClass(Achievement::class)
    val property = mockkClass(GameUserProperty::class)
    val project = "test"
    lateinit var questTask : SolveAchievementQuestTask

    beforeContainer {
        every { property.getCompletedAchievements(project) } returns CopyOnWriteArrayList()
        every { user.getProperty(GameUserProperty::class.java) } returns property
        questTask = SolveAchievementQuestTask(user, project)
    }

    afterSpec {
        unmockkAll()
    }

    feature("getScore") {
        questTask.getScore() shouldBe 1
    }

    feature("isSolved") {
        val parameters = Parameters()
        val run = mockkClass(Run::class)
        val listener = TaskListener.NULL
        every { user.getProperty(GameUserProperty::class.java) } returns null
        scenario("Property is null") {
            questTask.isSolved(parameters, run, listener, user) shouldBe false
        }

        every { property.getCompletedAchievements(any()) } returns CopyOnWriteArrayList()
        every { user.getProperty(GameUserProperty::class.java) } returns property
        scenario("Goal not reached") {
            questTask.isSolved(parameters, run, listener, user) shouldBe false
        }

        val achievements = CopyOnWriteArrayList<Achievement>()
        achievements.add(achievement)
        every { property.getCompletedAchievements(any()) } returns achievements
        every { user.getProperty(GameUserProperty::class.java) } returns property
        scenario("Goal reached") {
            questTask.isSolved(parameters, run, listener, user) shouldBe true
        }
    }

    feature("printToXML") {
        scenario("Without indentation") {
            questTask.printToXML("") shouldBe "<SolveAchievementQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" startNumberOfAchievements=\"0\">"
        }

        scenario("With indentation") {
            questTask.printToXML("    ") shouldBe "    <SolveAchievementQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" startNumberOfAchievements=\"0\">"
        }
    }

    feature("toString") {
        questTask.toString() shouldBe "Solve one additional achievement"
    }
})