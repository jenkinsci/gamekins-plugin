package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import io.jenkins.plugins.gamekins.util.GitUtil
import io.jenkins.plugins.gamekins.util.JacocoUtil
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.should
import io.kotest.matchers.types.beOfType
import io.mockk.*
import org.eclipse.jgit.revwalk.RevCommit
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import kotlin.collections.HashMap
import kotlin.random.Random

class ChallengeFactoryTest : AnnotationSpec() {

    private val user = mockkClass(User::class)
    private val run = mockkClass(Run::class)
    private val map = HashMap<String, String>()
    private val branch = "master"
    private val listener = TaskListener.NULL
    private val className = "Challenge"
    private val path = mockkClass(FilePath::class)
    private val shortFilePath = "src/main/java/io/jenkins/plugins/gamekins/challenge/$className.kt"
    private val shortJacocoPath = "**/target/site/jacoco/"
    private val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    private val coverage = 0.0
    private val testCount = 10
    private lateinit var details : JacocoUtil.ClassDetails

    @BeforeEach
    fun init() {
        map["branch"] = branch
        mockkStatic(JacocoUtil::class)
        val document = mockkClass(Document::class)
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        every { JacocoUtil.getTestCount(any(), any()) } returns testCount
        val commit = mockkClass(RevCommit::class)
        every { commit.name } returns "ef97erb"
        every { path.act(ofType(GitUtil.HeadCommitCallable::class)) } returns commit
        every { path.act(ofType(JacocoUtil.FilesOfAllSubDirectoriesCallable::class)) } returns arrayListOf()
        every { path.remote } returns "/home/test/workspace"
        every { path.channel } returns null
        details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath, listener)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun generateClassCoverageChallenge() {
        mockkObject(Random)
        every { Random.nextInt(any()) } returns 0
        every { Random.nextDouble() } returns 0.1
        every { JacocoUtil.calculateCoveredLines(any(), "nc") } returns 10
        ChallengeFactory.generateChallenge(user, map, listener, arrayListOf(details), path) should
                beOfType(ClassCoverageChallenge::class)
    }

    @Test
    fun generateDummyChallenge() {
        mockkObject(Random)
        every { Random.nextDouble() } returns 0.1
        ChallengeFactory.generateChallenge(user, map, listener, arrayListOf(details), path) should
                beOfType(DummyChallenge::class)
    }

    @Test
    fun generateLineCoverageChallenge() {
        mockkObject(Random)
        every { Random.nextInt(any()) } returns 2
        every { Random.nextDouble() } returns 0.1
        every { JacocoUtil.calculateCoveredLines(any(), "pc") } returns 10
        val element = mockkClass(Element::class)
        val elements = Elements(listOf(element))
        every { JacocoUtil.getLines(any()) }  returns elements
        every { element.attr("id") } returns "L5"
        every { element.attr("class") } returns "nc"
        every { element.text() } returns "toString();"
        ChallengeFactory.generateChallenge(user, map, listener, arrayListOf(details), path) should
                beOfType(LineCoverageChallenge::class)
    }

    @Test
    fun generateMethodCoverageChallenge() {
        mockkObject(Random)
        every { Random.nextInt(any()) } returns 1
        every { Random.nextDouble() } returns 0.1
        every { JacocoUtil.calculateCoveredLines(any(), "nc") } returns 10
        val method = JacocoUtil.CoverageMethod("toString", 10, 10)
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method)
        ChallengeFactory.generateChallenge(user, map, listener, arrayListOf(details), path) should
                beOfType(MethodCoverageChallenge::class)
    }

    @Test
    fun generateTestChallenge() {
        mockkObject(Random)
        every { Random.nextDouble() } returns 0.95
        ChallengeFactory.generateChallenge(user, map, listener, arrayListOf(details), path) should
                beOfType(TestChallenge::class)
    }
}
