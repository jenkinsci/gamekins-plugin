/*
 * Copyright 2020 Gamekins contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gamekins.util

import hudson.FilePath
import hudson.model.TaskListener
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.challenge.BuildChallenge
import org.gamekins.challenge.ClassCoverageChallenge
import org.gamekins.challenge.LineCoverageChallenge
import org.gamekins.test.TestUtils
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class AchievementUtilTest: AnnotationSpec() {

    private lateinit var root : String
    private val challenge = mockkClass(ClassCoverageChallenge::class)
    private val classes = arrayListOf<JacocoUtil.ClassDetails>()
    private val constants = hashMapOf<String, String>()
    private val run = mockkClass(hudson.model.Run::class)
    private val property = mockkClass(org.gamekins.GameUserProperty::class)
    private val workspace = mockkClass(FilePath::class)
    private val additionalParameters = hashMapOf<String, String>()

    @BeforeAll
    fun init() {
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)

        mockkStatic(AchievementUtil::class)
        constants["projectName"] = "Test-Project"
        every { property.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
        File(root).deleteRecursively()
    }

    @Test
    fun coverLineWithXBranches() {
        additionalParameters.clear()
        AchievementUtil.coverLineWithXBranches(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["branches"] = "2"
        val lineChallenge = mockkClass(LineCoverageChallenge::class)
        every { property.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf(lineChallenge))
        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 0
        AchievementUtil.coverLineWithXBranches(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 1
        AchievementUtil.coverLineWithXBranches(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 2
        AchievementUtil.coverLineWithXBranches(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 3
        AchievementUtil.coverLineWithXBranches(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { property.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))
    }

    @Test
    fun getLinesOfCode() {
        val path = FilePath(File("$root/src/main/java/com/example/Complex.java"))
        AchievementUtil.getLinesOfCode(path) shouldBe 108
    }

    @Test
    fun haveClassWithXCoverage() {
        additionalParameters.clear()
        every { challenge.solvedCoverage } returns 0.9
        AchievementUtil.haveClassWithXCoverage(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveCoverage"] = "0.8"
        AchievementUtil.haveClassWithXCoverage(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { challenge.solvedCoverage } returns 0.7
        AchievementUtil.haveClassWithXCoverage(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun haveXClassesWithYCoverage() {
        additionalParameters.clear()
        every { challenge.solvedCoverage } returns 0.9
        AchievementUtil.haveXClassesWithYCoverage(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveCoverage"] = "0.8"
        AchievementUtil.haveXClassesWithYCoverage(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["classesCount"] = "1"
        AchievementUtil.haveXClassesWithYCoverage(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { challenge.solvedCoverage } returns 0.7
        AchievementUtil.haveXClassesWithYCoverage(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun haveXClassesWithYCoverageAndZLines() {
        additionalParameters.clear()
        val details = mockkClass(JacocoUtil.ClassDetails::class)
        every { details.sourceFilePath } returns "/src/main/java/com/example/Complex.java"
        every { workspace.remote } returns root
        every { workspace.channel } returns null
        every { challenge.classDetails } returns details
        every { challenge.solvedCoverage } returns 0.9
        AchievementUtil.haveXClassesWithYCoverageAndZLines(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveCoverage"] = "0.8"
        AchievementUtil.haveXClassesWithYCoverageAndZLines(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["linesCount"] = "100"
        AchievementUtil.haveXClassesWithYCoverageAndZLines(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["classesCount"] = "1"
        AchievementUtil.haveXClassesWithYCoverageAndZLines(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { challenge.solvedCoverage } returns 0.7
        AchievementUtil.haveXClassesWithYCoverageAndZLines(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun haveXProjectCoverage() {
        additionalParameters.clear()
        constants["projectCoverage"] = "0.81"
        AchievementUtil.haveXProjectCoverage(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveCoverage"] = "0.8"
        AchievementUtil.haveXProjectCoverage(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true

        constants["projectCoverage"] = "0.79"
        AchievementUtil.haveXProjectCoverage(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun haveXProjectTests() {
        additionalParameters.clear()
        constants["projectTests"] = "101"
        AchievementUtil.haveXProjectTests(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveTests"] = "100"
        AchievementUtil.haveXProjectTests(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true

        constants["projectTests"] = "99"
        AchievementUtil.haveXProjectTests(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun solveChallengeInXSeconds() {
        additionalParameters.clear()
        every { challenge.getSolved() } returns 100000000
        every { challenge.getCreated() } returns 10000000
        AchievementUtil.solveChallengeInXSeconds(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["timeDifference"] = "3600"
        AchievementUtil.solveChallengeInXSeconds(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { challenge.getCreated() } returns 99996400
        AchievementUtil.solveChallengeInXSeconds(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { challenge.getCreated() } returns 99999999
        AchievementUtil.solveChallengeInXSeconds(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true
    }

    @Test
    fun solveFirstBuildFail() {
        additionalParameters.clear()
        val buildProperty = mockkClass(org.gamekins.GameUserProperty::class)
        every { buildProperty.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf())
        AchievementUtil.solveFirstBuildFail(classes, constants, run, buildProperty, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        val build = mockkClass(BuildChallenge::class)
        every { buildProperty.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf(build))
        AchievementUtil.solveFirstBuildFail(classes, constants, run, buildProperty, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true
    }

    @Test
    fun solveXChallenges() {
        additionalParameters.clear()
        AchievementUtil.solveXChallenges(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["solveNumber"] = "1"
        AchievementUtil.solveXChallenges(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true

        additionalParameters["solveNumber"] = "2"
        AchievementUtil.solveXChallenges(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun solveXAtOnce() {
        additionalParameters.clear()
        AchievementUtil.solveXAtOnce(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        constants["solved"] = "1"
        AchievementUtil.solveXAtOnce(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["solveNumber"] = "1"
        AchievementUtil.solveXAtOnce(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe true

        additionalParameters["solveNumber"] = "2"
        AchievementUtil.solveXAtOnce(classes, constants, run, property, workspace, TaskListener.NULL,
            additionalParameters) shouldBe false
    }
}