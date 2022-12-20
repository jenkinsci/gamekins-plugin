package org.gamekins

import hudson.util.FormValidation
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.achievement.AchievementInitializer

class GamePublisherDescriptorTest : FeatureSpec({

    afterSpec {
        unmockkAll()
    }

    feature("doCheckJacocoCSVPath") {
        val project = mockkClass(hudson.model.AbstractProject::class)
        every { project.someWorkspace } returns null
        val path = ""
        mockkStatic(AchievementInitializer::class)
        every { AchievementInitializer.initializeAchievements(any()) } returns listOf()

        val desc = GamePublisherDescriptor()
        scenario("No Project")
        {
            desc.doCheckJacocoCSVPath(null, path) shouldBe FormValidation.ok()
        }

        scenario("Project has no workspace")
        {
            desc.doCheckJacocoCSVPath(project, path) shouldBe FormValidation.ok()
        }
    }
})
