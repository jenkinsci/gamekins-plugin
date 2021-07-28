package org.gamekins.challenge.quest

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import org.gamekins.challenge.LineCoverageChallenge

class QuestStepTest : AnnotationSpec() {

    private val description = "Description"

    @Test
    fun printToXML() {
        val challenge = mockkClass(LineCoverageChallenge::class)
        every { challenge.printToXML(any(), any()) } returns "    <Challenge>"
        QuestStep(description, challenge).printToXML("") shouldBe "<QuestStep description=\"Description\">\n" +
                "    <Challenge>\n" +
                "</QuestStep>"

        every { challenge.printToXML(any(), any()) } returns "        <Challenge>"
        QuestStep(description, challenge).printToXML("    ") shouldBe "    <QuestStep description=\"Description\">\n" +
                "        <Challenge>\n" +
                "    </QuestStep>"
    }

    @Test
    fun testToString() {
        val challenge = mockkClass(LineCoverageChallenge::class)
        QuestStep(description, challenge).toString() shouldBe description

        every { challenge.toEscapedString() } returns "String"
        QuestStep("", challenge).toString() shouldBe "String"
    }
}
