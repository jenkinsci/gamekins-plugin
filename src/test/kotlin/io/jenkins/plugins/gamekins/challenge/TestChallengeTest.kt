package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Job
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import io.jenkins.plugins.gamekins.util.GitUtil
import io.jenkins.plugins.gamekins.util.JacocoUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.*
import jenkins.branch.MultiBranchProject

class TestChallengeTest : AnnotationSpec() {

    private lateinit var challenge : TestChallenge
    private val user = mockkClass(hudson.model.User::class)
    private val run = mockkClass(Run::class)
    private val map = HashMap<String, String>()
    private val listener = TaskListener.NULL
    private val path = FilePath(null, "")
    private val testCount = 10

    @BeforeEach
    fun init() {
        challenge = TestChallenge("", testCount, user, "master")
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
        challenge.isSolvable(map, run, listener, path) shouldBe true

        every { workJob.name } returns "branch"
        challenge.isSolvable(map, run, listener, path) shouldBe false

        every { job.parent } returns multiProject
        challenge.isSolvable(map, run, listener, path) shouldBe true
    }

    @Test
    fun isSolved() {
        mockkStatic(JacocoUtil::class)
        mockkStatic(GitUtil::class)
        mockkStatic(User::class)
        map["branch"] = "master"

        every { JacocoUtil.getTestCount(path, run) } returns testCount
        challenge.isSolved(map, run, listener, path) shouldBe false

        every { JacocoUtil.getTestCount(path, run) } returns (testCount + 1)
        every { GitUtil.getLastChangedTestFilesOfUser(path, user, 0, "", listOf()) } returns setOf()
        every { User.getAll() } returns listOf()
        challenge.isSolved(map, run, listener, path) shouldBe false

        every { GitUtil.getLastChangedTestFilesOfUser(path, user, 0, "", listOf()) } returns setOf("not empty")
        challenge.isSolved(map, run, listener, path) shouldBe  true
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