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
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.gamekins.mutation.MutationDetails
import org.gamekins.mutation.MutationInfo
import org.gamekins.mutation.MutationResults
import org.gamekins.util.GitUtil
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class MutationTestChallengeTest : AnnotationSpec() {

    private val className = "Challenge"
    private val path = FilePath(null, "/home/test/workspace")
    private val shortFilePath = "src/main/java/org/gamekins/challenge/$className.kt"
    private val shortJacocoPath = "**/target/site/jacoco/"
    private val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    private val mocoJSONPath = "**/target/site/moco/mutation/"
    private lateinit var details : JacocoUtil.ClassDetails
    private lateinit var challenge : MutationTestChallenge
    private lateinit var challenge1 : MutationTestChallenge
    private lateinit var challenge2 : MutationTestChallenge
    private val document = mockkClass(Document::class)
    private val element = mockkClass(Element::class)
    private val elements = Elements(listOf(element))
    private val coverage = 0.0
    private val run = mockkClass(Run::class)
    private val map = HashMap<String, String>()
    private val listener = TaskListener.NULL
    private val branch = "master"

    private val mutationDetails1 = MutationDetails(
        mapOf("className" to "org/example/ABC", "methodName" to "foo1", "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"),
        listOf(14),
        "AOR",
        "IADD-ISUB-51",
        "ABC.java", 51,
        "replace integer addition with integer subtraction",
        listOf("14"), mapOf("varName" to "numEntering"))
    private val mutation1 = MutationInfo(mutationDetails1, "survived", -1547277781)

    private val mutationDetails2 = MutationDetails(
        mapOf("className" to "org/example/Feature", "methodName" to "foo", "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"),
        listOf(22),
        "AOD",
        "IADD-S-56",
        "Feature.java", 56,
        "delete second operand after arithmetic operator integer addition",
        listOf("22"), mapOf())
    private val mutation2 = MutationInfo(mutationDetails2, "killed", -1547277782)


    private val mutationDetails3 = MutationDetails(
        mapOf("className" to "org/example/Feature", "methodName" to "foo", "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"),
        listOf(30),
        "ROR",
        "IF_ICMPGT-IF_ICMPLT-50",
        "Hihi.java", 56,
        "replace less than or equal operator with greater than or equal operator",
        listOf("30"), mapOf())
    private val mutation3 = MutationInfo(mutationDetails3, "survived", -1547277782)

    private val entries = mapOf("org.example.Feature" to setOf(mutation1, mutation2, mutation3))
    private val emptyEntries = mapOf<String, Set<MutationInfo>>()


    @BeforeEach
    fun init() {
        map["branch"] = branch
        mockkStatic(JacocoUtil::class)
        every { JacocoUtil.calculateCurrentFilePath(any(), any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        every { JacocoUtil.getLines(any()) }  returns elements
        mockkObject(MutationResults.Companion)
        every { MutationResults.retrievedMutationsFromJson(any(), any()) }  returns MutationResults(entries, "")
        details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath, mocoJSONPath, map,
            TaskListener.NULL)

        challenge = MutationTestChallenge(mutation1, details, branch, "commitID", "snippet", "line")
        challenge1 = MutationTestChallenge(mutation2, details, branch, "commitID", "", "line")
        challenge2 = MutationTestChallenge(mutation3, details, branch, "commitID", "", "line")
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun getScore() {
        challenge.getScore() shouldBe 4
    }

    @Test
    fun setSolved() {
        challenge.setSolved(1L)
        challenge.getSolved() shouldBe 1L
    }

    @Test
    fun getCreated() {
        challenge.getCreated() shouldNotBe null
    }

    @Test
    fun getName() {
        challenge.getName() shouldBe "Mutation"
    }


    @Test
    fun getMutationDescription() {
        challenge.getMutationDescription() shouldBe mutationDetails1.mutationDescription
    }

    @Test
    fun getFileName() {
        challenge.getFileName() shouldBe mutationDetails1.fileName
        challenge1.getFileName() shouldBe ""
    }

    @Test
    fun printToXML() {
        challenge.printToXML("abc", "") shouldBe
                ("<" + challenge::class.simpleName
                        + " created=\"" + challenge.getCreated()
                        + "\" solved=\"" + challenge.getSolved()
                        + "\" class=\"" + challenge.className
                        + "\" method=\"" + challenge.methodName
                        + "\" lineOfCode=\"" + challenge.lineOfCode
                        + "\" mutationDescription=\"" + challenge.getMutationDescription()
                        + "\" result=\"" + challenge.mutationInfo.result
                        + "\" reason=\"" + "abc"
                        + "\"/>")
    }

    @Test
    fun equals() {
        challenge.equals(challenge2) shouldBe false
        challenge.equals(challenge) shouldBe true
        challenge.equals(null) shouldBe false
        challenge.equals("abc") shouldBe false
    }

    @Test
    fun getConstants() {
        challenge.getConstants() shouldBe challenge.classDetails.constants
    }

    @Test
    fun getHashCode() {
        challenge.hashCode() shouldBe challenge.mutationInfo.hashCode()
    }

    @Test
    fun isSolvable() {
        val run = mockkClass(Run::class)
        val map = HashMap<String, String>()
        val listener = TaskListener.NULL
        val path1 = mockkClass(FilePath::class)
        var res:  List<String> = listOf("abc")
        every { path1.act(ofType(GitUtil.DiffFromHeadCallable::class)) } returns res
        challenge.isSolvable(map, run, listener, path1) shouldBe true
        res = listOf("org/gamekins/challenge/Challenge")
        every { path1.act(ofType(GitUtil.DiffFromHeadCallable::class)) } returns res
        challenge.isSolvable(map, run, listener, path1) shouldBe false
    }

    @Test
    fun isSolved() {
        val run = mockkClass(Run::class)
        val map = HashMap<String, String>()
        val listener = TaskListener.NULL
        val path1 = mockkClass(FilePath::class)


        challenge = MutationTestChallenge(mutation1, details, branch, "commitID", "snippet", "line")

        challenge.branch shouldBe branch
        challenge.commitID shouldBe "commitID"

        challenge.isSolved(map, run, listener, path1) shouldBe false
        challenge1.isSolved(map, run, listener, path1) shouldBe true
        every { MutationResults.retrievedMutationsFromJson(any(), any()) }  returns MutationResults(emptyEntries, "")
        challenge1.isSolved(map, run, listener, path1) shouldBe false
        every { MutationResults.retrievedMutationsFromJson(any(), any()) }  returns MutationResults(entries, "")
    }

    @Test
    fun testToString() {
        challenge.toString() shouldBe "Write a test to kill the mutant \"<b>replace integer addition with integer subtraction</b>\" at line <b>51</b> of method <b>foo1</b> in class <b>Challenge</b> in package <b>org.gamekins.challenge</b> (created for branch master)"
    }

    @Test
    fun testGetSnippet() {
        challenge.getSnippet() shouldBe "snippet"
    }

    @Test
    fun testGetMutatedLine() {
        challenge.getMutatedLine() shouldBe "line"
    }

    @Test
    fun testToEscapedString() {
        challenge.toEscapedString() shouldBe "Write a test to kill the mutant \"replace integer addition with integer subtraction\" at line 51 of method foo1 in class Challenge in package org.gamekins.challenge (created for branch master)"
    }

    @Test
    fun testCreateCodeSnippet() {
        val classDetails = mockkClass(JacocoUtil.ClassDetails::class)
        var lineOfCode = 51
        val path1 = mockkClass(FilePath::class)
        every { classDetails.jacocoSourceFile.exists() }  returns true
        every { classDetails.workspace } returns ""
        every { JacocoUtil.getLinesInRange(any(), any(), any()) }  returns Pair("", "")
        MutationTestChallenge.createCodeSnippet(classDetails, lineOfCode, path1) shouldBe Pair("", "")
        every { JacocoUtil.getLinesInRange(any(), any(), any()) }  returns Pair("abc", "")
        MutationTestChallenge.createCodeSnippet(classDetails, lineOfCode, path1) shouldBe Pair("<pre class='prettyprint linenums:49 mt-2'><code class='language-java'>abc</code></pre>", "")
        lineOfCode = -1
        MutationTestChallenge.createCodeSnippet(classDetails, lineOfCode, path1) shouldBe Pair("", "")
        lineOfCode = 51
        every { classDetails.jacocoSourceFile.exists() }  returns false
        MutationTestChallenge.createCodeSnippet(classDetails, lineOfCode, path1) shouldBe Pair("", "")
    }
}