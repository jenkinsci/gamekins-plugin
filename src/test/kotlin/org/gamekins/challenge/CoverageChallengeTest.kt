/*
 * Copyright 2020 Gamekins contributors
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
import hudson.model.TaskListener
import org.gamekins.util.JacocoUtil
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
        val map = HashMap<String, String>()
        map["branch"] = "master"
        details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath, map,
                TaskListener.NULL)
        challenge = ClassCoverageChallenge(details, path)

        challenge.printToXML("", "") shouldBe
                "<ClassCoverageChallenge created=\"${challenge.getCreated()}\" solved=\"${challenge.getSolved()}\" " +
                "class=\"$className\" coverage=\"$coverage\" coverageAtSolved=\"0.0\"/>"
        challenge.printToXML("", "    ") shouldStartWith "    <"
        challenge.printToXML("test", "") shouldBe
                "<ClassCoverageChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"$className\" " +
                "coverage=\"$coverage\" coverageAtSolved=\"0.0\" reason=\"test\"/>"
    }
}