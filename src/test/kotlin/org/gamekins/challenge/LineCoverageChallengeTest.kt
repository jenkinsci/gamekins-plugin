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
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.gamekins.file.SourceFileDetails
import org.gamekins.util.Constants.Parameters
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class LineCoverageChallengeTest : FeatureSpec({

    val className = "Challenge"
    val path = FilePath(null, "/home/test/workspace")
    val shortFilePath = "src/main/java/org/gamekins/challenge/$className.kt"
    val shortJacocoPath = "**/target/site/jacoco/"
    val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    val mocoJSONPath = "**/target/site/moco/mutation/"
    lateinit var details : SourceFileDetails
    lateinit var challenge : LineCoverageChallenge
    val document = mockkClass(Document::class)
    val element = mockkClass(Element::class)
    val elements = Elements(listOf(element))
    val coverage = 0.0
    val run = mockkClass(Run::class)
    val parameters = Parameters()
    val listener = TaskListener.NULL
    val branch = "master"
    val data = mockkClass(Challenge.ChallengeGenerationData::class)

    beforeSpec {
        mockkStatic(JacocoUtil::class)
    }

    beforeTest {
        parameters.branch = branch
        parameters.workspace = path
        parameters.jacocoResultsPath = shortJacocoPath
        parameters.jacocoCSVPath = shortJacocoCSVPath
        parameters.mocoJSONPath = mocoJSONPath
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
        every { data.selectedFile } returns details
        every { data.parameters } returns parameters
        every { data.line } returns element
        challenge = LineCoverageChallenge(data)
    }

    afterSpec {
        unmockkAll()
    }

    feature("getMaxCoveredBranchesIfFullyCovered") {
        scenario("Default Value")
        {
            challenge.getMaxCoveredBranchesIfFullyCovered() shouldBe 0
        }

        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns true
        every { pathMock.remote } returns path.remote
        every { JacocoUtil.getJacocoFileInMultiBranchProject(any(), any(), any(), any()) } returns pathMock
        every { document.select("span.pc") } returns Elements()
        every { document.select("span.nc") } returns Elements()
        every { document.select("span.fc") } returns elements
        scenario("Line covered")
        {
            challenge.isSolved(parameters, run, listener) shouldBe true     //needs to be called to update
            challenge.getMaxCoveredBranchesIfFullyCovered() shouldBe 1
        }

        every { document.select("span.fc") } returns Elements()
        scenario("No lines covered")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false    //needs to be called to update
            challenge.getMaxCoveredBranchesIfFullyCovered() shouldBe 0
        }

    }

    feature("getScore") {
        scenario("Default Value")
        {
            challenge.getScore() shouldBe 2
        }

        scenario("Sufficient coverage")
        {
            every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns 0.9
            details = SourceFileDetails(parameters, shortFilePath, listener)
            every { data.selectedFile } returns details
            challenge = LineCoverageChallenge(data)
            challenge.getScore() shouldBe 3
        }

        scenario("Partially covered")
        {
            every { element.attr("class") } returns "pc"
            every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
            details = SourceFileDetails(parameters, shortFilePath, listener)
            every { data.selectedFile } returns details
            challenge = LineCoverageChallenge(data)
            challenge.getScore() shouldBe 3
        }

        scenario("Partially covered, values from title")
        {
            every { element.attr("class") } returns "pc"
            every { element.attr("title") } returns "1 of 2 branches missed."
            every { data.line } returns element
            challenge = LineCoverageChallenge(data)
            challenge.getScore() shouldBe 3
        }

        scenario("Not covered, values from title")
        {
            every { element.attr("title") } returns "All 2 branches missed."
            every { element.attr("class") } returns "nc"
            every { data.line } returns element
            challenge = LineCoverageChallenge(data)
            challenge.getScore() shouldBe 2
        }
    }

    feature("isSolvable") {
        scenario("JacocoFiles do not exist")
        {
            challenge.isSolvable(parameters, run, listener) shouldBe true
        }

        every { document.select("span.pc") } returns Elements()
        every { document.select("span.nc") } returns Elements()
        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns true
        every { JacocoUtil.calculateCurrentFilePath(any(), any(), any()) } returns pathMock
        scenario("No uncovered lines exist")
        {
            challenge.isSolvable(parameters, run, listener) shouldBe false
        }

        every { document.select("span.nc") } returns elements
        scenario("An uncovered line exists")
        {
            challenge.isSolvable(parameters, run, listener) shouldBe true
        }
    }

    feature("isSolved") {
        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns false
        every { pathMock.remote } returns path.remote
        every { JacocoUtil.getJacocoFileInMultiBranchProject(any(), any(), any(), any()) } returns pathMock
        scenario("Scenario1")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false
        }

        every { pathMock.exists() } returns true
        every { document.select("span.pc") } returns Elements()
        every { document.select("span.fc") } returns Elements()
        every { document.select("span.nc") } returns Elements()
        scenario("Scenario2")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false
        }

        every { document.select("span.fc") } returns elements
        scenario("Scenario3")
        {
            challenge.isSolved(parameters, run, listener) shouldBe true
        }

        every { element.attr("class") } returns "pc"
        every { data.line } returns element
        challenge = LineCoverageChallenge(data)
        scenario("Scenario4")
        {
            challenge.isSolved(parameters, run, listener) shouldBe true
        }

        every { element.attr("class") } returns "nc"
        every { document.select("span.fc") } returns Elements()
        every { document.select("span.nc") } returns elements
        scenario("Scenario5")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false
        }

        every { element.attr("class") } returns "fc"
        every { element.attr("id") } returns "L6"
        every { document.select("span.fc") } returns elements
        every { document.select("span.nc") } returns Elements()
        scenario("Scenario6")
        {
            challenge.isSolved(parameters, run, listener) shouldBe true
        }

        every { data.line } returns element

        scenario("Scenario7")
        {
            every { element.attr("title") } returns "2 of 3 branches missed."
            every { element.attr("class") } returns "pc"
            challenge = LineCoverageChallenge(data)
            challenge.isSolved(parameters, run, listener) shouldBe false
        }

        every { data.line } returns element
        challenge = LineCoverageChallenge(data)

        scenario("Scenario8")
        {
            every { element.attr("title") } returns "All 3 branches missed."
            every { element.attr("class") } returns "nc"
            challenge.isSolved(parameters, run, listener) shouldBe false
        }

        every { element.attr("title") } returns "1 of 3 branches missed."
        scenario("Scenario9")
        {
            challenge.isSolved(parameters, run, listener) shouldBe true
        }
    }
})