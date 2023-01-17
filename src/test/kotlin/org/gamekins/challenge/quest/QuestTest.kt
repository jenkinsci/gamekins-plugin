package org.gamekins.challenge.quest

import hudson.model.TaskListener
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.unmockkAll
import org.gamekins.challenge.LineCoverageChallenge
import org.gamekins.util.Constants

class QuestTest : AnnotationSpec() {

    private lateinit var quest: Quest
    private val challenge1 = mockkClass(LineCoverageChallenge::class)
    private val challenge2 = mockkClass(LineCoverageChallenge::class)
    private val step1 = QuestStep("", challenge1)
    private val step2 = QuestStep("", challenge2)
    private lateinit var quest1: Quest
    private val run = mockkClass(hudson.model.Run::class)

    @BeforeEach
    fun init() {
        quest = Quest("Lines over lines", arrayListOf())
        quest1 = Quest("Lines over Lines", arrayListOf(step1, step2))
        every { challenge1.equals(any()) } returns true
        every { challenge1.isSolved(any(), any(), any()) } returns true
        every { challenge1.update(any()) } returns Unit
        every { challenge1.getScore() } returns 2
        every { challenge1.isSolvable(any(), any(), any()) } returns true
        every { challenge1.printToXML(any(), any()) } returns "            <Challenge>"
        every { challenge2.equals(any()) } returns true
        every { challenge2.isSolved(any(), any(), any()) } returns true
        every { challenge2.update(any()) } returns Unit
        every { challenge2.getScore() } returns 3
        every { challenge2.isSolvable(any(), any(), any()) } returns true
        every { challenge2.printToXML(any(), any()) } returns "            <Challenge>"
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun equals() {
        quest.equals(null) shouldBe false

        quest.equals(Constants.Parameters()) shouldBe false

        (quest == quest) shouldBe true

        val challenge = mockkClass(LineCoverageChallenge::class)
        every { challenge.equals(any()) } returns true
        val quest1 = Quest("Lines over Lines", arrayListOf(QuestStep("", challenge)))
        (quest == quest1) shouldBe false

        val quest2 = Quest("Lines over Lines", arrayListOf(QuestStep("", challenge)))
        (quest1 == quest2) shouldBe true
    }

    @Test
    fun getCurrentStep() {
        quest1.getCurrentStep() shouldBe step1

        every { challenge2.isSolved(any(), any(), any()) } returns false
        quest1.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        quest1.getCurrentStep() shouldBe step2

        every { challenge2.isSolved(any(), any(), any()) } returns true
        quest1.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        quest1.getCurrentStep() shouldBe step2
    }

    @Test
    fun getCurrentStepNumber() {
        quest1.getCurrentStepNumber() shouldBe 0

        every { challenge2.isSolved(any(), any(), any()) } returns false
        quest1.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        quest1.getCurrentStepNumber() shouldBe 1

        every { challenge2.isSolved(any(), any(), any()) } returns true
        quest1.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        quest1.getCurrentStepNumber() shouldBe 2
    }

    @Test
    fun getLastStep() {
        quest1.getLastStep() shouldBe step1

        every { challenge2.isSolved(any(), any(), any()) } returns false
        quest1.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        quest1.getLastStep() shouldBe step1

        every { challenge2.isSolved(any(), any(), any()) } returns true
        quest1.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        quest1.getLastStep() shouldBe step2
    }

    @Test
    fun getScore() {
        quest.getScore() shouldBe 0

        quest1.getScore() shouldBe 7
    }

    @Test
    fun isCurrentStepSolved() {
        quest.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe false

        every { challenge2.isSolved(any(), any(), any()) } returns false
        quest1.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true

        quest1.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe false

        every { challenge2.isSolved(any(), any(), any()) } returns true
        quest1.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
    }

    @Test
    fun isSolvable() {
        quest.isSolvable(Constants.Parameters(), run, TaskListener.NULL) shouldBe false

        quest1.isSolvable(Constants.Parameters(), run, TaskListener.NULL) shouldBe true

        every { challenge1.isSolvable(any(), any(), any()) } returns false
        quest1.isSolvable(Constants.Parameters(), run, TaskListener.NULL) shouldBe false
    }

    @Test
    fun isSolved() {
        quest.isSolved() shouldBe false

        every { challenge2.isSolved(any(), any(), any()) } returns false
        quest1.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        quest1.isSolved() shouldBe false

        every { challenge2.isSolved(any(), any(), any()) } returns true
        quest1.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        quest1.isSolved() shouldBe true
        quest1.solved shouldNotBe 0

        val quest2 = Quest("Lines over Lines", arrayListOf(step1, step2))
        quest2.isCurrentStepSolved(Constants.Parameters(), run, TaskListener.NULL) shouldBe true
        quest2.getCurrentStepNumber() shouldBe 2
        quest2.isSolved() shouldBe true
        quest2.solved shouldNotBe 0
    }

    @Test
    fun printToXML() {
        val result1 = "<Quest name=\"Lines over Lines\" created=\"${quest1.created}\" solved=\"0\">\n" +
                "    <QuestSteps count=\"2\">\n" +
                "        <QuestStep description=\"\">\n" +
                "            <Challenge>\n" +
                "        </QuestStep>\n" +
                "        <QuestStep description=\"\">\n" +
                "            <Challenge>\n" +
                "        </QuestStep>\n" +
                "    </QuestSteps>\n" +
                "</Quest>"
        quest1.printToXML("", "") shouldBe result1

        val result2 = "<Quest name=\"Lines over Lines\" created=\"${quest1.created}\" solved=\"0\" reason=\"reason\">\n" +
                "    <QuestSteps count=\"2\">\n" +
                "        <QuestStep description=\"\">\n" +
                "            <Challenge>\n" +
                "        </QuestStep>\n" +
                "        <QuestStep description=\"\">\n" +
                "            <Challenge>\n" +
                "        </QuestStep>\n" +
                "    </QuestSteps>\n" +
                "</Quest>"
        quest1.printToXML("reason", "") shouldBe result2
    }

    @Test
    fun testToString() {
        quest1.toString() shouldBe "Lines over Lines"
    }
}
