package org.gamekins.questtask

import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockkClass
import io.mockk.unmockkAll
import org.gamekins.challenge.LineCoverageChallenge
import org.gamekins.util.Constants.Parameters

class ReceiveChallengeQuestTaskTest : FeatureSpec({

    val questTask = ReceiveChallengeQuestTask()
    val challenge = mockkClass(LineCoverageChallenge::class)

    afterSpec {
        unmockkAll()
    }

    feature("challengeSent") {
        questTask.challengeSent(challenge)
        questTask.challenges shouldHaveSize 1
        questTask.currentNumber shouldBe 1
    }

    feature("getScore") {
        questTask.getScore() shouldBe 1
    }

    feature("isSolved") {
        val parameters = Parameters()
        val run = mockkClass(Run::class)
        val listener = TaskListener.NULL
        val user = mockkClass(User::class)
        questTask.currentNumber = 0
        scenario("Goal not reached") {
            questTask.isSolved(parameters, run, listener, user) shouldBe false
        }

        questTask.challengeSent(challenge)
        scenario("Goal reached") {
            questTask.isSolved(parameters, run, listener, user) shouldBe true
        }
    }

    feature("printToXML") {
        scenario("Without indentation") {
            questTask.printToXML("") shouldBe "<ReceiveChallengeQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" currentNumber=\"${questTask.currentNumber}\" numberGoal=\"1\">"
        }

        scenario("With indentation") {
            questTask.printToXML("    ") shouldBe "    <ReceiveChallengeQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" currentNumber=\"${questTask.currentNumber}\" numberGoal=\"1\">"
        }
    }

    feature("toString") {
        questTask.toString() shouldBe "Receive one challenge from your colleagues"
    }
})