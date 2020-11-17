package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.TaskListener
import io.jenkins.plugins.gamekins.util.JacocoUtil
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.*
import org.jsoup.nodes.Document

class CoverageChallengeTest : AnnotationSpec() {

    private val className = "Challenge"
    private val path = FilePath(null, "/home/test/workspace")
    private val shortFilePath = "src/main/java/io/jenkins/plugins/gamekins/challenge/$className.kt"
    private val shortJacocoPath = "**/target/site/jacoco/"
    private val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    private lateinit var details : JacocoUtil.ClassDetails
    private lateinit var challenge : ClassCoverageChallenge
    private val coverage = 0.0

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun printToXML() {
        mockkStatic(JacocoUtil::class)
        val document = mockkClass(Document::class)
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath, hashMapOf(),
                TaskListener.NULL)
        challenge = ClassCoverageChallenge(details, "master", path)

        challenge.printToXML("", "") shouldBe
                "<ClassCoverageChallenge created=\"${challenge.getCreated()}\" solved=\"${challenge.getSolved()}\" " +
                "class=\"$className\" coverage=\"$coverage\" coverageAtSolved=\"0.0\"/>"
        challenge.printToXML("", "    ") shouldStartWith "    <"
        challenge.printToXML("test", "") shouldBe
                "<ClassCoverageChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"$className\" " +
                "coverage=\"$coverage\" coverageAtSolved=\"0.0\" reason=\"test\"/>"
    }
}