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

package org.gamekins.mutation

import hudson.FilePath
import hudson.model.User
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.gamekins.GameUserProperty
import org.gamekins.challenge.Challenge
import org.gamekins.challenge.MutationTestChallenge
import org.gamekins.file.SourceFileDetails
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals


class MutationUtilsTest: AnnotationSpec()  {

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

    private val entries = mapOf("org.example.Feature" to listOf(mutation1, mutation2, mutation3))
    private val details = mockkClass(SourceFileDetails::class)
    private val path = FilePath(null, "/home/test/workspace")
    private val challenge = MutationTestChallenge(mutation1, details, "branch", "commitID", "snippet", "line")

    @Test
    fun testGetSurvivedMutationList() {
        val user = mockkClass(User::class)
        every { user.getProperty(GameUserProperty::class.java).getRejectedChallenges("abcd") } returns CopyOnWriteArrayList<Pair<Challenge, String>>()
        MutationUtils.getSurvivedMutationList(setOf(mutation1, mutation2), "abc", listOf(), user, "abcd") shouldBe listOf(mutation1)

        every { user.getProperty(GameUserProperty::class.java).getRejectedChallenges("abcd") } returns CopyOnWriteArrayList<Pair<Challenge, String>>(
            listOf(Pair(challenge as Challenge, "")))
        MutationUtils.getSurvivedMutationList(setOf(mutation1, mutation2), "abc", listOf(), user, "abcd") shouldBe listOf()
    }

    @Test
    fun testGetCurrentLinesOperatorMapping() {
        every { details.packageName } returns "org.example"
        every { details.fileName } returns "Example"
        val challenge1 = MutationTestChallenge(mutation1, details, "branch", "commitID", "snippet", "line")

        val currentChallenges = listOf(challenge1)
        val fullClassName = "org.example.Example"
        var currentLinesOperatorMap = mutableMapOf<Int, MutableSet<String>>()
        MutationUtils.getCurrentLinesOperatorMapping(currentChallenges, fullClassName, currentLinesOperatorMap)
        assertEquals(currentLinesOperatorMap, mutableMapOf(51 to mutableSetOf("AOR")))
        currentLinesOperatorMap = mutableMapOf(51 to mutableSetOf("UOI"))
        MutationUtils.getCurrentLinesOperatorMapping(currentChallenges, fullClassName, currentLinesOperatorMap)
        assertEquals(currentLinesOperatorMap, mutableMapOf(51 to mutableSetOf("UOI", "AOR")))
    }

    @Test
    fun testShouldIgnoreMutation() {
        MutationUtils.shouldIgnoreMutation(challenge, mutation1, "commitID") shouldBe true
        MutationUtils.shouldIgnoreMutation(challenge, mutation1, "commitID1") shouldBe true
    }


    @Test
    fun testFindMutationHasCodeSnippets() {
        val survivedList = listOf(mutation1)
        val remotePath = mockkClass(FilePath::class)
        val currentLinesOperatorMap = mutableMapOf(51 to mutableSetOf("UOI", "AOR"))
        val currentChallengeMethods = setOf("foo")
        mockkObject(MutationUtils)
        every { MutationUtils.handleMutationInNewMethods(any(), details, remotePath, currentLinesOperatorMap) } returns Pair(mutation1, mapOf())
        MutationUtils.findMutationHasCodeSnippets(survivedList, details, remotePath, currentLinesOperatorMap, currentChallengeMethods) shouldBe Pair(mutation1, mapOf())

        every { MutationUtils.handleMutationInNewMethods(any(), details, remotePath, currentLinesOperatorMap) } returns Pair(null, mapOf())
        every { MutationUtils.handleMutationInOldMethods(any(), details, remotePath, currentLinesOperatorMap) } returns Pair(mutation1, mapOf())
        MutationUtils.findMutationHasCodeSnippets(survivedList, details, remotePath, currentLinesOperatorMap, currentChallengeMethods) shouldBe Pair(mutation1, mapOf())
    }


    @Test
    fun testHandleMutationInNewMethods() {
        val muList = listOf(mutation1)
        val remotePath = mockkClass(FilePath::class)
        val currentLinesOperatorMap = mutableMapOf(51 to mutableSetOf("UOI", "AOR"))
        mockkObject(MutationUtils)
        every { MutationUtils.processMutationSnippet(any(), details, remotePath) } returns Pair(mutation1, mapOf())
        MutationUtils.handleMutationInNewMethods(muList, details, remotePath, currentLinesOperatorMap) shouldBe Pair(mutation1, mapOf())
        every { MutationUtils.processMutationSnippet(any(), details, remotePath) } returns Pair(null, mapOf())
        MutationUtils.handleMutationInNewMethods(muList, details, remotePath, currentLinesOperatorMap) shouldBe Pair(null, mapOf())
    }


    @Test
    fun testHandleMutationInOldMethods() {
        val muList = listOf(mutation1)
        val remotePath = mockkClass(FilePath::class)
        var currentLinesOperatorMap = mutableMapOf(50 to mutableSetOf("UOI", "AOR"))
        mockkObject(MutationUtils)
        every { MutationUtils.processMutationSnippet(any(), details, remotePath) } returns Pair(mutation1, mapOf())
        MutationUtils.handleMutationInOldMethods(muList, details, remotePath, currentLinesOperatorMap) shouldBe Pair(mutation1, mapOf())
        every { MutationUtils.processMutationSnippet(any(), details, remotePath) } returns Pair(null, mapOf())
        MutationUtils.handleMutationInOldMethods(muList, details, remotePath, currentLinesOperatorMap) shouldBe Pair(null, mapOf())
        currentLinesOperatorMap = mutableMapOf(51 to mutableSetOf("UOI", "AOR"))
        every { MutationUtils.processMutationSnippet(any(), details, remotePath) } returns Pair(null, mapOf())
        MutationUtils.handleMutationInOldMethods(muList, details, remotePath, currentLinesOperatorMap) shouldBe Pair(null, mapOf())
    }


    @Test
    fun testProcessMutationSnippet() {
        val muList = listOf(mutation1)
        val remotePath = mockkClass(FilePath::class)
        mockkObject(MutationTestChallenge.Companion)
        every { MutationTestChallenge.createCodeSnippet(any(), any(), any()) } returns Pair("", "")
        MutationUtils.processMutationSnippet(muList, details, remotePath) shouldBe Pair(null, mapOf("codeSnippet" to "", "mutatedSnippet" to ""))
        every { MutationTestChallenge.createCodeSnippet(any(), any(), any()) } returns Pair("abc", "def")
        mockkObject(MutationPresentation)
        every { MutationPresentation.createMutatedLine(any(), any(), any()) } returns "xyz"
        MutationUtils.processMutationSnippet(muList, details, remotePath) shouldBe Pair(mutation1, mapOf("codeSnippet" to "abc", "mutatedSnippet" to "xyz"))
    }
}