package io.jenkins.plugins.gamekins.util

import hudson.FilePath
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith

class PublisherUtilTest : AnnotationSpec() {

    private lateinit var root : String
    private lateinit var path : FilePath
    val jacocoResultsPath = "**/target/site/jacoco/"
    val jacocoCSVPath = "**/target/site/jacoco/jacoco.csv"

    @BeforeAll
    fun initAll() {
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
        path = FilePath(null, root)
    }

    @Test
    fun doCheckJacocoCSVPath() {
        PublisherUtil.doCheckJacocoCSVPath(path, jacocoCSVPath) shouldBe true
        PublisherUtil.doCheckJacocoCSVPath(FilePath(null, path.remote + "/src"), jacocoCSVPath) shouldBe false
    }

    @Test
    fun doCheckJacocoResultsPath() {
        PublisherUtil.doCheckJacocoResultsPath(path, jacocoResultsPath) shouldBe true
        PublisherUtil.doCheckJacocoResultsPath(FilePath(null, path.remote + "/src"), jacocoResultsPath) shouldBe false
    }
}