package org.gamekins

import hudson.util.FormValidation
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.achievement.AchievementInitializer

class GamePublisherDescriptorTest : AnnotationSpec() {

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun doCheckJacocoCSVPath() {
        val project = mockkClass(hudson.model.AbstractProject::class)
        every { project.someWorkspace } returns null
        val path = ""
        mockkStatic(AchievementInitializer::class)
        every { AchievementInitializer.initializeAchievements(any()) } returns listOf()

        val desc = GamePublisherDescriptor()
        desc.doCheckJacocoCSVPath(null, path) shouldBe FormValidation.ok()

        desc.doCheckJacocoCSVPath(project, path) shouldBe FormValidation.ok()
    }
}
