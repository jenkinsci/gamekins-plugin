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

package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Job
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.util.GitUtil
import org.gamekins.util.JacocoUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.*
import jenkins.branch.MultiBranchProject
import org.gamekins.file.TestFileDetails
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.JUnitUtil

class TestChallengeTest : AnnotationSpec() {

    private lateinit var challenge : TestChallenge
    private val user = mockkClass(hudson.model.User::class)
    private val run = mockkClass(Run::class)
    private val parameters = Parameters()
    private val listener = TaskListener.NULL
    private val path = FilePath(null, "")
    private val testCount = 10

    @BeforeEach
    fun init() {
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

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun isSolvable() {
        val job = mockkClass(Job::class)
        val project = mockkClass(WorkflowMultiBranchProject::class)
        val workJob = mockkClass(WorkflowJob::class)
        val multiProject = mockkClass(MultiBranchProject::class)


        every { run.parent } returns job
        every { job.parent } returns project
        every { project.items } returns listOf(workJob)
        every { workJob.name } returns "master"
        challenge.isSolvable(parameters, run, listener) shouldBe true

        every { workJob.name } returns "branch"
        challenge.isSolvable(parameters, run, listener) shouldBe false

        every { job.parent } returns multiProject
        challenge.isSolvable(parameters, run, listener) shouldBe true
    }

    @Test
    fun isSolved() {
        mockkStatic(JacocoUtil::class)
        mockkStatic(JUnitUtil::class)
        mockkStatic(GitUtil::class)
        mockkStatic(User::class)
        parameters.branch = "master"

        every { JUnitUtil.getTestCount(path, run) } returns testCount
        challenge.isSolved(parameters, run, listener) shouldBe false

        every { JUnitUtil.getTestCount(path, run) } returns (testCount + 1)
        every { GitUtil.getLastChangedTestsOfUser(any(), any(), any(), any(), any(), any()) } returns listOf()
        every { User.getAll() } returns listOf()
        challenge.isSolved(parameters, run, listener) shouldBe false

        //every { GitUtil.getLastChangedTestsOfUser(GitUtil.DEFAULT_SEARCH_COMMIT_COUNT, "", map, listener, GitUtil.GameUser(user), arrayListOf(), path) } returns listOf(
        //    mockkClass(TestFileDetails::class))
        every { GitUtil.getLastChangedTestsOfUser(any(), any(), any(), any(), any(), any()) } returns listOf(
            mockkClass(TestFileDetails::class))
        challenge.isSolved(parameters, run, listener) shouldBe  true
        challenge.getSolved() shouldNotBe 0
        challenge.getScore() shouldBe 1
    }

    @Test
    fun printToXML() {
        challenge.printToXML("", "") shouldBe
                "<TestChallenge created=\"${challenge.getCreated()}\" solved=\"${challenge.getSolved()}\" " +
                "tests=\"$testCount\" testsAtSolved=\"0\"/>"
        challenge.printToXML("", "    ") shouldStartWith "    <"
        challenge.printToXML("test", "") shouldBe
                "<TestChallenge created=\"${challenge.getCreated()}\" solved=\"0\" tests=\"$testCount\" " +
                "testsAtSolved=\"0\" reason=\"test\"/>"
        challenge.toString() shouldBe "Write a new test in branch master"
    }
}