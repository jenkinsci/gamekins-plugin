package org.gamekins.challenge.quest

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import org.gamekins.challenge.LineCoverageChallenge

class QuestStepTest : FeatureSpec({

    val description = "Description"

    feature("printToXML") {
        val challenge = mockkClass(LineCoverageChallenge::class)
        every { challenge.printToXML(any(), any()) } returns "    <Challenge>"
        scenario("With Indentation")
        {
            QuestStep(description, challenge).printToXML("") shouldBe "<QuestStep description=\"Description\">\n" +
                    "    <Challenge>\n" +
                    "</QuestStep>"
        }

        every { challenge.printToXML(any(), any()) } returns "        <Challenge>"
        scenario("Without Indentation")
        {
            QuestStep(description, challenge).printToXML("    ") shouldBe "    <QuestStep description=\"Description\">\n" +
                    "        <Challenge>\n" +
                    "    </QuestStep>"
        }
    }

    feature("ToString") {
        val challenge = mockkClass(LineCoverageChallenge::class)
        scenario("From Description")
        {
            QuestStep(description, challenge).toString() shouldBe description
        }

        every { challenge.toEscapedString() } returns "String"
        scenario("Empty description, from EscapedString")
        {
            QuestStep("", challenge).toString() shouldBe "String"
        }
    }
})
