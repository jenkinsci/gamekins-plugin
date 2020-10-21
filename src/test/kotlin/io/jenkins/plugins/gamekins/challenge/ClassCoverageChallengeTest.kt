package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.gamekins.util.JacocoUtil
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.jsoup.nodes.Document

class ClassCoverageChallengeTest : AnnotationSpec() {

    private val className = "Challenge"
    private val path = FilePath(null, "/home/test/workspace")
    private val shortFilePath = "src/main/java/io/jenkins/plugins/gamekins/challenge/$className.kt"
    private val shortJacocoPath = "**/target/site/jacoco/"
    private val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    private lateinit var details : JacocoUtil.ClassDetails
    private lateinit var challenge : ClassCoverageChallenge
    private val coverage = 0.0
    private val run = mockkClass(Run::class)
    private val map = HashMap<String, String>()
    private val listener = TaskListener.NULL
    private val branch = "master"

    @BeforeEach
    fun init() {
        mockkStatic(JacocoUtil::class)
        val document = mockkClass(Document::class)
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath,
                TaskListener.NULL)
        challenge = ClassCoverageChallenge(details, branch, path)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun getScore() {
        challenge.getScore() shouldBe 1
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns 0.9
        details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath,
                TaskListener.NULL)
        challenge = ClassCoverageChallenge(details, branch, path)
        challenge.getScore() shouldBe 2
        challenge.toString() shouldBe "Write a test to cover more lines in class $className in package " +
                "io.jenkins.plugins.gamekins.challenge (created for branch $branch)"
    }

    @Test
    fun isSolvable() {
        challenge.isSolvable(map, run, listener, path) shouldBe true
        map["branch"] = branch
        challenge.isSolvable(map, run, listener, path) shouldBe true
        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns true
        every { JacocoUtil.calculateCurrentFilePath(any(), any(), any()) } returns pathMock
        challenge.isSolvable(map, run, listener, pathMock) shouldBe false
        every { JacocoUtil.calculateCoveredLines(any(), "pc") } returns 1
        challenge.isSolvable(map, run, listener, pathMock) shouldBe true
        every { JacocoUtil.calculateCoveredLines(any(), "pc") } returns 0
        every { JacocoUtil.calculateCoveredLines(any(), "nc") } returns 1
        challenge.isSolvable(map, run, listener, pathMock) shouldBe true
    }

    @Test
    fun isSolved() {
        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns false
        every { pathMock.remote } returns path.remote
        every { JacocoUtil.getJacocoFileInMultiBranchProject(any(), any(), any(), any()) } returns pathMock
        challenge.isSolved(map, run, listener, path) shouldBe false
        every { pathMock.exists() } returns true
        challenge.isSolved(map, run, listener, path) shouldBe false
        every { JacocoUtil.calculateCoveredLines(any(), "fc") } returns 1
        challenge.isSolved(map, run, listener, path) shouldBe true
    }
}