/*
 * Copyright 2023 Gamekins contributors
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

package org.gamekins.util

import hudson.FilePath
import hudson.model.Job
import hudson.model.Run
import org.gamekins.test.TestUtils
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.file.SourceFileDetails
import org.gamekins.util.Constants.Parameters
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.jsoup.select.Elements
import java.io.File

class JacocoUtilTest : FeatureSpec({

    lateinit var root : String
    lateinit var path : FilePath
    lateinit var jacocoSourceFile : FilePath
    lateinit var jacocoCSVFile : FilePath
    lateinit var jacocoMethodFile : FilePath
    lateinit var sourceFile : FilePath
    lateinit var parameters : Parameters

    beforeSpec {
        //Needed because of bug in mockk library which does not release mocked objects
        mockkStatic(JacocoUtil::class)
        mockkStatic(JUnitUtil::class)
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory!!.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
        path = FilePath(null, root)
        jacocoSourceFile = FilePath(null, path.remote + "/target/site/jacoco/com.example/Complex.java.html")
        jacocoCSVFile = FilePath(null, path.remote + "/target/site/jacoco/jacoco.csv")
        sourceFile = FilePath(null, path.remote + "/src/main/java/com/example/Complex.java")
        jacocoMethodFile = FilePath(null, path.remote + "/target/site/jacoco/com.example/Complex.html")
        parameters = Parameters()
        parameters.workspace = path

    }

    afterSpec {
        unmockkAll()
        File(root).deleteRecursively()
    }

    feature("calculateCoveredLines") {
        scenario("Fully covered")
        {
            JacocoUtil.calculateCoveredLines(JacocoUtil.generateDocument(jacocoSourceFile), "fc") shouldBe 5
        }

        scenario("Partially covered")
        {
            JacocoUtil.calculateCoveredLines(JacocoUtil.generateDocument(jacocoSourceFile), "pc") shouldBe 1
        }

        scenario("Not covered")
        {
            JacocoUtil.calculateCoveredLines(JacocoUtil.generateDocument(jacocoSourceFile), "nc") shouldBe 54
        }
    }

    feature("calculateCurrentFilePath") {
        JacocoUtil.calculateCurrentFilePath(path, File(path.remote)).toURI().path shouldBe path.toURI().path

        JacocoUtil.calculateCurrentFilePath(path, File(jacocoSourceFile.remote), path.remote).toURI().path shouldBe jacocoSourceFile.toURI().path
    }

    feature("chooseRandomLine") {
        val classDetails = mockkClass(SourceFileDetails::class)
        every { classDetails.jacocoSourceFile } returns File(jacocoSourceFile.remote)
        every { classDetails.parameters } returns parameters
        scenario("Successful Action")
        {
            JacocoUtil.chooseRandomLine(classDetails, path) shouldNotBe null
        }

        every { JacocoUtil.getLines(any()) } returns Elements()
        scenario("File is empty")
        {
            JacocoUtil.chooseRandomLine(classDetails, path) shouldBe null
        }
    }

    feature("chooseRandomMethod") {
        mockkStatic(JacocoUtil::class)
        val classDetails = mockkClass(SourceFileDetails::class)
        every { classDetails.jacocoMethodFile } returns File(jacocoMethodFile.remote)
        every { classDetails.parameters } returns parameters
        scenario("Successful Action")
        {
            JacocoUtil.chooseRandomMethod(classDetails, path) shouldNotBe null
        }

        every { JacocoUtil.getNotFullyCoveredMethodEntries(any()) } returns arrayListOf()
        scenario("All Methods fully covered")
        {
            JacocoUtil.chooseRandomMethod(classDetails, path) shouldBe null
        }
    }

    feature("computePackageName") {
        JacocoUtil.computePackageName(sourceFile.remote.removePrefix(path.remote)) shouldBe "com.example"
    }

    feature("generateDocument") {
        JacocoUtil.generateDocument(FilePath(null, path.remote + "/target/site/jacoco/index.html")) shouldNotBe null
    }

    feature("getCoverageInPercentageFromJacoco") {
        scenario("Example class Complex")
        {
            JacocoUtil.getCoverageInPercentageFromJacoco("Complex", jacocoCSVFile) shouldBe 0.04177545691906005
        }

        scenario("Non existing class")
        {
            JacocoUtil.getCoverageInPercentageFromJacoco("NotExisting", jacocoCSVFile) shouldBe 0.0
        }

        scenario("Example class NestedLoop")
        {
            JacocoUtil.getCoverageInPercentageFromJacoco("NestedLoop", jacocoCSVFile) shouldBe 0.6923076923076923
        }

        scenario("Example class Calculator")
        {
            JacocoUtil.getCoverageInPercentageFromJacoco("Calculator", jacocoCSVFile) shouldBe 1.0
        }

        scenario("Example class Rational")
        {
            JacocoUtil.getCoverageInPercentageFromJacoco("Rational", jacocoCSVFile) shouldBe 0.0
        }
    }

    feature("getFilesInAllSubdirectories") {
        scenario("Search for xml-files starting with TEST-")
        {
            JacocoUtil.getFilesInAllSubDirectories(path, "TEST-.+\\.xml") shouldHaveSize 4
        }

        scenario("Search for jacoco.csv")
        {
            JacocoUtil.getFilesInAllSubDirectories(path, "jacoco.csv") shouldHaveSize  1
        }

        scenario("Search for index.html")
        {
            JacocoUtil.getFilesInAllSubDirectories(path, "index.html") shouldHaveSize 2
        }
    }

    feature("getJacocoFileInMultiBranchProject") {
        val run = mockkClass(Run::class)
        val job = mockkClass(Job::class)
        val project = mockkClass(WorkflowMultiBranchProject::class)
        every { run.parent } returns job
        every { job.parent } returns project
        parameters.branch = "testing"
        parameters.projectName = "test-project"
        val jacocoFile = FilePath(null, "/home/test/test-project_testing/file.html")
        val jacocoFileMaster = FilePath(null, "/home/test/test-project_master/file.html")

        scenario("Same Branch")
        {
            JacocoUtil.getJacocoFileInMultiBranchProject(run, parameters, jacocoFile, "testing") shouldBe jacocoFile
        }

        scenario("Different Branch")
        {
            JacocoUtil.getJacocoFileInMultiBranchProject(run, parameters, jacocoFileMaster, "master") shouldBe jacocoFile
        }

        every { job.parent } returns mockkClass(jenkins.branch.MultiBranchProject::class)
        scenario("MultiBranchProject")
        {
            JacocoUtil.getJacocoFileInMultiBranchProject(run, parameters, jacocoFile, "testing") shouldBe jacocoFile
        }
    }

    feature("getLines") {
        mockkStatic(JacocoUtil::class)
        scenario("jacocoSourceFile")
        {
            JacocoUtil.getLines(jacocoSourceFile) shouldHaveSize 54
        }

        val rationalPath = FilePath(null, path.remote + "/target/site/jacoco/com.example/Rational.java.html")
        scenario("Rational.java.html")
        {
            JacocoUtil.getLines(rationalPath) shouldHaveSize 91
        }
    }

    feature("getMethodEntries") {
        JacocoUtil.getMethodEntries(jacocoMethodFile) shouldHaveSize 23
    }

    feature("getNotFullyCoveredMethodEntries") {
        JacocoUtil.getNotFullyCoveredMethodEntries(jacocoMethodFile) shouldHaveSize 21
    }

    feature("getProjectCoverage") {
        JacocoUtil.getProjectCoverage(path, "jacoco.csv") shouldBe 0.12087912087912088
    }

    feature("isGetterOrSetter") {
        val rationalPath = FilePath(null, path.remote + "/target/site/jacoco/com.example/Rational.java.html")

        scenario("\"    return num;\" in Rational.java.html, is Getter")
        {
            JacocoUtil.isGetterOrSetter(rationalPath.readToString().split("\n"),
                "    return num;") shouldBe true
        }

        scenario("\"    return den.equals(B_ONE);\" in Rational.java.html, is neither Getter nor Setter")
        {
            JacocoUtil.isGetterOrSetter(rationalPath.readToString().split("\n"),
                "    return den.equals(B_ONE);") shouldBe false
        }

        scenario("\"      BigInteger num = decimal.toBigInteger();\" in Rational.java.html, is neither Getter nor Setter")
        {
            JacocoUtil.isGetterOrSetter(rationalPath.readToString().split("\n"),
                "      BigInteger num = decimal.toBigInteger();") shouldBe false
        }

        scenario("Line does not exist")
        {
            JacocoUtil.isGetterOrSetter(rationalPath.readToString().split("\n"),
                "not found") shouldBe false
        }
    }

    feature("getLinesInRange") {
        scenario("4 Lines around Line 41")
        {
            JacocoUtil.getLinesInRange(jacocoSourceFile, 41, 4) shouldBe Pair(
                    "    double ns = a * a + b * b;" + System.lineSeparator() +
                    "    double dArg = d / 2;" + System.lineSeparator() +
                    "    double cArg = c * arg();" + System.lineSeparator() +
                    "    double dDenom = Math.pow(Math.E, d * arg());" + System.lineSeparator() +
                    System.lineSeparator(), "    double cArg = c * arg();")
        }

        scenario("4 Lines around first occurrence of \"dDenom\"")
        {
            JacocoUtil.getLinesInRange(jacocoSourceFile, "dDenom", 4) shouldBe Pair(
                    "    double dArg = d / 2;" + System.lineSeparator() +
                    "    double cArg = c * arg();" + System.lineSeparator() +
                    "    double dDenom = Math.pow(Math.E, d * arg());" + System.lineSeparator() +
                    System.lineSeparator() +
                    "    double newReal =" + System.lineSeparator(), "")
        }

        scenario("Invalid target")
        {
            JacocoUtil.getLinesInRange(jacocoSourceFile, 5.0, 4) shouldBe Pair("", "")
        }
    }
})