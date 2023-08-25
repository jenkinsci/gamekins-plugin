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
import org.gamekins.util.JacocoUtil

class CoverMoreLinesQuestTaskTest : FeatureSpec({

    val workspace = mockkClass(FilePath::class)
    lateinit var questTask: CoverMoreLinesQuestTask

    beforeContainer {
        mockkStatic(JacocoUtil::class)
        every { JacocoUtil.getCoveredLines(any()) } returns 0
        questTask = CoverMoreLinesQuestTask(10, workspace)
    }

    afterSpec {
        unmockkAll()
    }

    feature("getScore") {
        questTask.getScore() shouldBe 2
    }

    feature("isSolved") {
        val parameters = Parameters()
        val run = mockkClass(Run::class)
        val listener = TaskListener.NULL
        val user = mockkClass(User::class)
        scenario("Test goal not reached") {
            questTask.isSolved(parameters, run, listener, user) shouldBe false
        }

        every { JacocoUtil.getCoveredLines(any()) } returns 10
        scenario("Test goal reached") {
            questTask.isSolved(parameters, run, listener, user) shouldBe true
        }

        every { JacocoUtil.getCoveredLines(any()) } returns 11
        scenario("Test goal overreached") {
            questTask.isSolved(parameters, run, listener, user) shouldBe true
        }
    }

    feature("printToXML") {
        scenario("Without indentation") {
            questTask.printToXML("") shouldBe "<CoverMoreLinesQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" currentNumber=\"${questTask.currentNumber}\" numberGoal=\"10\" startNumberOfLines=\"0\">"
        }

        scenario("With indentation") {
            questTask.printToXML("    ") shouldBe "    <CoverMoreLinesQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" currentNumber=\"${questTask.currentNumber}\" numberGoal=\"10\" startNumberOfLines=\"0\">"
        }
    }

    feature("toString") {
        questTask.toString() shouldBe "Cover ${questTask.numberGoal} more lines in your project by tests"
    }
})