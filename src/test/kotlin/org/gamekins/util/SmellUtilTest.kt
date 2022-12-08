package org.gamekins.util

import hudson.FilePath
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.file.SourceFileDetails
import org.gamekins.test.TestUtils
import java.io.File

class SmellUtilTest : FeatureSpec({

    lateinit var root : String
    lateinit var path : FilePath
    val shortJacocoPath = "**/target/site/jacoco/"
    val shortJacocoCSVPath = "**/target/site/jacoco/jacoco.csv"
    val mocoJSONPath = "**/target/site/moco/mutation/"

    beforeSpec {
        //Needed because of bug in mockk library which does not release mocked objects
        mockkStatic(SmellUtil::class)
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory!!.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
        path = FilePath(null, root)
    }

    afterSpec {
        unmockkAll()
        File(root).deleteRecursively()
    }

    feature("getSmellsOfFile") {
        val parameters = Constants.Parameters()
        parameters.jacocoResultsPath = shortJacocoPath
        parameters.jacocoCSVPath = shortJacocoCSVPath
        parameters.mocoJSONPath = mocoJSONPath
        parameters.workspace = path
        var file = SourceFileDetails(parameters, "/src/main/java/com/example/Calculator.java")

        scenario("Smells of Calculator.java")
        {
            SmellUtil.getSmellsOfFile(file) shouldHaveSize 0
        }

        file = SourceFileDetails(parameters, "/src/main/java/com/example/Complex.java")
        scenario("Smells of Complex.java")
        {
            SmellUtil.getSmellsOfFile(file) shouldHaveSize 9
        }
    }

    feature("getLineContent") {
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

        scenario("No startLine and endLine given")
        {
            SmellUtil.getLineContent(file, null, null) shouldBe content
        }

        scenario("No endLine given")
        {
            SmellUtil.getLineContent(file, 1, null) shouldBe content
        }

        scenario("No startLine given")
        {
            SmellUtil.getLineContent(file, null, 2) shouldBe content
        }

        scenario("Get first line")
        {
            SmellUtil.getLineContent(file, 1, 1) shouldBe "package com.example;"
        }

        scenario("get first and second line")
        {
            SmellUtil.getLineContent(file, 1, 2) shouldBe "package com.example;\n"
        }

        scenario("Get first line by starting from line 0, but lines are indexed from 1")
        {
            SmellUtil.getLineContent(file, 0, 1) shouldBe "package com.example;"
        }

        scenario("Get second and third line")
        {
            SmellUtil.getLineContent(file, 2, 3) shouldBe "\npublic class Calculator {"
        }
    }
})