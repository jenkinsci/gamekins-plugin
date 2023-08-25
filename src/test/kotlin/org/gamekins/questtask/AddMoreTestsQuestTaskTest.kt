package org.gamekins.questtask

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.JUnitUtil

class AddMoreTestsQuestTaskTest: FeatureSpec({

    val workspace = mockkClass(FilePath::class)
    val questTask = AddMoreTestsQuestTask(10, workspace)

    beforeContainer {
        mockkStatic(JUnitUtil::class)
        every { JUnitUtil.getTestCount(any()) } returns 0
    }

    afterSpec {
        unmockkAll()
    }

    feature("getScore") {
        questTask.getScore() shouldBe 10
    }

    feature("isSolved") {
        val parameters = Parameters()
        val run = mockkClass(Run::class)
        val listener = TaskListener.NULL
        val user = mockkClass(User::class)
        scenario("Test goal not reached") {
            questTask.isSolved(parameters, run, listener, user) shouldBe false
        }

        every { JUnitUtil.getTestCount(any()) } returns 10
        scenario("Test goal reached") {
            questTask.isSolved(parameters, run, listener, user) shouldBe true
        }

        every { JUnitUtil.getTestCount(any()) } returns 11
        scenario("Test goal overreached") {
            questTask.isSolved(parameters, run, listener, user) shouldBe true
        }
    }

    feature("printToXML") {
        scenario("Without indentation") {
            questTask.printToXML("") shouldBe "<AddMoreTestsQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" currentNumber=\"${questTask.currentNumber}\" numberGoal=\"10\" startNumberOfTests=\"0\">"
        }

        scenario("With indentation") {
            questTask.printToXML("    ") shouldBe "    <AddMoreTestsQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" currentNumber=\"${questTask.currentNumber}\" numberGoal=\"10\" startNumberOfTests=\"0\">"
        }
    }

    feature("toString") {
        scenario("Multiple tests") {
            questTask.toString() shouldBe "Add 10 tests to your project"
        }

        val task2 = AddMoreTestsQuestTask(1, workspace)
        scenario("One test") {
            task2.toString() shouldBe "Add one test to your project"
        }
    }
})