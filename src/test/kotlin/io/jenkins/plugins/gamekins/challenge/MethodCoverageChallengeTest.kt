package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.gamekins.util.JacocoUtil
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.jsoup.nodes.Document

class MethodCoverageChallengeTest : AnnotationSpec() {

    private val className = "Challenge"
    private val path = FilePath(null, "/home/test/workspace")
    private val shortFilePath = "src/main/java/io/jenkins/plugins/gamekins/challenge/$className.kt"
    private val shortJacocoPath = "**/target/site/jacoco/"
    private val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    private lateinit var details : JacocoUtil.ClassDetails
    private lateinit var challenge : MethodCoverageChallenge
    private lateinit var method : JacocoUtil.CoverageMethod
    private val coverage = 0.0
    private val run = mockkClass(Run::class)
    private val map = HashMap<String, String>()
    private val listener = TaskListener.NULL
    private val branch = "master"
    private val methodName = "toString"

    @BeforeEach
    fun init() {
        mockkStatic(JacocoUtil::class)
        val document = mockkClass(Document::class)
        method = JacocoUtil.CoverageMethod(methodName, 10, 10)
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        every { JacocoUtil.getNotFullyCoveredMethodEntries(any()) } returns arrayListOf(method)
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf()
        details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath,
                TaskListener.NULL)
        challenge = MethodCoverageChallenge(details, branch, path, method)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun getScore() {
        challenge.getScore() shouldBe 2
        method = JacocoUtil.CoverageMethod(methodName, 10, 1)
        every { JacocoUtil.getNotFullyCoveredMethodEntries(any()) } returns arrayListOf(method)
        challenge = MethodCoverageChallenge(details, branch, path, method)
        challenge.getScore() shouldBe 3
        challenge.toString() shouldBe "Write a test to cover more lines of method $methodName in class " +
                "$className in package io.jenkins.plugins.gamekins.challenge (created for branch $branch)"
        challenge.getName() shouldBe "MethodCoverageChallenge"
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
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method)
        challenge.isSolvable(map, run, listener, pathMock) shouldBe true
        method = JacocoUtil.CoverageMethod(methodName, 10, 0)
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method)
        challenge.isSolvable(map, run, listener, pathMock) shouldBe false
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
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method)
        challenge.isSolved(map, run, listener, path) shouldBe false
        method = JacocoUtil.CoverageMethod(methodName, 10, 0)
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method)
        challenge.isSolved(map, run, listener, path) shouldBe true
    }
}