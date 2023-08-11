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
import org.gamekins.challenge.*
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.Pair
import java.util.concurrent.CopyOnWriteArrayList

class SolveChallengesWithoutRejectionQuestTaskTest : FeatureSpec({

    val questTask = SolveChallengesWithoutRejectionQuestTask(1)

    afterSpec {
        unmockkAll()
    }

    feature("getScore") {
        questTask.getScore() shouldBe 1
    }

    feature("isSolved") {
        val parameters = Parameters(projectName = "test")
        val run = mockkClass(Run::class)
        val listener = TaskListener.NULL
        val user = mockkClass(User::class)
        val property = mockkClass(GameUserProperty::class)
        every { user.getProperty(GameUserProperty::class.java) } returns property
        val challenges = CopyOnWriteArrayList<Challenge>()
        val challenge1 = mockkClass(LineCoverageChallenge::class)
        every { challenge1.getSolved() } returns questTask.created + 1000
        challenges.add(challenge1)
        val challenge2 = mockkClass(LineCoverageChallenge::class)
        every { challenge2.getSolved() } returns questTask.created - 1000
        challenges.add(challenge2)
        every { property.getCompletedChallenges(any()) } returns challenges
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList()
        scenario("No rejected challenges") {
            questTask.isSolved(parameters, run, listener, user) shouldBe  true
        }

        val rejectedChallenge = mockkClass(LineCoverageChallenge::class)
        every { rejectedChallenge.getSolved() } returns questTask.created + 500
        val rejectedChallenges = CopyOnWriteArrayList<Pair<Challenge, String>>()
        rejectedChallenges.add(Pair(rejectedChallenge, "reason"))
        every { property.getRejectedChallenges(any()) } returns rejectedChallenges
        scenario("Rejected challenge before challenge") {
            questTask.isSolved(parameters, run, listener, user) shouldBe  true
        }

        every { rejectedChallenge.getSolved() } returns questTask.created + 2000
        rejectedChallenges.removeFirst()
        rejectedChallenges.add(Pair(rejectedChallenge, "reason"))
        every { property.getRejectedChallenges(any()) } returns rejectedChallenges
        every { property.getCompletedChallenges(any()) } returns challenges
        scenario("Rejected challenge after challenge") {
            questTask.isSolved(parameters, run, listener, user) shouldBe  false
        }
    }

    feature("printToXML") {
        scenario("Without indentation") {
            questTask.printToXML("") shouldBe "<SolveChallengesWithoutRejectionQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" currentNumber=\"${questTask.currentNumber}\" numberGoal=\"1\">"
        }

        scenario("With indentation") {
            questTask.printToXML("    ") shouldBe "    <SolveChallengesWithoutRejectionQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" currentNumber=\"${questTask.currentNumber}\" numberGoal=\"1\">"
        }
    }

    feature("toString") {
        questTask.toString() shouldBe "Solve 1 challenge(s) without rejecting one"
    }
})