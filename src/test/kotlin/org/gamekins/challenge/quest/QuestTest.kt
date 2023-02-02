package org.gamekins.challenge.quest

import hudson.model.TaskListener
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.unmockkAll
import org.gamekins.challenge.LineCoverageChallenge
import org.gamekins.util.Constants

class QuestTest : FeatureSpec({

    lateinit var noStepQuest: Quest
    val challenge1 = mockkClass(LineCoverageChallenge::class)
    val challenge2 = mockkClass(LineCoverageChallenge::class)
    val step1 = QuestStep("", challenge1)
    val step2 = QuestStep("", challenge2)
    lateinit var twoStepQuest: Quest
    val run = mockkClass(hudson.model.Run::class)

    beforeContainer {
        noStepQuest = Quest("Lines over lines", arrayListOf())
        twoStepQuest = Quest("Lines over Lines", arrayListOf(step1, step2))
        every { challenge1 == any() } returns true
        every { challenge1.isSolved(any(), any(), any()) } returns true
        every { challenge1.update(any()) } returns Unit
        every { challenge1.getScore() } returns 2
        every { challenge1.isSolvable(any(), any(), any()) } returns true
        every { challenge1.printToXML(any(), any()) } returns "            <Challenge>"
        every { challenge2 == any() } returns true
        every { challenge2.isSolved(any(), any(), any()) } returns true
        every { challenge2.update(any()) } returns Unit
        every { challenge2.getScore() } returns 3
        every { challenge2.isSolvable(any(), any(), any()) } returns true
        every { challenge2.printToXML(any(), any()) } returns "            <Challenge>"
    }

    afterSpec {
        unmockkAll()
    }

    feature("equals") {
        scenario("Null")
        {
            noStepQuest.equals(null) shouldBe false
        }

        scenario("Not Quest")
        {
            noStepQuest.equals(Constants.Parameters()) shouldBe false
        }

        scenario("Self")
        {
            (noStepQuest == noStepQuest) shouldBe true
        }

        val challenge = mockkClass(LineCoverageChallenge::class)
        every { challenge == any() } returns true
        val quest1 = Quest("Lines over Lines", arrayListOf(QuestStep("", challenge)))
        scenario("Different Quest")
        {
            (noStepQuest == quest1) shouldBe false
        }

        val quest2 = Quest("Lines over Lines", arrayListOf(QuestStep("", challenge)))
        scenario("Same Steps in Quest")
        {
            (quest1 == quest2) shouldBe true
        }

        scenario("Quest has Subset of Steps")
        {
            (quest1 == noStepQuest) shouldBe false
        }
    }

    feature("getCurrentStep") {
        scenario("Quest on Initial Step")
        {
            twoStepQuest.getCurrentStep() shouldBe step1
        }

        every { challenge2.isSolved(any(), any(), any()) } returns false
        scenario("Challenge (first step) is solved, second is not")
        {
            twoStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
            twoStepQuest.getCurrentStep() shouldBe step2
        }

        every { challenge2.isSolved(any(), any(), any()) } returns true
        scenario("Both steps solved, Quest stays on last step")
        {
            twoStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
            twoStepQuest.getCurrentStep() shouldBe step2
        }
    }

    feature("getCurrentStepNumber") {
        scenario("Quest on Initial Step")
        {
            twoStepQuest.getCurrentStepNumber() shouldBe 0
        }

        every { challenge2.isSolved(any(), any(), any()) } returns false
        scenario("Challenge (first step) is solved, second is not")
        {
            twoStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
            twoStepQuest.getCurrentStepNumber() shouldBe 1
        }

        every { challenge2.isSolved(any(), any(), any()) } returns true
        scenario("Both steps solved, StepNumber moved past last step")
        {
            twoStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
            twoStepQuest.getCurrentStepNumber() shouldBe 2
        }
    }

    feature("getLastSolvedStep") {
        scenario("No step solved yet")
        {
            twoStepQuest.getLastSolvedStep() shouldBe step1
        }

        every { challenge2.isSolved(any(), any(), any()) } returns false
        scenario("First Step solved")
        {
            twoStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
            twoStepQuest.getLastSolvedStep() shouldBe step1
        }

        every { challenge2.isSolved(any(), any(), any()) } returns true
        scenario("All Steps solved")
        {
            twoStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
            twoStepQuest.getLastSolvedStep() shouldBe step2
        }
    }

    feature("getScore") {
        scenario("Quest without steps")
        {
            noStepQuest.getScore() shouldBe 0
        }

        scenario("Quest with steps")
        {
            twoStepQuest.getScore() shouldBe 7
        }
    }

    feature("isCurrentStepSolved") {
        scenario("No Steps exist")
        {
            noStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe false
        }

        every { challenge2.isSolved(any(), any(), any()) } returns false
        scenario("Current (First) Step solved")
        {
            twoStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        }

        scenario("Current (Second) Step not solved")
        {
            twoStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe false
        }

        every { challenge2.isSolved(any(), any(), any()) } returns true
        scenario("Current (Second) Step solved")
        {
            twoStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        }
    }

    feature("isSolvable") {
        scenario("Quest has no Steps to be solved")
        {
            noStepQuest.isSolvable(Constants.Parameters(), run, TaskListener.NULL) shouldBe false
        }

        scenario("All Steps can be solved")
        {
            twoStepQuest.isSolvable(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        }

        every { challenge1.isSolvable(any(), any(), any()) } returns false
        scenario("A Step cannot be solved")
        {
            twoStepQuest.isSolvable(Constants.Parameters(), run, TaskListener.NULL) shouldBe false
        }
    }

    feature("isSolved") {
        scenario("Quest has no Steps")
        {
            noStepQuest.isSolved() shouldBe false
        }

        every { challenge2.isSolved(any(), any(), any()) } returns false
        scenario("A Step is not solved")
        {
            twoStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
            twoStepQuest.isSolved() shouldBe false
        }

        every { challenge2.isSolved(any(), any(), any()) } returns true
        scenario("All steps are solved")
        {
            twoStepQuest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
            twoStepQuest.isSolved() shouldBe true
            twoStepQuest.solved shouldNotBe 0
        }

        val quest2 = Quest("Lines over Lines", arrayListOf(step1, step2))
        scenario("Different Quest with same steps is solved too")
        {
            quest2.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
            quest2.getCurrentStepNumber() shouldBe 2
            quest2.isSolved() shouldBe true
            quest2.solved shouldNotBe 0
        }
    }

    feature("printToXML") {
        val result1 = "<Quest name=\"Lines over Lines\" created=\"${twoStepQuest.created}\" solved=\"0\">\n" +
                "    <QuestSteps count=\"2\">\n" +
                "        <QuestStep description=\"\">\n" +
                "            <Challenge>\n" +
                "        </QuestStep>\n" +
                "        <QuestStep description=\"\">\n" +
                "            <Challenge>\n" +
                "        </QuestStep>\n" +
                "    </QuestSteps>\n" +
                "</Quest>"
        scenario("No Reason, no Indentation")
        {
            twoStepQuest.printToXML("", "") shouldBe result1
        }

        val result2 = "<Quest name=\"Lines over Lines\" created=\"${twoStepQuest.created}\" solved=\"0\" reason=\"reason\">\n" +
                "    <QuestSteps count=\"2\">\n" +
                "        <QuestStep description=\"\">\n" +
                "            <Challenge>\n" +
                "        </QuestStep>\n" +
                "        <QuestStep description=\"\">\n" +
                "            <Challenge>\n" +
                "        </QuestStep>\n" +
                "    </QuestSteps>\n" +
                "</Quest>"
        scenario("With Reason, no Indentation")
        {
            twoStepQuest.printToXML("reason", "") shouldBe result2
        }
    }

    feature("toString") {
        twoStepQuest.toString() shouldBe "Lines over Lines"
    }
})
