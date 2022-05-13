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
import hudson.model.AbstractItem
import hudson.model.AbstractProject
import hudson.model.Job
import hudson.model.User
import hudson.util.FormValidation
import org.gamekins.GameUserProperty
import org.gamekins.challenge.ChallengeFactory
import org.gamekins.challenge.LineCoverageChallenge
import org.gamekins.property.GameJobProperty
import org.gamekins.statistics.Statistics
import org.gamekins.util.Constants.Parameters
import org.gamekins.test.TestUtils
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.file.SourceFileDetails
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class ActionUtilTest: AnnotationSpec() {

    private val job = mockkClass(AbstractItem::class)
    private val user = mockkClass(User::class)
    private val userProperty = mockkClass(GameUserProperty::class)
    private val challenge = mockkClass(LineCoverageChallenge::class)
    private val stringChallenge = "LineCoverageChallenge"
    private val parameters = Parameters()
    private lateinit var root : String

    @BeforeAll
    fun init() {
        mockkStatic(User::class)

        every { job.fullName } returns "test-project"
        every { job.save() } returns Unit
        every { user.save() } returns Unit
        every { challenge.toString() } returns stringChallenge
        every { challenge.toEscapedString() } returns stringChallenge
        every { userProperty.rejectChallenge(any(), any(), any()) } returns Unit
        every { userProperty.newChallenge(any(), any()) } returns Unit
        every { userProperty.storeChallenge(any(), any()) } returns Unit
        every { userProperty.undoStoreChallenge(any(), any()) } returns Unit

        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
        File(root).deleteRecursively()
    }

    @Test
    fun doRejectChallenge() {
        ActionUtil.doRejectChallenge(job, "", "").kind shouldBe FormValidation.Kind.ERROR

        every { User.current() } returns null
        ActionUtil.doRejectChallenge(job, "", " ").kind shouldBe FormValidation.Kind.ERROR

        every { User.current() } returns user
        every { user.getProperty(GameUserProperty::class.java) } returns null
        ActionUtil.doRejectChallenge(job, "", "reason").kind shouldBe FormValidation.Kind.ERROR

        every { user.getProperty(GameUserProperty::class.java) } returns userProperty
        every { userProperty.getCurrentChallenges(any()) } returns CopyOnWriteArrayList()
        ActionUtil.doRejectChallenge(job, "", "reason").kind shouldBe FormValidation.Kind.ERROR

        mockkStatic(ActionUtil::class)
        every { ActionUtil.generateChallengeAfterRejection(any(), any(), any(), any()) } returns ""
        every { userProperty.getCurrentChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))
        ActionUtil.doRejectChallenge(job, stringChallenge, "reason").kind shouldBe FormValidation.Kind.OK
    }

    @Test
    fun doStoreChallenge() {
        ActionUtil.doStoreChallenge(job, "").kind shouldBe FormValidation.Kind.ERROR

        every { User.current() } returns null
        ActionUtil.doStoreChallenge(job, "").kind shouldBe FormValidation.Kind.ERROR

        every { User.current() } returns user
        every { user.getProperty(GameUserProperty::class.java) } returns null
        ActionUtil.doStoreChallenge(job, "").kind shouldBe FormValidation.Kind.ERROR

        every { user.getProperty(GameUserProperty::class.java) } returns userProperty
        every { userProperty.getCurrentChallenges(any()) } returns CopyOnWriteArrayList()
        ActionUtil.doStoreChallenge(job, "").kind shouldBe FormValidation.Kind.ERROR

        mockkStatic(ActionUtil::class)
        every { ActionUtil.generateChallengeAfterRejection(any(), any(), any(), any()) } returns ""
        every { userProperty.getCurrentChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))
        every { userProperty.getStoredChallenges(any()).size } returns 0
        val job = mockkClass(AbstractProject::class)
        every { job.fullName } returns "test-project"
        every { job.save() } returns Unit
        every { job.getProperty(GameJobProperty::class.java).currentStoredChallengesCount } returns 1
        ActionUtil.doStoreChallenge(job, stringChallenge).kind shouldBe FormValidation.Kind.OK
    }

    @Test
    fun doUndoStoreChallenge() {
        every { userProperty.getStoredChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))

        ActionUtil.doUndoStoreChallenge(job, "").kind shouldBe FormValidation.Kind.ERROR

        every { User.current() } returns null
        ActionUtil.doUndoStoreChallenge(job, "").kind shouldBe FormValidation.Kind.ERROR

        every { User.current() } returns user
        every { user.getProperty(GameUserProperty::class.java) } returns null
        ActionUtil.doUndoStoreChallenge(job, "").kind shouldBe FormValidation.Kind.ERROR

        every { user.getProperty(GameUserProperty::class.java) } returns userProperty
        every { userProperty.getCurrentChallenges(any()) } returns CopyOnWriteArrayList()
        ActionUtil.doUndoStoreChallenge(job, "").kind shouldBe FormValidation.Kind.ERROR

        ActionUtil.doUndoStoreChallenge(job, stringChallenge).kind shouldBe FormValidation.Kind.OK
    }

    @Test
    fun generateChallengeAfterRejection() {
        mockkStatic(ActionUtil::class)
        mockkStatic(User::class)
        every { User.getAll() } returns listOf()
        every { challenge.getParameters() } returns parameters

        parameters.workspace = FilePath(null, "/home/1241352356/branch1")
        ActionUtil.generateChallengeAfterRejection(challenge, user, userProperty, job) shouldBe
                ": No additional Challenge generated (Workspace deleted or on remote machine)"

        parameters.branch = "branch1"
        ActionUtil.generateChallengeAfterRejection(challenge, user, userProperty, job) shouldBe
                ": No additional Challenge generated (Workspace deleted or on remote machine)"

        mockkStatic(PublisherUtil::class)
        every { PublisherUtil.retrieveLastChangedClasses(any(), any(), any()) } returns arrayListOf()
        parameters.workspace = FilePath(null, root)
        parameters.projectName = "test-project"
        ActionUtil.generateChallengeAfterRejection(challenge, user, userProperty, job) shouldBe
                ": New Challenge generated"

        mockkStatic(ChallengeFactory::class)
        every { ChallengeFactory.generateNewChallenges(any(), any(), any(), any(), any()) } returns 1
        val job1 = mockkClass(Job::class)
        val jobProperty1 = mockkClass(GameJobProperty::class)
        val statistics = mockkClass(Statistics::class)
        every { job1.getProperty(any()) } returns jobProperty1
        every { jobProperty1.getStatistics() } returns statistics
        every { statistics.addGeneratedAfterRejection(any(), any()) } returns Unit
        every { PublisherUtil.retrieveLastChangedClasses(any(), any(), any()) } returns
                arrayListOf(mockkClass(SourceFileDetails::class))
        ActionUtil.generateChallengeAfterRejection(challenge, user, userProperty, job1) shouldBe
                ": New Challenge generated"
    }

    @Test
    fun doSendChallenge()
    {
        every { userProperty.getStoredChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))

        ActionUtil.doSendChallenge(job, "", "").kind shouldBe FormValidation.Kind.ERROR

        every { User.current() } returns null
        ActionUtil.doSendChallenge(job, "", "").kind shouldBe FormValidation.Kind.ERROR

        every { User.current() } returns user
        every { user.getProperty(GameUserProperty::class.java) } returns null
        ActionUtil.doSendChallenge(job, "", "").kind shouldBe FormValidation.Kind.ERROR

        every { user.getProperty(GameUserProperty::class.java) } returns userProperty
        every { User.get("User1", false, any()) } returns null
        every { User.get("User0", false, any()) } returns user

        ActionUtil.doSendChallenge(job, stringChallenge, "User1").kind shouldBe FormValidation.Kind.ERROR

        ActionUtil.doSendChallenge(job, stringChallenge, "User0").kind shouldBe FormValidation.Kind.ERROR

        val user1 = mockkClass(User::class)
        every { User.get("User1", false, any()) } returns user1
        every { user1.save() } returns Unit
        every { user1.getProperty(GameUserProperty::class.java) } returns null
        ActionUtil.doSendChallenge(job, stringChallenge, "User1").kind shouldBe FormValidation.Kind.ERROR

        val userProperty1 = mockkClass(GameUserProperty::class)
        every { user1.getProperty(GameUserProperty::class.java) } returns userProperty1
        every { userProperty1.getStoredChallenges(any()).size } returns 1
        val job = mockkClass(AbstractProject::class)
        every { job.fullName } returns "test-project"
        every { job.save() } returns Unit
        every { job.getProperty(GameJobProperty::class.java).currentStoredChallengesCount } returns 1

        ActionUtil.doSendChallenge(job, stringChallenge, "User1").kind shouldBe FormValidation.Kind.ERROR

        every { userProperty1.getStoredChallenges(any()).size } returns 0
        every { userProperty.removeStoredChallenge(any(), any()) } returns Unit
        every { userProperty1.addStoredChallenge(any(), any()) } returns Unit
        ActionUtil.doSendChallenge(job, stringChallenge, "User1").kind shouldBe FormValidation.Kind.OK

    }
}