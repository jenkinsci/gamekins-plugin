/*
 * Copyright 2021 Gamekins contributors
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
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class LineCoverageChallengeTest : AnnotationSpec() {

    private val className = "Challenge"
    private val path = FilePath(null, "/home/test/workspace")
    private val shortFilePath = "src/main/java/org/gamekins/challenge/$className.kt"
    private val shortJacocoPath = "**/target/site/jacoco/"
    private val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    private val mocoJSONPath = "**/target/site/moco/mutation/"
    private lateinit var details : SourceFileDetails
    private lateinit var challenge : LineCoverageChallenge
    private val document = mockkClass(Document::class)
    private val element = mockkClass(Element::class)
    private val elements = Elements(listOf(element))
    private val coverage = 0.0
    private val run = mockkClass(Run::class)
    private val parameters = Parameters()
    private val listener = TaskListener.NULL
    private val branch = "master"
    private val data = mockkClass(Challenge.ChallengeGenerationData::class)

    @BeforeEach
    fun init() {
        parameters.branch = branch
        parameters.workspace = path
        parameters.jacocoResultsPath = shortJacocoPath
        parameters.jacocoCSVPath = shortJacocoCSVPath
        parameters.mocoJSONPath = mocoJSONPath
        mockkStatic(JacocoUtil::class)
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        every { JacocoUtil.getLines(any()) }  returns elements
        every { element.attr("id") } returns "L5"
        every { element.attr("class") } returns "nc"
        every { element.attr("title") } returns ""
        every { element.text() } returns "toString();"
        details = SourceFileDetails(parameters, shortFilePath, listener)
        every { data.selectedClass } returns details
        every { data.parameters } returns parameters
        every { data.line } returns element
        challenge = LineCoverageChallenge(data)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun getMaxCoveredBranchesIfFullyCovered() {
        challenge.getMaxCoveredBranchesIfFullyCovered() shouldBe 0
        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns true
        every { pathMock.remote } returns path.remote
        every { JacocoUtil.getJacocoFileInMultiBranchProject(any(), any(), any(), any()) } returns pathMock
        every { document.select("span.pc") } returns Elements()
        every { document.select("span.nc") } returns Elements()
        every { document.select("span.fc") } returns elements
        challenge.isSolved(parameters, run, listener) shouldBe true
        challenge.getMaxCoveredBranchesIfFullyCovered() shouldBe 1
    }

    @Test
    fun getScore() {
        challenge.getScore() shouldBe 2
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns 0.9
        details = SourceFileDetails(parameters, shortFilePath, listener)
        every { data.selectedClass } returns details
        challenge = LineCoverageChallenge(data)
        challenge.getScore() shouldBe 3
        challenge.toEscapedString()
        every { element.attr("class") } returns "pc"
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        details = SourceFileDetails(parameters, shortFilePath, listener)
        every { data.selectedClass } returns details
        challenge = LineCoverageChallenge(data)
        challenge.getScore() shouldBe 3
        challenge.toEscapedString()
        challenge.getName() shouldBe "Line Coverage"
        every { element.attr("title") } returns "1 of 2 branches missed."
        every { data.line } returns element
        challenge = LineCoverageChallenge(data)
        challenge.getScore() shouldBe 3
        challenge.toEscapedString()
        every { element.attr("title") } returns "All 2 branches missed."
        every { element.attr("class") } returns "nc"
        every { data.line } returns element
        challenge = LineCoverageChallenge(data)
        challenge.getScore() shouldBe 2
        challenge.toEscapedString()
    }

    @Test
    fun isSolvable() {
        challenge.isSolvable(parameters, run, listener) shouldBe true
        parameters.branch = branch
        challenge.isSolvable(parameters, run, listener) shouldBe true
        every { document.select("span.pc") } returns Elements()
        every { document.select("span.nc") } returns Elements()
        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns true
        every { JacocoUtil.calculateCurrentFilePath(any(), any(), any()) } returns pathMock
        challenge.isSolvable(parameters, run, listener) shouldBe false
        every { document.select("span.nc") } returns elements
        challenge.isSolvable(parameters, run, listener) shouldBe true
    }

    @Test
    fun isSolved() {
        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns false
        every { pathMock.remote } returns path.remote
        every { JacocoUtil.getJacocoFileInMultiBranchProject(any(), any(), any(), any()) } returns pathMock
        challenge.isSolved(parameters, run, listener) shouldBe false
        every { pathMock.exists() } returns true
        every { document.select("span.pc") } returns Elements()
        every { document.select("span.fc") } returns Elements()
        every { document.select("span.nc") } returns Elements()
        challenge.isSolved(parameters, run, listener) shouldBe false
        every { document.select("span.fc") } returns elements
        challenge.isSolved(parameters, run, listener) shouldBe true
        every { element.attr("class") } returns "pc"
        every { data.line } returns element
        challenge = LineCoverageChallenge(data)
        challenge.isSolved(parameters, run, listener) shouldBe true
        every { element.attr("class") } returns "nc"
        every { document.select("span.fc") } returns Elements()
        every { document.select("span.nc") } returns elements
        challenge.isSolved(parameters, run, listener) shouldBe false
        every { element.attr("class") } returns "fc"
        every { element.attr("id") } returns "L6"
        every { document.select("span.fc") } returns elements
        every { document.select("span.nc") } returns Elements()
        challenge.isSolved(parameters, run, listener) shouldBe true
        every { element.attr("title") } returns "2 of 3 branches missed."
        every { element.attr("class") } returns "pc"
        every { data.line } returns element
        challenge = LineCoverageChallenge(data)
        challenge.isSolved(parameters, run, listener) shouldBe false
        every { element.attr("title") } returns "All 3 branches missed."
        every { element.attr("class") } returns "nc"
        every { data.line } returns element
        challenge = LineCoverageChallenge(data)
        challenge.isSolved(parameters, run, listener) shouldBe false
        every { element.attr("title") } returns "1 of 3 branches missed."
        challenge.isSolved(parameters, run, listener) shouldBe true
    }
}