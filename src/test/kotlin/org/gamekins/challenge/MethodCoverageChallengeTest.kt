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

package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.util.JacocoUtil
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.gamekins.file.SourceFileDetails
import org.gamekins.util.Constants.Parameters
import org.jsoup.nodes.Document

class MethodCoverageChallengeTest : AnnotationSpec() {

    private val className = "Challenge"
    private val path = FilePath(null, "/home/test/workspace")
    private val shortFilePath = "src/main/java/org/gamekins/challenge/$className.kt"
    private val shortJacocoPath = "**/target/site/jacoco/"
    private val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    private val mocoJSONPath = "**/target/site/moco/mutation/"
    private lateinit var details : SourceFileDetails
    private lateinit var challenge : MethodCoverageChallenge
    private lateinit var method : JacocoUtil.CoverageMethod
    private val coverage = 0.0
    private val run = mockkClass(Run::class)
    private val parameters = Parameters()
    private val listener = TaskListener.NULL
    private val branch = "master"
    private val methodName = "toString"
    private val data = mockkClass(Challenge.ChallengeGenerationData::class)

    @BeforeEach
    fun init() {
        parameters.branch = branch
        parameters.workspace = path
        parameters.jacocoResultsPath = shortJacocoPath
        parameters.jacocoCSVPath = shortJacocoCSVPath
        parameters.mocoJSONPath = mocoJSONPath
        mockkStatic(JacocoUtil::class)
        val document = mockkClass(Document::class)
        method = JacocoUtil.CoverageMethod(methodName, 10, 10, "")
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        every { JacocoUtil.getNotFullyCoveredMethodEntries(any()) } returns arrayListOf(method)
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf()
        details = SourceFileDetails(parameters, shortFilePath, listener)
        every { data.selectedFile } returns details
        every { data.parameters } returns parameters
        every { data.method } returns method
        challenge = MethodCoverageChallenge(data)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun getScore() {
        challenge.getScore() shouldBe 2
        method = JacocoUtil.CoverageMethod(methodName, 10, 1, "")
        every { JacocoUtil.getNotFullyCoveredMethodEntries(any()) } returns arrayListOf(method)
        every { data.method } returns method
        challenge = MethodCoverageChallenge(data)
        challenge.getScore() shouldBe 3
        challenge.toEscapedString()
        challenge.getName() shouldBe "Method Coverage"
    }

    @Test
    fun isSolvable() {
        challenge.isSolvable(parameters, run, listener) shouldBe true
        parameters.branch = branch
        challenge.isSolvable(parameters, run, listener) shouldBe true
        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns true
        every { JacocoUtil.calculateCurrentFilePath(any(), any(), any()) } returns pathMock
        challenge.isSolvable(parameters, run, listener) shouldBe false
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method)
        challenge.isSolvable(parameters, run, listener) shouldBe true
        method = JacocoUtil.CoverageMethod(methodName, 10, 0, "")
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method)
        challenge.isSolvable(parameters, run, listener) shouldBe false
    }

    @Test
    fun isSolved() {
        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns false
        every { pathMock.remote } returns path.remote
        every { JacocoUtil.getJacocoFileInMultiBranchProject(any(), any(), any(), any()) } returns pathMock
        challenge.isSolved(parameters, run, listener) shouldBe false
        every { pathMock.exists() } returns true
        challenge.isSolved(parameters, run, listener) shouldBe false
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method)
        challenge.isSolved(parameters, run, listener) shouldBe false
        method = JacocoUtil.CoverageMethod(methodName, 10, 0, "")
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method)
        challenge.isSolved(parameters, run, listener) shouldBe true
    }
}