package org.gamekins.achievement

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.GameUserProperty
import org.gamekins.util.AchievementUtil
import org.gamekins.util.JacocoUtil

class AchievementTest: AnnotationSpec() {

    private lateinit var achievement: Achievement
    private val classes = arrayListOf<JacocoUtil.ClassDetails>()
    private val constants = hashMapOf<String, String>()
    private val run = mockkClass(Run::class)
    private val property = mockkClass(GameUserProperty::class)
    private val workspace = mockkClass(FilePath::class)

    @BeforeEach
    fun init() {
        mockkStatic(AchievementInitializer::class)
        mockkStatic(AchievementUtil::class)

        achievement = AchievementInitializer.initializeAchievement("solve_challenge.json")
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun getSolvedTimeString() {
        achievement.solvedTimeString shouldBe "Not solved"

        every { AchievementUtil.solveXChallenges(any(), any(), any(), any(), any(), any(), any()) } returns true
        achievement.isSolved(classes, constants, run, property, workspace, TaskListener.NULL) shouldBe true
        achievement.solvedTimeString shouldNotBe "Not solved"
    }

    @Test
    fun testEquals() {
        achievement.equals(null) shouldBe false

        achievement.equals(classes) shouldBe false

        val achievement2 = mockkClass(Achievement::class)
        every { achievement2.badgePath } returns ""
        every { achievement2.description } returns ""
        every { achievement2.title } returns ""
        (achievement == achievement2) shouldBe false

        every { achievement2.badgePath } returns "/plugin/gamekins/icons/trophy.png"
        every { achievement2.description } returns "Solve your first Challenge"
        every { achievement2.title } returns "I took the first Challenge"
        (achievement == achievement2) shouldBe true
    }

    @Test
    fun isSolved() {
        every { AchievementUtil.solveXChallenges(any(), any(), any(), any(), any(), any(), any()) } returns false
        achievement.isSolved(classes, constants, run, property, workspace, TaskListener.NULL) shouldBe false

        every { AchievementUtil.solveXChallenges(any(), any(), any(), any(), any(), any(), any()) } returns true
        achievement.isSolved(classes, constants, run, property, workspace, TaskListener.NULL) shouldBe true
    }

    @Test
    fun printToXML() {
        achievement.printToXML("") shouldBe "<Achievement title=\"I took the first Challenge\" description=\"Solve your first Challenge\" secret=\"false\" solved=\"0\"/>"
    }

    @Test
    fun testToString() {
        achievement.toString() shouldBe "I took the first Challenge: Solve your first Challenge"
    }
}