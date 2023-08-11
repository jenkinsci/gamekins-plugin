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
import java.util.concurrent.CopyOnWriteArrayList

class SolveChallengesQuestTaskTest : FeatureSpec({

    val challengeType = LineCoverageChallenge::class.java
    val questTask = SolveChallengesQuestTask(1, challengeType)

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
        every { property.getCompletedChallenges(any()) } returns CopyOnWriteArrayList()
        scenario("Not enough new challenges") {
            questTask.isSolved(parameters, run, listener, user) shouldBe false
        }

        val challenges = CopyOnWriteArrayList<Challenge>()
        val challenge1 = mockkClass(LineCoverageChallenge::class)
        every { challenge1.getSolved() } returns questTask.created + 1000
        challenges.add(challenge1)
        every { property.getCompletedChallenges(any()) } returns challenges
        scenario("One new challenge") {
            questTask.isSolved(parameters, run, listener, user) shouldBe true
        }
    }

    feature("printToXML") {
        scenario("Without indentation") {
            questTask.printToXML("") shouldBe "<SolveChallengesQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" currentNumber=\"${questTask.currentNumber}\" numberGoal=\"1\">"
        }

        scenario("With indentation") {
            questTask.printToXML("    ") shouldBe "    <SolveChallengesQuestTask created=\"${questTask.created}\" solved=\"${questTask.solved}\" currentNumber=\"${questTask.currentNumber}\" numberGoal=\"1\">"
        }
    }

    feature("toString") {
        scenario("Line Coverage") {
            questTask.toString() shouldBe "Solve one Line Coverage Challenge"
        }

        var task2 = SolveChallengesQuestTask(10, BranchCoverageChallenge::class.java)
        scenario("Branch Coverage") {
            task2.toString() shouldBe "Solve 10 Branch Coverage Challenges"
        }

        task2 = SolveChallengesQuestTask(1, BranchCoverageChallenge::class.java)
        scenario("One Branch Coverage") {
            task2.toString() shouldBe "Solve one Branch Coverage Challenge"
        }

        task2 = SolveChallengesQuestTask(10, MethodCoverageChallenge::class.java)
        scenario("Method Coverage") {
            task2.toString() shouldBe "Solve 10 Method Coverage Challenges"
        }

        task2 = SolveChallengesQuestTask(10, ClassCoverageChallenge::class.java)
        scenario("Class Coverage") {
            task2.toString() shouldBe "Solve 10 Class Coverage Challenges"
        }

        task2 = SolveChallengesQuestTask(10, CoverageChallenge::class.java)
        scenario("Coverage") {
            task2.toString() shouldBe "Solve 10 Coverage Challenges"
        }

        task2 = SolveChallengesQuestTask(10, MutationChallenge::class.java)
        scenario("Mutation") {
            task2.toString() shouldBe "Solve 10 Mutation Challenges"
        }

        task2 = SolveChallengesQuestTask(10, SmellChallenge::class.java)
        scenario("Smell") {
            task2.toString() shouldBe "Solve 10 Smell Challenges"
        }

        task2 = SolveChallengesQuestTask(10, Challenge::class.java)
        scenario("General Challenge") {
            task2.toString() shouldBe "Solve 10 Challenges"
        }
    }
})