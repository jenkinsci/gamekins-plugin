/*
 * Copyright 2023 Gamekins contributors
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

package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Job
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import io.kotest.core.spec.style.FeatureSpec
import org.gamekins.util.GitUtil
import org.gamekins.util.JacocoUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.*
import jenkins.branch.MultiBranchProject
import org.gamekins.file.TestFileDetails
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.JUnitUtil

class TestChallengeTest : FeatureSpec({

    lateinit var challenge : TestChallenge
    val user = mockkClass(hudson.model.User::class)
    val run = mockkClass(Run::class)
    val parameters = Parameters()
    val listener = TaskListener.NULL
    val path = FilePath(null, "")
    val testCount = 10

    beforeContainer {
        parameters.branch = "master"
        every { user.id } returns ""
        every { user.fullName } returns ""
        every { user.getProperty(hudson.tasks.Mailer.UserProperty::class.java) } returns null
        every { user.getProperty(org.gamekins.GameUserProperty::class.java) } returns null
        val data = mockkClass(Challenge.ChallengeGenerationData::class)
        every { data.headCommitHash } returns ""
        every { data.testCount } returns testCount
        every { data.user } returns user
        every { data.parameters } returns parameters
        challenge = TestChallenge(data)
    }

    afterSpec {
        unmockkAll()
    }

    feature("isSolvable") {
        val job = mockkClass(Job::class)
        val project = mockkClass(WorkflowMultiBranchProject::class)
        val workJob = mockkClass(WorkflowJob::class)
        val multiProject = mockkClass(MultiBranchProject::class)


        every { run.parent } returns job
        every { job.parent } returns project
        every { project.items } returns listOf(workJob)
        every { workJob.name } returns "master"
        scenario("Branch still exists")
        {
            challenge.isSolvable(parameters, run, listener) shouldBe true
        }

        every { workJob.name } returns "branch"
        scenario("Branch does not exist")
        {
            challenge.isSolvable(parameters, run, listener) shouldBe false
        }

        every { job.parent } returns multiProject
        scenario("Project is not WorkflowMultiBranchProject")
        {
            challenge.isSolvable(parameters, run, listener) shouldBe true
        }
    }

    feature("isSolved") {
        mockkStatic(JacocoUtil::class)
        mockkStatic(JUnitUtil::class)
        mockkStatic(GitUtil::class)
        mockkStatic(User::class)
        parameters.branch = "master"

        every { JUnitUtil.getTestCount(path, run) } returns testCount
        scenario("No new tests")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false
        }

        every { JUnitUtil.getTestCount(path, run) } returns (testCount + 1)
        every { GitUtil.getLastChangedTestsOfUser(any(), any(), any(), any(), any()) } returns listOf()
        every { User.getAll() } returns listOf()
        scenario("User has not changed/added any tests")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false
        }

        every { GitUtil.getLastChangedTestsOfUser(any(), any(), any(), any(), any()) } returns listOf(
            mockkClass(TestFileDetails::class))
        scenario("Challenge solved")
        {
            challenge.isSolved(parameters, run, listener) shouldBe  true
            challenge.getSolved() shouldNotBe 0
            challenge.getScore() shouldBe 1
        }
    }

    feature("printToXML") {

        scenario("No Reason, no Indentation")
        {
            challenge.printToXML("", "") shouldBe
                    "<TestChallenge created=\"${challenge.getCreated()}\" solved=\"${challenge.getSolved()}\" " +
                    "tests=\"$testCount\" testsAtSolved=\"0\"/>"
        }

        scenario("No Reason, with Indentation")
        {
            challenge.printToXML("", "    ") shouldStartWith "    <"
        }

        scenario("With Reason, no Indentation")
        {
            challenge.printToXML("test", "") shouldBe
                    "<TestChallenge created=\"${challenge.getCreated()}\" solved=\"0\" tests=\"$testCount\" " +
                    "testsAtSolved=\"0\" reason=\"test\"/>"
        }
    }

    feature("toString")
    {
        challenge.toString() shouldBe "Write a new test in branch master"
    }
})
