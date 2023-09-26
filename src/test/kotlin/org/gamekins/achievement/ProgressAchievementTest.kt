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

class ProgressAchievementTest: FeatureSpec({
    lateinit var achievement: ProgressAchievement
    val files = arrayListOf<FileDetails>()
    val parameters = Constants.Parameters()
    val run = mockkClass(Run::class)
    val property = mockkClass(GameUserProperty::class)

    beforeSpec {
        mockkStatic(AchievementInitializer::class)
        mockkStatic(AchievementUtil::class)

        achievement = AchievementInitializer.initializeProgressAchievements("progress_achievements.json").first()
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

        val achievement2 = mockkClass(ProgressAchievement::class)
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
        every { achievement2.title } returns "Cover fire"
        scenario("No description, same title")
        {
            (achievement == achievement2) shouldBe false
        }

        every { achievement2.description } returns "Solve a CoverageChallenge with as much coverage as possible"
        scenario("Same title, same description")
        {
            (achievement == achievement2) shouldBe true
        }
    }

    feature("progress") {
        every { AchievementUtil.getMaxClassCoverage(any(), any(), any(), any(), any()) } returns 5
        scenario("Requirements met")
        {
            achievement.progress(files, parameters, run, property, TaskListener.NULL) shouldBe true
            achievement.progress shouldBe 5
        }

        every { AchievementUtil.getMaxClassCoverage(any(), any(), any(), any(), any()) } returns 0
        scenario("Requirements not met")
        {
            achievement.progress(files, parameters, run, property, TaskListener.NULL) shouldBe false
            achievement.progress shouldBe 5
        }

    }

    feature("printToXML") {
        achievement.printToXML("") shouldMatch  "<Achievement title=\"Cover fire\" description=\"Solve a CoverageChallenge with as much coverage as possible\" progress=\"[0-9]*\"/>"
    }

    feature("testToString") {
        achievement.toString() shouldBe "Cover fire: Solve a CoverageChallenge with as much coverage as possible"
    }
})