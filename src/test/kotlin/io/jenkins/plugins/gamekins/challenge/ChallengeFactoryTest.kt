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

package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Result
import hudson.model.TaskListener
import hudson.model.User
import hudson.tasks.Mailer.UserProperty
import io.jenkins.plugins.gamekins.util.GitUtil
import io.jenkins.plugins.gamekins.util.JacocoUtil
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beOfType
import io.mockk.*
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.collections.HashMap
import kotlin.random.Random

class ChallengeFactoryTest : AnnotationSpec() {

    private val user = mockkClass(User::class)
    private val property = mockkClass(io.jenkins.plugins.gamekins.GameUserProperty::class)
    private val map = HashMap<String, String>()
    private val branch = "master"
    private val listener = TaskListener.NULL
    private val className = "Challenge"
    private val path = mockkClass(FilePath::class)
    private val shortFilePath = "src/main/java/io/jenkins/plugins/gamekins/challenge/$className.kt"
    private val shortJacocoPath = "**/target/site/jacoco/"
    private val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    private val coverage = 0.0
    private val testCount = 10
    private lateinit var details : JacocoUtil.ClassDetails

    @BeforeEach
    fun init() {
        map["branch"] = branch
        map["projectName"] = "test-project"
        mockkStatic(JacocoUtil::class)
        mockkStatic(ChallengeFactory::class)
        val document = mockkClass(Document::class)
        every { user.getProperty(io.jenkins.plugins.gamekins.GameUserProperty::class.java) } returns property
        every { user.fullName } returns "Philipp Straubinger"
        every { user.id } returns "id"
        val mailProperty = mockkClass(UserProperty::class)
        every { mailProperty.address } returns "philipp.straubinger@uni-passau.de"
        every { user.getProperty(UserProperty::class.java) } returns mailProperty
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList()
        every { property.getGitNames() } returns CopyOnWriteArraySet(listOf("Philipp Straubinger"))
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        every { JacocoUtil.getTestCount(any(), any()) } returns testCount
        val commit = mockkClass(RevCommit::class)
        every { commit.name } returns "ef97erb"
        every { commit.authorIdent } returns PersonIdent("", "")
        every { path.act(ofType(GitUtil.HeadCommitCallable::class)) } returns commit
        every { path.act(ofType(JacocoUtil.FilesOfAllSubDirectoriesCallable::class)) } returns arrayListOf()
        every { path.remote } returns "/home/test/workspace"
        every { path.channel } returns null
        details = JacocoUtil.ClassDetails(path, shortFilePath, shortJacocoPath, shortJacocoCSVPath, map, listener)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun generateBuildChallenge() {
        ChallengeFactory.generateBuildChallenge(null, user, path, property, map) shouldBe false

        ChallengeFactory.generateBuildChallenge(Result.SUCCESS, user, path, property, map) shouldBe false

        mockkStatic(User::class)
        mockkStatic(GitUtil::class)
        every { User.getAll() } returns listOf()
        every { GitUtil.mapUser(any(), listOf()) } returns null
        ChallengeFactory.generateBuildChallenge(Result.FAILURE, user, path, property, map) shouldBe false

        every { GitUtil.mapUser(any(), listOf()) } returns user
        every { property.getCurrentChallenges(any()) } returns CopyOnWriteArrayList(listOf(BuildChallenge(map)))
        ChallengeFactory.generateBuildChallenge(Result.FAILURE, user, path, property, map) shouldBe false

        every { property.getCurrentChallenges(any()) } returns CopyOnWriteArrayList()
        every { property.newChallenge(any(), any()) } returns Unit
        every { user.save() } returns Unit
        ChallengeFactory.generateBuildChallenge(Result.FAILURE, user, path, property, map) shouldBe true
    }

    @Test
    fun generateNewChallenges() {
        every { property.getCurrentChallenges(any()) } returns CopyOnWriteArrayList()
        ChallengeFactory.generateNewChallenges(user, property, map, arrayListOf(details), path, maxChallenges = 0) shouldBe 0

        every { property.newChallenge(any(), any()) } returns Unit
        ChallengeFactory.generateNewChallenges(user, property, map, arrayListOf(details), path) shouldBe 0

        val newDetails = mockkClass(JacocoUtil.ClassDetails::class)
        every { newDetails.changedByUsers } returns hashSetOf(GitUtil.GameUser(user))
        mockkStatic(ChallengeFactory::class)
        every { ChallengeFactory.generateChallenge(any(), any(), any(), any(), any()) } returns mockkClass(TestChallenge::class)
        ChallengeFactory.generateNewChallenges(user, property, map, arrayListOf(newDetails), path) shouldBe 3
        mockkStatic(ChallengeFactory::class)
    }

    @Test
    fun generateClassCoverageChallenge() {
        mockkObject(Random)
        every { Random.nextInt(any()) } returns 0
        every { JacocoUtil.calculateCoveredLines(any(), "nc") } returns 10
        ChallengeFactory.generateChallenge(user, map, listener, arrayListOf(details), path) should
                beOfType(ClassCoverageChallenge::class)
    }

    @Test
    fun generateDummyChallenge() {
        mockkObject(Random)
        every { Random.nextInt(any()) } returns 0
        ChallengeFactory.generateChallenge(user, map, listener, arrayListOf(details), path) should
                beOfType(DummyChallenge::class)
    }

    @Test
    fun generateLineCoverageChallenge() {
        mockkObject(Random)
        every { Random.nextInt(any()) } returns 2
        every { Random.nextInt(1) } returns 0
        every { JacocoUtil.calculateCoveredLines(any(), "pc") } returns 10
        val element = mockkClass(Element::class)
        val elements = Elements(listOf(element))
        every { JacocoUtil.getLines(any()) }  returns elements
        every { element.attr("id") } returns "L5"
        every { element.attr("class") } returns "nc"
        every { element.attr("title") } returns ""
        every { element.text() } returns "toString();"
        ChallengeFactory.generateChallenge(user, map, listener, arrayListOf(details), path) should
                beOfType(LineCoverageChallenge::class)
    }

    @Test
    fun generateMethodCoverageChallenge() {
        mockkObject(Random)
        every { Random.nextInt(any()) } returns 6
        every { Random.nextInt(1) } returns 0
        every { JacocoUtil.calculateCoveredLines(any(), "nc") } returns 10
        val method = JacocoUtil.CoverageMethod("toString", 10, 10)
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method)
        ChallengeFactory.generateChallenge(user, map, listener, arrayListOf(details), path) should
                beOfType(MethodCoverageChallenge::class)
    }

    @Test
    fun generateTestChallenge() {
        mockkObject(Random)
        every { Random.nextInt(any()) } returns 9
        ChallengeFactory.generateChallenge(user, map, listener, arrayListOf(details), path) should
                beOfType(TestChallenge::class)
    }
}
