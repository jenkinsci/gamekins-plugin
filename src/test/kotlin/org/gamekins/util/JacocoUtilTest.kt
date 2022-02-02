/*
 * Copyright 2022 Gamekins contributors
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
import hudson.model.TaskListener
import org.gamekins.test.TestUtils
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.eclipse.jgit.revwalk.RevCommit
import org.gamekins.file.SourceFileDetails
import org.gamekins.util.Constants.Parameters
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.File

class JacocoUtilTest : AnnotationSpec() {

    private lateinit var root : String
    private lateinit var path : FilePath
    private lateinit var jacocoSourceFile : FilePath
    private lateinit var jacocoCSVFile : FilePath
    private lateinit var jacocoMethodFile : FilePath
    private lateinit var sourceFile : FilePath
    private lateinit var parameters : Parameters

    @BeforeAll
    fun initAll() {
        //Needed because of bug in mockk library which does not release mocked objects
        mockkStatic(JacocoUtil::class)
        mockkStatic(JUnitUtil::class)
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory.file.removeSuffix(".zip")
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

    @AfterAll
    fun cleanUp() {
        unmockkAll()
        File(root).deleteRecursively()
    }

    @Test
    fun calculateCoveredLines() {
        JacocoUtil.calculateCoveredLines(JacocoUtil.generateDocument(jacocoSourceFile), "fc") shouldBe 5
        JacocoUtil.calculateCoveredLines(JacocoUtil.generateDocument(jacocoSourceFile), "pc") shouldBe 1
        JacocoUtil.calculateCoveredLines(JacocoUtil.generateDocument(jacocoSourceFile), "nc") shouldBe 54
    }

    @Test
    fun calculateCurrentFilePath() {
        JacocoUtil.calculateCurrentFilePath(path, File(path.remote)) shouldBe path

        JacocoUtil.calculateCurrentFilePath(path, File(jacocoSourceFile.remote), path.remote) shouldBe jacocoSourceFile
    }

    @Test
    fun chooseRandomLine() {
        val classDetails = mockkClass(SourceFileDetails::class)
        every { classDetails.jacocoSourceFile } returns File(jacocoSourceFile.remote)
        every { classDetails.parameters } returns parameters
        JacocoUtil.chooseRandomLine(classDetails, path) shouldNotBe null

        every { JacocoUtil.getLines(any()) } returns Elements()
        JacocoUtil.chooseRandomLine(classDetails, path) shouldBe null
    }

    @Test
    fun chooseRandomMethod() {
        mockkStatic(JacocoUtil::class)
        val classDetails = mockkClass(SourceFileDetails::class)
        every { classDetails.jacocoMethodFile } returns File(jacocoMethodFile.remote)
        every { classDetails.parameters } returns parameters
        JacocoUtil.chooseRandomMethod(classDetails, path) shouldNotBe null

        every { JacocoUtil.getNotFullyCoveredMethodEntries(any()) } returns arrayListOf()
        JacocoUtil.chooseRandomMethod(classDetails, path) shouldBe null
    }

    @Test
    fun computePackageName() {
        JacocoUtil.computePackageName(sourceFile.remote.removePrefix(path.remote)) shouldBe "com.example"
    }

    @Test
    fun generateDocument() {
        JacocoUtil.generateDocument(FilePath(null, path.remote + "/target/site/jacoco/index.html")) shouldNotBe null
    }

    @Test
    fun getCoverageInPercentageFromJacoco() {
        JacocoUtil.getCoverageInPercentageFromJacoco("Complex", jacocoCSVFile) shouldBe 0.04177545691906005
        JacocoUtil.getCoverageInPercentageFromJacoco("NotExisting", jacocoCSVFile) shouldBe 0.0
        JacocoUtil.getCoverageInPercentageFromJacoco("NestedLoop", jacocoCSVFile) shouldBe 0.6923076923076923
        JacocoUtil.getCoverageInPercentageFromJacoco("Calculator", jacocoCSVFile) shouldBe 1.0
        JacocoUtil.getCoverageInPercentageFromJacoco("Rational", jacocoCSVFile) shouldBe 0.0
    }

    @Test
    fun getFilesInAllSubdirectories() {
        JacocoUtil.getFilesInAllSubDirectories(path, "TEST-.+\\.xml") shouldHaveSize 4
        JacocoUtil.getFilesInAllSubDirectories(path, "jacoco.csv") shouldHaveSize  1
        JacocoUtil.getFilesInAllSubDirectories(path, "index.html") shouldHaveSize 2
    }

    @Test
    fun getJacocoFileInMultiBranchProject() {
        val run = mockkClass(Run::class)
        val job = mockkClass(Job::class)
        val project = mockkClass(WorkflowMultiBranchProject::class)
        every { run.parent } returns job
        every { job.parent } returns project
        parameters.branch = "testing"
        parameters.projectName = "test-project"
        val jacocoFile = FilePath(null, "/home/test/test-project_testing/file.html")
        val jacocoFileMaster = FilePath(null, "/home/test/test-project_master/file.html")

        JacocoUtil.getJacocoFileInMultiBranchProject(run, parameters, jacocoFile, "testing") shouldBe jacocoFile
        JacocoUtil.getJacocoFileInMultiBranchProject(run, parameters, jacocoFileMaster, "master") shouldBe jacocoFile
        every { job.parent } returns mockkClass(jenkins.branch.MultiBranchProject::class)
        JacocoUtil.getJacocoFileInMultiBranchProject(run, parameters, jacocoFile, "testing") shouldBe jacocoFile
    }

    @Test
    fun getLines() {
        mockkStatic(JacocoUtil::class)
        JacocoUtil.getLines(jacocoSourceFile) shouldHaveSize 55
        val rationalPath = FilePath(null, path.remote + "/target/site/jacoco/com.example/Rational.java.html")
        JacocoUtil.getLines(rationalPath) shouldHaveSize 91
    }

    @Test
    fun getMethodEntries() {
        JacocoUtil.getMethodEntries(jacocoMethodFile) shouldHaveSize 23
    }

    @Test
    fun getNotFullyCoveredMethodEntries() {
        JacocoUtil.getNotFullyCoveredMethodEntries(jacocoMethodFile) shouldHaveSize 21
    }

    @Test
    fun getProjectCoverage() {
        JacocoUtil.getProjectCoverage(path, "jacoco.csv") shouldBe 0.12087912087912088
    }

    @Test
    fun isGetterOrSetter() {
        val rationalPath = FilePath(null, path.remote + "/target/site/jacoco/com.example/Rational.java.html")
        JacocoUtil.isGetterOrSetter(rationalPath.readToString().split("\n"), "    return num;") shouldBe true
        JacocoUtil.isGetterOrSetter(rationalPath.readToString().split("\n"),
                "    return den.equals(B_ONE);") shouldBe false
        JacocoUtil.isGetterOrSetter(rationalPath.readToString().split("\n"),
                "      BigInteger num = decimal.toBigInteger();") shouldBe false
        JacocoUtil.isGetterOrSetter(rationalPath.readToString().split("\n"),
                "not found") shouldBe false
    }

    @Test
    fun testClassDetails() {
        val className = "Complex"
        val path = mockkClass(FilePath::class)
        val shortFilePath = "src/main/java/com/example/$className.java"
        val shortJacocoPath = "**/target/site/jacoco/"
        val shortJacocoCSVPath = "**/target/site/jacoco/jacoco.csv"
        val mocoJSONPath = "**/target/site/moco/mutation/"

        val coverage = 0.0
        val testCount = 10
        mockkStatic(JacocoUtil::class)
        val document = mockkClass(Document::class)
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        every { JUnitUtil.getTestCount(any(), any()) } returns testCount
        val commit = mockkClass(RevCommit::class)
        every { commit.name } returns "ef97erb"
        every { path.act(ofType(GitUtil.HeadCommitCallable::class)) } returns commit
        every { path.act(ofType(JacocoUtil.FilesOfAllSubDirectoriesCallable::class)) } returns arrayListOf()
        every { path.remote } returns this.path.remote
        every { path.channel } returns null
        val details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath, mocoJSONPath, hashMapOf(),
                TaskListener.NULL)

        details.filesExists() shouldBe true

        details.toString() shouldBe
                "ClassDetails{className='Complex', extension='java', packageName='com.example', changedByUsers}"

        val user = mockkClass(GitUtil.GameUser::class)
        every { user.fullName } returns "Philipp Straubinger"
        details.addUser(user)
        details.toString() shouldBe
                "ClassDetails{className='Complex', extension='java', packageName='com.example', " +
                "changedByUsers=Philipp Straubinger}"
    }

    @Test
    fun isGetLinesInRange() {
        JacocoUtil.getLinesInRange(jacocoSourceFile, 41, 4) shouldBe Pair("    double ns = a * a + b * b;\n" +
                "    double dArg = d / 2;\n" +
                "    double cArg = c * arg();\n" +
                "    double dDenom = Math.pow(Math.E, d * arg());\n" +
                "\n", "    double cArg = c * arg();")


        JacocoUtil.getLinesInRange(jacocoSourceFile, "dDenom", 4) shouldBe Pair("    double dArg = d / 2;\n" +
                "    double cArg = c * arg();\n" +
                "    double dDenom = Math.pow(Math.E, d * arg());\n" +
                "\n" +
                "    double newReal =\n", "")

        JacocoUtil.getLinesInRange(jacocoSourceFile, 5.0, 4) shouldBe Pair("", "")
    }
}