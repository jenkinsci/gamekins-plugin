/*
 * Copyright 2022 Gamekins contributors
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
import io.kotest.core.spec.style.FeatureSpec
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

class AchievementUtilTest: FeatureSpec({

    lateinit var root : String
    val challenge = mockkClass(ClassCoverageChallenge::class)
    val files = arrayListOf<FileDetails>()
    val parameters = Parameters()
    val run = mockkClass(hudson.model.Run::class)
    val property = mockkClass(org.gamekins.GameUserProperty::class)
    val workspace = mockkClass(FilePath::class)
    val additionalParameters = hashMapOf<String, String>()
    lateinit var path: FilePath

    beforeSpec {
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory!!.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
        path = FilePath(null, root)

        mockkStatic(AchievementUtil::class)
        every { workspace.remote } returns ""
        parameters.projectName = "Test-Project"
        parameters.workspace = workspace
        every { property.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))
    }

    afterSpec {
        unmockkAll()
        File(root).deleteRecursively()
    }

    feature("coverLineWithXBranches") {
        additionalParameters.clear()
        scenario("No Parameters")
        {
            AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["branches"] = "2"
        val lineChallenge = mockkClass(LineCoverageChallenge::class)
        every { property.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf(lineChallenge))
        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 0
        scenario("Lines do not have enough (0) branches")
        {
            AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 1
        scenario("Lines do not have enough (1) branches")
        {
            AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 2
        scenario("Lines have exactly enough branches")
        {
            AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        every { lineChallenge.getMaxCoveredBranchesIfFullyCovered() } returns 3
        scenario("Lines have more than enough branches")
        {
            AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }


        additionalParameters["maxBranches"] = "3"
        scenario("Lines have to many branches")
        {
            AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["maxBranches"] = "4"
        scenario("Lines have enough branches with upper limit")
        {
            AchievementUtil.coverLineWithXBranches(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        every { property.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))
    }

    feature("getLinesOfCode") {
        var filePath : FilePath

        scenario("Lines of Code of Complex.java")
        {
            filePath = FilePath(File("$root/src/main/java/com/example/Complex.java"))
            AchievementUtil.getLinesOfCode(filePath) shouldBe 108
        }

        scenario("Lines of Code of Rational.java")
        {
            filePath = FilePath(File("$root/src/main/java/com/example/Rational.java"))
            AchievementUtil.getLinesOfCode(filePath) shouldBe 206
        }
    }

    feature("haveBuildWithXSeconds") {
        additionalParameters.clear()
        scenario("No Value for additionalParameters[\"more\"]")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { run.duration } returns 100000000000
        additionalParameters["more"] = "has to have a value for this test"
        every { run.result } returns Result.FAILURE
        scenario("Build failed")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["more"] = "false"
        every { run.result } returns Result.SUCCESS
        scenario("Mode: more, no duration specified")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["more"] = "true"
        scenario("Mode: less, no duration specified")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["duration"] = "100000000"
        scenario("Mode : more, unsuccessful")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { run.duration } returns 100000000001
        scenario("Mode : more, successful")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        every { run.duration } returns 100000000000
        additionalParameters["more"] = "false"
        scenario("Mode: less, unsuccessful")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { run.duration } returns 99999999999
        scenario("Mode: less, successful")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        every { run.duration } returns 99999999998
        additionalParameters["minDuration"] = "99999999999"
        scenario("duration below minDuration")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { run.duration } returns 99999999998
        additionalParameters["minDuration"] = "99999999997"
        scenario("duration above minDuration")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        every { run.duration } returns 100000000002
        additionalParameters["more"] = "true"
        additionalParameters["maxDuration"] = "100000000001"
        scenario("duration above maxDuration")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { run.duration } returns 100000000002
        additionalParameters["maxDuration"] = "100000000003"
        scenario("duration below maxDuration")
        {
            AchievementUtil.haveBuildWithXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }
    }

    feature("haveClassWithXCoverage") {
        additionalParameters.clear()
        every { challenge.solvedCoverage } returns 0.9
        scenario("No Coverage specified")
        {
            AchievementUtil.haveClassWithXCoverage(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["haveCoverage"] = "0.8"
        scenario("Coverage above Requirement")
        {
            AchievementUtil.haveClassWithXCoverage(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        every { challenge.solvedCoverage } returns 0.7
        scenario("Coverage below Requirement")
        {
            AchievementUtil.haveClassWithXCoverage(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }
    }

    feature("haveXClassesWithYCoverage") {
        additionalParameters.clear()
        every { challenge.solvedCoverage } returns 0.9

        scenario("No Coverage specified")
        {
            AchievementUtil.haveXClassesWithYCoverage(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }
        additionalParameters["haveCoverage"] = "0.8"
        scenario("No ClassCount specified")
        {
            AchievementUtil.haveXClassesWithYCoverage(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }
        additionalParameters["classesCount"] = "1"
        scenario("1 Class above Coverage needed")
        {
            AchievementUtil.haveXClassesWithYCoverage(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }
        every { challenge.solvedCoverage } returns 0.7
        scenario("All Classes below required Coverage")
        {
            AchievementUtil.haveXClassesWithYCoverage(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }
    }

    feature("haveXClassesWithYCoverageAndZLines") {
        additionalParameters.clear()
        val details = mockkClass(SourceFileDetails::class)
        every { details.filePath } returns "/src/main/java/com/example/Complex.java"
        every { workspace.remote } returns root
        every { workspace.channel } returns null
        every { challenge.details } returns details
        every { challenge.solvedCoverage } returns 0.9
        parameters.workspace = workspace

        scenario("No Coverage specified")
        {
            AchievementUtil.haveXClassesWithYCoverageAndZLines(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["haveCoverage"] = "0.8"
        scenario("No line count specified")
        {
            AchievementUtil.haveXClassesWithYCoverageAndZLines(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["linesCount"] = "100"
        scenario("No class count specified")
        {
            AchievementUtil.haveXClassesWithYCoverageAndZLines(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["classesCount"] = "1"
        scenario("Classes covered")
        {
            AchievementUtil.haveXClassesWithYCoverageAndZLines(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        every { challenge.solvedCoverage } returns 0.7
        scenario("Classes not covered enough")
        {
            AchievementUtil.haveXClassesWithYCoverageAndZLines(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }
    }

    feature("haveXFailedTests") {
        additionalParameters.clear()
        every { run.result } returns Result.SUCCESS
        mockkStatic(JUnitUtil::class)
        every { JUnitUtil.getTestCount(any(), any()) } returns 0
        scenario("No Amount of failed tests specified, successful run")
        {
            AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { run.result } returns Result.FAILURE
        every { JUnitUtil.getTestFailCount(any(), any()) } returns 0
        scenario("No Amount of failed tests specified, unsuccessful run")
        {
            AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["failedTests"] = "1"
        scenario("Not enough failed tests")
        {
            AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { JUnitUtil.getTestFailCount(any(), any()) } returns 1
        scenario("Enough failed tests")
        {
            AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        additionalParameters["failedTests"] = "0"
        every { JUnitUtil.getTestCount(any(), any()) } returns 2
        scenario("Not all tests failed")
        {
            AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { JUnitUtil.getTestCount(any(), any()) } returns 1
        scenario("All tests failed")
        {
            AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        every{ JUnitUtil.getTestCount(any(), any()) } returns 0
        scenario("No tests exist")
        {
            AchievementUtil.haveXFailedTests(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }
    }

    feature("haveXProjectCoverage") {
        additionalParameters.clear()
        parameters.projectCoverage = 0.81
        scenario("No coverage specified")
        {
            AchievementUtil.haveXProjectCoverage(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["haveCoverage"] = "0.8"
        scenario("Enough coverage")
        {
            AchievementUtil.haveXProjectCoverage(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        parameters.projectCoverage = 0.79
        scenario("Not enough coverage")
        {
            AchievementUtil.haveXProjectCoverage(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }
    }

    feature("haveXProjectTests") {
        additionalParameters.clear()
        parameters.projectTests = 101
        scenario("No required amount of tests specified")
        {
            AchievementUtil.haveXProjectTests(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["haveTests"] = "100"
        scenario("Project has enough tests")
        {
            AchievementUtil.haveXProjectTests(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        parameters.projectTests = 99
        scenario("Project does not have enough tests")
        {
            AchievementUtil.haveXProjectTests(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }
    }

    feature("improveClassCoverageByX") {
        additionalParameters.clear()
        every { challenge.coverage } returns 0.0
        every { challenge.solvedCoverage } returns 0.0
        scenario("No required amount specified")
        {
            AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["haveCoverage"] = "0.1"
        scenario("No increased coverage")
        {
            AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { challenge.coverage } returns 0.7
        every { challenge.solvedCoverage } returns 0.75
        scenario("Not enough increase")
        {
            AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { challenge.solvedCoverage } returns 0.8
        scenario("Exactly enough coverage")
        {
            AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        every { challenge.solvedCoverage } returns 0.85
        scenario("More than enough coverage")
        {
            AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        additionalParameters["maxCoverage"] = "0.15"
        scenario("Upper boundary added, too much coverage increase")
        {
            AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["maxCoverage"] = "0.2"
        scenario("Upper boundary added, coverage increase in boundaries")
        {
            AchievementUtil.improveClassCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }
    }

    feature("improveProjectCoverageByX") {
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
        scenario("Wrong user")
        {
            AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

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
        scenario("Last run is null")
        {
            AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        val runEntry = mockkClass(Statistics.RunEntry::class)
        parameters.projectCoverage = 0.7
        every { runEntry.coverage } returns 0.6
        every { statistics.getLastRun("master") } returns runEntry
        scenario("No required amount specified")
        {
            AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["haveCoverage"] = "0.2"
        scenario("Not enough coverage increase")
        {
            AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["haveCoverage"] = "0.1"
        scenario("Exactly enough coverage increase")
        {
            AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        additionalParameters["haveCoverage"] = "0.05"
        scenario("More than enough coverage increase")
        {
            AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        additionalParameters["maxCoverage"] = "0.09"
        scenario("Upper boundary added, too much coverage increase")
        {
            AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["maxCoverage"] = "0.2"
        scenario("Upper boundary added, coverage increase in boundaries")
        {
            AchievementUtil.improveProjectCoverageByX(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }
    }

    feature("solveChallengeInXSeconds") {
        additionalParameters.clear()
        every { challenge.getSolved() } returns 100000000
        every { challenge.getCreated() } returns 10000000
        scenario("No timeDifference specified")
        {
            AchievementUtil.solveChallengeInXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["timeDifference"] = "3600"
        scenario("No minTimeDifference specified")
        {
            AchievementUtil.solveChallengeInXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["minTimeDifference"] = "3000"
        scenario("Time difference to large")
        {
            AchievementUtil.solveChallengeInXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { challenge.getCreated() } returns 99996400
        scenario("Time difference to small")
        {
            AchievementUtil.solveChallengeInXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        every { challenge.getCreated() } returns 96990000
        scenario("Time difference in boundaries")
        {
            AchievementUtil.solveChallengeInXSeconds(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }
    }

    feature("solveFirstBuildFail") {
        additionalParameters.clear()
        val buildProperty = mockkClass(org.gamekins.GameUserProperty::class)
        every { buildProperty.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf())
        scenario("No Build Challenge solved yet")
        {
            AchievementUtil.solveFirstBuildFail(files, parameters, run, buildProperty, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        val build = mockkClass(BuildChallenge::class)
        every { buildProperty.getCompletedChallenges(any()) } returns CopyOnWriteArrayList(listOf(build))
        scenario("Build challenge solved before")
        {
            AchievementUtil.solveFirstBuildFail(files, parameters, run, buildProperty, TaskListener.NULL,
                additionalParameters) shouldBe true
        }
    }

    feature("solveXChallenges") {
        additionalParameters.clear()
        scenario("No amount specified")
        {
            AchievementUtil.solveXChallenges(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["solveNumber"] = "1"
        scenario("Enough solved challenges")
        {
            AchievementUtil.solveXChallenges(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        additionalParameters["solveNumber"] = "2"
        scenario("Not enough solved challenges")
        {
            AchievementUtil.solveXChallenges(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }
    }

    feature("solveXAtOnce") {
        additionalParameters.clear()
        scenario("No amount specified")
        {
            AchievementUtil.solveXAtOnce(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        parameters.solved = 1
        scenario("No amount specified, one challenge solved")
        {
            AchievementUtil.solveXAtOnce(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }

        additionalParameters["solveNumber"] = "1"
        scenario("Exactly enough challenges solved")
        {
            AchievementUtil.solveXAtOnce(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe true
        }

        additionalParameters["solveNumber"] = "2"
        scenario("Not enough challenges solved")
        {
            AchievementUtil.solveXAtOnce(files, parameters, run, property, TaskListener.NULL,
                additionalParameters) shouldBe false
        }
    }
})