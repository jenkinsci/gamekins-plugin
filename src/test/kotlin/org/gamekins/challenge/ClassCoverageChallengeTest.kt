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

class ClassCoverageChallengeTest : FeatureSpec({

    val className = "Challenge"
    val path = FilePath(null, "/home/test/workspace")
    val shortFilePath = "src/main/java/org/gamekins/challenge/$className.kt"
    val shortJacocoPath = "**/target/site/jacoco/"
    val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    lateinit var details : SourceFileDetails
    lateinit var challenge : ClassCoverageChallenge
    val coverage = 0.0
    val run = mockkClass(Run::class)
    val parameters = Parameters()
    val listener = TaskListener.NULL
    val branch = "master"
    val data = mockkClass(Challenge.ChallengeGenerationData::class)

    beforeSpec {
        parameters.branch = branch
        parameters.workspace = path
        parameters.jacocoResultsPath = shortJacocoPath
        parameters.jacocoCSVPath = shortJacocoCSVPath
        mockkStatic(JacocoUtil::class)
        val document = mockkClass(Document::class)
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        details = SourceFileDetails(parameters, shortFilePath, listener)
        every { data.selectedFile } returns details
        every { data.parameters } returns parameters
        challenge = ClassCoverageChallenge(data)
    }

    afterSpec {
        unmockkAll()
    }

    feature("getScore") {
        scenario("Coverage below 0.8")
        {
            challenge.getScore() shouldBe 1
        }

        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns 0.9
        details = SourceFileDetails(parameters, shortFilePath, listener)
        every { data.selectedFile } returns details
        challenge = ClassCoverageChallenge(data)
        scenario("Coverage above 0.8")
        {
            challenge.getScore() shouldBe 2
        }
    }

    feature("toEscapedString")
    {
        challenge.toEscapedString() shouldBe "Write a test to cover more lines in class $className in package " +
                "org.gamekins.challenge (created for branch $branch)"
    }

    feature("isSolvable") {
        val solvableDetails = mockkClass(SourceFileDetails::class)
        every { solvableDetails.jacocoSourceFile } returns details.jacocoSourceFile
        every { solvableDetails.parameters } returns parameters
        every { solvableDetails.coverage } returns details.coverage
        every { solvableDetails.fileName } returns details.fileName
        every { solvableDetails.update(any()) } returns solvableDetails
        every { solvableDetails.filesExists() } returns false
        val solvableData = mockkClass(Challenge.ChallengeGenerationData::class)
        every { solvableData.selectedFile } returns solvableDetails
        every { solvableData.parameters } returns parameters
        val solvableChallenge = ClassCoverageChallenge(solvableData)
        val newParameters = Parameters(branch = "stale")
        scenario("Different Branch")
        {
            solvableChallenge.isSolvable(newParameters, run, listener) shouldBe true
        }

        scenario("FileDetails do not exist")
        {
            solvableChallenge.isSolvable(parameters, run, listener) shouldBe false
        }

        every { solvableDetails.filesExists() } returns true
        scenario("JacocoSourceFile does not exist yet (No coverage)")
        {
            solvableChallenge.isSolvable(parameters, run, listener) shouldBe true
        }

        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns true
        every { JacocoUtil.calculateCurrentFilePath(any(), any(), any()) } returns pathMock
        scenario("All Lines covered (No uncovered / partially covered lines exist)")
        {
            solvableChallenge.isSolvable(parameters, run, listener) shouldBe false
        }

        every { JacocoUtil.calculateCoveredLines(any(), "pc") } returns 1
        scenario("Line to be fully covered still exists")
        {
            solvableChallenge.isSolvable(parameters, run, listener) shouldBe true
        }

        every { JacocoUtil.calculateCoveredLines(any(), "pc") } returns 0
        every { JacocoUtil.calculateCoveredLines(any(), "nc") } returns 1
        scenario("Line to be covered still exists")
        {
            solvableChallenge.isSolvable(parameters, run, listener) shouldBe true
        }
    }

    feature("isSolved") {
        val pathMock = mockkClass(FilePath::class)
        every { pathMock.exists() } returns false
        every { pathMock.remote } returns path.remote
        every { JacocoUtil.getJacocoFileInMultiBranchProject(any(), any(), any(), any()) } returns pathMock
        scenario("File does not exist")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false
        }
        every { pathMock.exists() } returns true
        scenario("No line fully covered")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false
        }
        every { JacocoUtil.calculateCoveredLines(any(), "fc") } returns 1
        scenario("One line is fully covered")
        {
            challenge.isSolved(parameters, run, listener) shouldBe true
        }
    }
})