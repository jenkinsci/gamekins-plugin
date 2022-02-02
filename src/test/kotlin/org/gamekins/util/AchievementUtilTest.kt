/*
 * Copyright 2021 Gamekins contributors
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
import hudson.model.ItemGroup
import hudson.model.Result
import hudson.model.TaskListener
import hudson.model.User
import hudson.tasks.junit.TestResultAction
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.gamekins.challenge.BuildChallenge
import org.gamekins.challenge.ClassCoverageChallenge
import org.gamekins.challenge.LineCoverageChallenge
import org.gamekins.file.FileDetails
import org.gamekins.file.SourceFileDetails
import org.gamekins.property.GameJobProperty
import org.gamekins.statistics.Statistics
import org.gamekins.util.Constants.Parameters
import org.gamekins.test.TestUtils
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class AchievementUtilTest: AnnotationSpec() {

    private lateinit var root : String
    private val challenge = mockkClass(ClassCoverageChallenge::class)
    private val files = arrayListOf<FileDetails>()
    private val parameters = Parameters()
    private val run = mockkClass(hudson.model.Run::class)
    private val property = mockkClass(org.gamekins.GameUserProperty::class)
    private val workspace = mockkClass(FilePath::class)
    private val additionalParameters = hashMapOf<String, String>()
    private lateinit var path: FilePath

    @BeforeAll
    fun init() {
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
        path = FilePath(null, root)

        mockkStatic(AchievementUtil::class)
        every { workspace.remote } returns ""
        parameters.projectName = "Test-Project"
        parameters.workspace = workspace
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
        AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["branches"] = "2"
        val lineChallenge = mockkClass(LineCoverageChallenge::class)
        every { property.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf(lineChallenge))
        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 0
        AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 1
        AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 2
        AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 3
        AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        additionalParameters["maxBranches"] = "3"
        AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["maxBranches"] = "4"
        AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { property.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))
    }

    @Test
    fun getLinesOfCode() {
        val path = FilePath(File("$root/src/main/java/com/example/Complex.java"))
        AchievementUtil.getLinesOfCode(path) shouldBe 108
    }

    @Test
    fun haveBuildWithXSeconds() {
        additionalParameters.clear()
        every { run.duration } returns 100000000000
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { run.result } returns Result.FAILURE
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["more"] = "false"
        every { run.result } returns Result.SUCCESS
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["more"] = "true"
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["duration"] = "100000000"
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { run.duration } returns 100000000001
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { run.duration } returns 100000000000
        additionalParameters["more"] = "false"
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { run.duration } returns 99999999999
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { run.duration } returns 99999999998
        additionalParameters["minDuration"] = "99999999999"
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { run.duration } returns 99999999998
        additionalParameters["minDuration"] = "99999999997"
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { run.duration } returns 100000000002
        additionalParameters["more"] = "true"
        additionalParameters["maxDuration"] = "100000000001"
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { run.duration } returns 100000000002
        additionalParameters["maxDuration"] = "100000000003"
        AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true
    }

    @Test
    fun haveClassWithXCoverage() {
        additionalParameters.clear()
        every { challenge.solvedCoverage } returns 0.9
        AchievementUtil.haveClassWithXCoverage(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveCoverage"] = "0.8"
        AchievementUtil.haveClassWithXCoverage(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { challenge.solvedCoverage } returns 0.7
        AchievementUtil.haveClassWithXCoverage(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun haveXClassesWithYCoverage() {
        additionalParameters.clear()
        every { challenge.solvedCoverage } returns 0.9
        AchievementUtil.haveXClassesWithYCoverage(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveCoverage"] = "0.8"
        AchievementUtil.haveXClassesWithYCoverage(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["classesCount"] = "1"
        AchievementUtil.haveXClassesWithYCoverage(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { challenge.solvedCoverage } returns 0.7
        AchievementUtil.haveXClassesWithYCoverage(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun haveXClassesWithYCoverageAndZLines() {
        additionalParameters.clear()
        val details = mockkClass(SourceFileDetails::class)
        every { details.filePath } returns "/src/main/java/com/example/Complex.java"
        every { workspace.remote } returns root
        every { workspace.channel } returns null
        every { challenge.details } returns details
        every { challenge.solvedCoverage } returns 0.9
        parameters.workspace = workspace
        AchievementUtil.haveXClassesWithYCoverageAndZLines(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveCoverage"] = "0.8"
        AchievementUtil.haveXClassesWithYCoverageAndZLines(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["linesCount"] = "100"
        AchievementUtil.haveXClassesWithYCoverageAndZLines(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["classesCount"] = "1"
        AchievementUtil.haveXClassesWithYCoverageAndZLines(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { challenge.solvedCoverage } returns 0.7
        AchievementUtil.haveXClassesWithYCoverageAndZLines(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun haveXFailedTests() {
        additionalParameters.clear()
        every { run.result } returns Result.SUCCESS
        every { run.getAction(TestResultAction::class.java) } returns null
        AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        mockkStatic(JUnitUtil::class)
        every { run.result } returns Result.FAILURE
        every { JUnitUtil.getTestFailCount(any(), any()) } returns 0
        AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["failedTests"] = "1"
        AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { JUnitUtil.getTestFailCount(any(), any()) } returns 1
        AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        additionalParameters["failedTests"] = "0"
        every { JUnitUtil.getTestCount(any(), any()) } returns 2
        AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { JUnitUtil.getTestCount(any(), any()) } returns 1
        AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { JUnitUtil.getTestCount(any(), any()) } returns 0
        AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun haveXProjectCoverage() {
        additionalParameters.clear()
        parameters.projectCoverage = 0.81
        AchievementUtil.haveXProjectCoverage(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveCoverage"] = "0.8"
        AchievementUtil.haveXProjectCoverage(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        parameters.projectCoverage = 0.79
        AchievementUtil.haveXProjectCoverage(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun haveXProjectTests() {
        additionalParameters.clear()
        parameters.projectTests = 101
        AchievementUtil.haveXProjectTests(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveTests"] = "100"
        AchievementUtil.haveXProjectTests(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        parameters.projectTests = 99
        AchievementUtil.haveXProjectTests(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun improveClassCoverageByX() {
        additionalParameters.clear()
        every { challenge.coverage } returns 0.0
        every { challenge.solvedCoverage } returns 0.0
        AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveCoverage"] = "0.1"
        AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { challenge.coverage } returns 0.7
        every { challenge.solvedCoverage } returns 0.75
        AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { challenge.solvedCoverage } returns 0.8
        AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        every { challenge.solvedCoverage } returns 0.85
        AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        additionalParameters["maxCoverage"] = "0.15"
        AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["maxCoverage"] = "0.2"
        AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true
    }

    @Test
    fun improveProjectCoverageByX() {
        additionalParameters.clear()
        mockkStatic(GitUtil::class)
        mockkStatic(User::class)
        val head = mockkClass(RevCommit::class)
        val user = mockkClass(User::class)
        val user2 = mockkClass(User::class)
        val userList = arrayListOf(user)
        every { User.getAll() } returns userList
        every { head.authorIdent } returns mockkClass(PersonIdent::class)
        every { GitUtil.getHead(any()) } returns head
        every { GitUtil.mapUser(any(), userList) } returns user
        every { property.getUser() } returns user2
        parameters.workspace = path
        AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { property.getUser() } returns user
        val job = mockkClass(WorkflowJob::class)
        val jobProperty = mockkClass(GameJobProperty::class)
        val statistics = mockkClass(Statistics::class)
        parameters.branch = "master"
        every { run.parent } returns job
        every { job.parent } returns mockkClass(ItemGroup::class)
        every { job.getProperty(any()) } returns jobProperty
        every { jobProperty.getStatistics() } returns statistics
        every { statistics.getLastRun("master") } returns null
        AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        val runEntry = mockkClass(Statistics.RunEntry::class)
        parameters.projectCoverage = 0.7
        every { runEntry.coverage } returns 0.6
        every { statistics.getLastRun("master") } returns runEntry
        AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveCoverage"] = "0.2"
        AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["haveCoverage"] = "0.1"
        AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        additionalParameters["haveCoverage"] = "0.05"
        AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        additionalParameters["maxCoverage"] = "0.09"
        AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["maxCoverage"] = "0.2"
        AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true
    }

    @Test
    fun solveChallengeInXSeconds() {
        additionalParameters.clear()
        every { challenge.getSolved() } returns 100000000
        every { challenge.getCreated() } returns 10000000
        AchievementUtil.solveChallengeInXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["timeDifference"] = "3600"
        AchievementUtil.solveChallengeInXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["minTimeDifference"] = "3000"
        AchievementUtil.solveChallengeInXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { challenge.getCreated() } returns 99996400
        AchievementUtil.solveChallengeInXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        every { challenge.getCreated() } returns 96990000
        AchievementUtil.solveChallengeInXSeconds(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true
    }

    @Test
    fun solveFirstBuildFail() {
        additionalParameters.clear()
        val buildProperty = mockkClass(org.gamekins.GameUserProperty::class)
        every { buildProperty.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf())
        AchievementUtil.solveFirstBuildFail(files, parameters, run, buildProperty, TaskListener.NULL,
            additionalParameters) shouldBe false

        val build = mockkClass(BuildChallenge::class)
        every { buildProperty.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf(build))
        AchievementUtil.solveFirstBuildFail(files, parameters, run, buildProperty, TaskListener.NULL,
            additionalParameters) shouldBe true
    }

    @Test
    fun solveXChallenges() {
        additionalParameters.clear()
        AchievementUtil.solveXChallenges(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["solveNumber"] = "1"
        AchievementUtil.solveXChallenges(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        additionalParameters["solveNumber"] = "2"
        AchievementUtil.solveXChallenges(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false
    }

    @Test
    fun solveXAtOnce() {
        additionalParameters.clear()
        AchievementUtil.solveXAtOnce(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        parameters.solved = 1
        AchievementUtil.solveXAtOnce(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false

        additionalParameters["solveNumber"] = "1"
        AchievementUtil.solveXAtOnce(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe true

        additionalParameters["solveNumber"] = "2"
        AchievementUtil.solveXAtOnce(files, parameters, run, property, TaskListener.NULL,
            additionalParameters) shouldBe false
    }
}