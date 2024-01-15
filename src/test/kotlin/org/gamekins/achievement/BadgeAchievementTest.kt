package org.gamekins.achievement

import hudson.model.Run
import hudson.model.TaskListener
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.GameUserProperty
import org.gamekins.file.FileDetails
import org.gamekins.util.AchievementUtil
import org.gamekins.util.Constants

class BadgeAchievementTest: FeatureSpec({
    lateinit var achievement: BadgeAchievement
    val files = arrayListOf<FileDetails>()
    val parameters = Constants.Parameters()
    val run = mockkClass(Run::class)
    val property = mockkClass(GameUserProperty::class)

    beforeSpec {
        mockkStatic(AchievementInitializer::class)
        mockkStatic(AchievementUtil::class)

        achievement = AchievementInitializer.initializeBadgeAchievements("badge_achievements.json").first()
    }

    afterSpec {
        unmockkAll()
    }

    feature("clone") {
        achievement.clone() shouldBe achievement
    }

    feature("testEquals") {
        scenario("Null")
        {
            achievement.equals(null) shouldBe false
        }

        scenario("Different class")
        {
            achievement.equals(files) shouldBe false
        }

        val achievement2 = mockkClass(BadgeAchievement::class)
        every { achievement2.description } returns ""
        every { achievement2.title } returns ""
        scenario("No title, no description")
        {
            (achievement == achievement2) shouldBe false
        }

        every { achievement2.description } returns "Solve your first Challenge"
        scenario("No title")
        {
            (achievement == achievement2) shouldBe false
        }

        every { achievement2.description } returns ""
        every { achievement2.title } returns "Multitasking"
        scenario("No description, same title")
        {
            (achievement == achievement2) shouldBe false
        }

        every { achievement2.description } returns "Solve as many challenges as possible at once"
        scenario("Same title, same description")
        {
            (achievement == achievement2) shouldBe true
        }
    }

    feature("update") {
        var badgeCountExpected1 = 0
        var badgeCountExpected2 = 0

        every { AchievementUtil.getSolvedChallengesSimultaneouslyCount(any(), any(), any(), any(), any()) } returns listOf(3.0)
        scenario("Requirements met for first badge")
        {
            achievement.update(files, parameters, run, property, TaskListener.NULL) shouldBe true
            badgeCountExpected1++
            achievement.badgeCounts[0] shouldBe badgeCountExpected1
            achievement.badgeCounts[1] shouldBe badgeCountExpected2
        }

        every { AchievementUtil.getSolvedChallengesSimultaneouslyCount(any(), any(), any(), any(), any()) } returns listOf(4.0)
        scenario("Requirements met for second badge")
        {
            achievement.update(files, parameters, run, property, TaskListener.NULL) shouldBe true
            badgeCountExpected2++
            achievement.badgeCounts[0] shouldBe badgeCountExpected1
            achievement.badgeCounts[1] shouldBe badgeCountExpected2
        }

        every { AchievementUtil.getSolvedChallengesSimultaneouslyCount(any(), any(), any(), any(), any()) } returns listOf(0.0)
        scenario("Requirements not met")
        {
            achievement.update(files, parameters, run, property, TaskListener.NULL) shouldBe false
            achievement.badgeCounts[0] shouldBe badgeCountExpected1
            achievement.badgeCounts[1] shouldBe badgeCountExpected2
        }

    }

    feature("printToXML") {
        achievement.printToXML("") shouldMatch  "<Achievement title=\"Multitasking\" description=\"Solve as many challenges as possible at once\" badgeCounts=\"\\[[0-9]*, [0-9]*\\]\"/>"
    }

    feature("testToString") {
        achievement.toString() shouldBe "Multitasking: Solve as many challenges as possible at once"
    }
})