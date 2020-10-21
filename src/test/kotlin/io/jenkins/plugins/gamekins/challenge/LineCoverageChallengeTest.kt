package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.jenkins.plugins.gamekins.util.JacocoUtil
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class LineCoverageChallengeTest : AnnotationSpec() {

    private val className = "Challenge"
    private val path = FilePath(null, "/home/test/workspace")
    private val shortFilePath = "src/main/java/io/jenkins/plugins/gamekins/challenge/$className.kt"
    private val shortJacocoPath = "**/target/site/jacoco/"
    private val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    private lateinit var details : JacocoUtil.ClassDetails
    private lateinit var challenge : LineCoverageChallenge
    private val document = mockkClass(Document::class)
    private val element = mockkClass(Element::class)
    private val elements = Elements(listOf(element))
    private val coverage = 0.0
    private val run = mockkClass(Run::class)
    private val map = HashMap<String, String>()
    private val listener = TaskListener.NULL
    private val branch = "master"

    @BeforeEach
    fun init() {
        mockkStatic(JacocoUtil::class)
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        every { JacocoUtil.getLines(any()) }  returns elements
        every { element.attr("id") } returns "L5"
        every { element.attr("class") } returns "nc"
        every { element.text() } returns "toString();"
        details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath,
                TaskListener.NULL)
        challenge = LineCoverageChallenge(details, branch, path)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun getScore() {
        challenge.getScore() shouldBe 2
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns 0.9
        details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath,
                TaskListener.NULL)
        challenge = LineCoverageChallenge(details, branch, path)
        challenge.getScore() shouldBe 3
        challenge.toString() shouldBe "Write a test to cover more branches of line 5 in class $className in package " +
                "io.jenkins.plugins.gamekins.challenge (created for branch $branch)"
        every { element.attr("class") } returns "pc"
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath,
                TaskListener.NULL)
        challenge = LineCoverageChallenge(details, branch, path)
        challenge.getScore() shouldBe 3
        challenge.toString() shouldBe "Write a test to fully cover line 5 in class $className in package " +
                "io.jenkins.plugins.gamekins.challenge (created for branch $branch)"
        challenge.getName() shouldBe "LineCoverageChallenge"
    }

    @Test
    fun isSolvable() {
        challenge.isSolvable(map, run, listener, path) shouldBe true
        map["branch"] = branch
        challenge.isSolvable(map, run, listener, path) shouldBe true
        every { document.select("span.pc") } returns Elements()
        every { document.select("span.nc") } returns Elements()
        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns true
        every { JacocoUtil.calculateCurrentFilePath(any(), any(), any()) } returns pathMock
        challenge.isSolvable(map, run, listener, pathMock) shouldBe false
        every { document.select("span.nc") } returns elements
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
        every { document.select("span.pc") } returns Elements()
        every { document.select("span.fc") } returns Elements()
        challenge.isSolved(map, run, listener, path) shouldBe false
        every { document.select("span.fc") } returns elements
        challenge.isSolved(map, run, listener, path) shouldBe true
        every { element.attr("class") } returns "pc"
        challenge = LineCoverageChallenge(details, branch, path)
        challenge.isSolved(map, run, listener, path) shouldBe true
    }
}