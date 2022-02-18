package org.gamekins.util

import hudson.FilePath
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.file.SourceFileDetails
import org.gamekins.test.TestUtils
import java.io.File

class SmellUtilTest : AnnotationSpec() {

    private lateinit var root : String
    private lateinit var path : FilePath
    private val shortJacocoPath = "**/target/site/jacoco/"
    private val shortJacocoCSVPath = "**/target/site/jacoco/jacoco.csv"
    private val mocoJSONPath = "**/target/site/moco/mutation/"

    @BeforeAll
    fun initAll() {
        //Needed because of bug in mockk library which does not release mocked objects
        mockkStatic(SmellUtil::class)
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
        path = FilePath(null, root)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
        File(root).deleteRecursively()
    }

    @Test
    fun getSmellsOfFile() {
        val parameters = Constants.Parameters()
        parameters.jacocoResultsPath = shortJacocoPath
        parameters.jacocoCSVPath = shortJacocoCSVPath
        parameters.mocoJSONPath = mocoJSONPath
        parameters.workspace = path
        var file = SourceFileDetails(parameters, "/src/main/java/com/example/Calculator.java")

        SmellUtil.getSmellsOfFile(file) shouldHaveSize 0

        file = SourceFileDetails(parameters, "/src/main/java/com/example/Complex.java")
        SmellUtil.getSmellsOfFile(file) shouldHaveSize 9
    }

    @Test
    fun getLineContent() {
        val parameters = Constants.Parameters()
        parameters.jacocoResultsPath = shortJacocoPath
        parameters.jacocoCSVPath = shortJacocoCSVPath
        parameters.mocoJSONPath = mocoJSONPath
        parameters.workspace = path
        val file = SourceFileDetails(parameters, "/src/main/java/com/example/Calculator.java")
        val content = "package com.example;\n" +
                "\n" +
                "public class Calculator {\n" +
                "\n" +
                "  public int evaluate(final String pExpression) {\n" +
                "    int sum = 0;\n" +
                "    for (String summand : pExpression.split(\"\\\\+\")) {\n" +
                "      sum += Integer.parseInt(summand);\n" +
                "    }\n" +
                "    return sum;\n" +
                "  }\n" +
                "}\n"

        SmellUtil.getLineContent(file, null, null) shouldBe content

        SmellUtil.getLineContent(file, 1, null) shouldBe content

        SmellUtil.getLineContent(file, null, 2) shouldBe content

        SmellUtil.getLineContent(file, 1, 1) shouldBe "package com.example;"

        SmellUtil.getLineContent(file, 1, 2) shouldBe "package com.example;\n"

        SmellUtil.getLineContent(file, 0, 1) shouldBe "package com.example;"

        SmellUtil.getLineContent(file, 2, 3) shouldBe "\npublic class Calculator {"
    }
}