package org.gamekins.questtask

import hudson.FilePath
import hudson.model.User
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.unmockkAll
import org.gamekins.GameUserProperty
import org.gamekins.challenge.Challenge
import org.gamekins.challenge.LineCoverageChallenge
import org.gamekins.util.Pair
import java.util.concurrent.CopyOnWriteArrayList

class QuestTaskTest : FeatureSpec({

    val workspace = mockkClass(FilePath::class)
    val questTask = AddMoreTestsQuestTask(10, workspace)

    afterSpec {
        unmockkAll()
    }

    feature("getCompletedPercentage") {
        scenario("0%") {
            questTask.getCompletedPercentage() shouldBe 0
        }

        scenario("50%") {
            questTask.currentNumber = 5
            questTask.getCompletedPercentage() shouldBe 50
        }

        scenario("100%") {
            questTask.currentNumber = 10
            questTask.getCompletedPercentage() shouldBe 100
        }

        scenario("110%") {
            questTask.currentNumber = 11
            questTask.getCompletedPercentage() shouldBe 100
        }
    }

    feature("getSolvedChallengesOfUserSince") {
        val user = mockkClass(User::class)
        val project = "test"
        val since = 1000L
        val type = LineCoverageChallenge::class.java
        every { user.getProperty(GameUserProperty::class.java) } returns null
        scenario("property is null") {
            questTask.getSolvedChallengesOfUserSince(user, project, since, type) should beEmpty()
        }

        val property = mockkClass(GameUserProperty::class)
        every { user.getProperty(GameUserProperty::class.java) } returns property
        every { property.getCompletedChallenges(project) } returns CopyOnWriteArrayList()
        scenario("No solved challenges") {
            questTask.getSolvedChallengesOfUserSince(user, project, since, type) should beEmpty()
        }

        val challenges = CopyOnWriteArrayList<Challenge>()
        val challenge1 = mockkClass(LineCoverageChallenge::class)
        every { challenge1.getSolved() } returns 1500L
        challenges.add(challenge1)
        every { property.getCompletedChallenges(project) } returns challenges
        scenario("One challenge solved since") {
            questTask.getSolvedChallengesOfUserSince(user, project, since) shouldHaveSize 1
        }

        challenges.removeFirst()
        every { challenge1.getSolved() } returns 500L
        challenges.add(challenge1)
        every { property.getCompletedChallenges(project) } returns challenges
        scenario("No challenge solved since") {
            questTask.getSolvedChallengesOfUserSince(user, project, since) should beEmpty()
        }
    }

    feature("getSolvedChallengesOfUserSinceLastRejection") {
        val user = mockkClass(User::class)
        val project = "test"
        val since = 1000L
        every { user.getProperty(GameUserProperty::class.java) } returns null
        scenario("property is null") {
            questTask.getSolvedChallengesOfUserSinceLastRejection(user, project, since) should beEmpty()
        }

        val property = mockkClass(GameUserProperty::class)
        every { user.getProperty(GameUserProperty::class.java) } returns property
        val challenges = CopyOnWriteArrayList<Challenge>()
        val challenge1 = mockkClass(LineCoverageChallenge::class)
        every { challenge1.getSolved() } returns 1500L
        challenges.add(challenge1)
        val challenge2 = mockkClass(LineCoverageChallenge::class)
        every { challenge2.getSolved() } returns 500L
        challenges.add(challenge2)
        every { property.getCompletedChallenges(project) } returns challenges
        every { property.getRejectedChallenges(project) } returns CopyOnWriteArrayList()
        scenario("No rejected challenges") {
            questTask.getSolvedChallengesOfUserSinceLastRejection(user, project, since) shouldHaveSize 1
        }

        val rejectedChallenge = mockkClass(LineCoverageChallenge::class)
        every { rejectedChallenge.getSolved() } returns 1200L
        val rejectedChallenges = CopyOnWriteArrayList<Pair<Challenge, String>>()
        rejectedChallenges.add(Pair(rejectedChallenge, "reason"))
        every { property.getRejectedChallenges(project) } returns rejectedChallenges
        scenario("Rejected challenge before challenge") {
            questTask.getSolvedChallengesOfUserSinceLastRejection(user, project, since) shouldHaveSize 1
        }

        every { rejectedChallenge.getSolved() } returns 1600L
        rejectedChallenges.removeFirst()
        rejectedChallenges.add(Pair(rejectedChallenge, "reason"))
        every { property.getRejectedChallenges(project) } returns rejectedChallenges
        scenario("Rejected challenge after challenge") {
            questTask.getSolvedChallengesOfUserSinceLastRejection(user, project, since) should beEmpty()
        }
    }
})