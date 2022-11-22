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
import hudson.tasks.Mailer
import hudson.util.FormValidation
import org.gamekins.GameUserProperty
import org.gamekins.challenge.ChallengeFactory
import org.gamekins.challenge.LineCoverageChallenge
import org.gamekins.property.GameJobProperty
import org.gamekins.statistics.Statistics
import org.gamekins.util.Constants.Parameters
import org.gamekins.test.TestUtils
import io.kotest.core.spec.style.FunSpec
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

class ActionUtilTest: FunSpec({

    val job = mockkClass(AbstractItem::class)
    val user = mockkClass(User::class)
    val userProperty = mockkClass(GameUserProperty::class)
    val challenge = mockkClass(LineCoverageChallenge::class)
    val stringChallenge = "LineCoverageChallenge"
    val parameters = Parameters()
    lateinit var root : String

    beforeSpec {
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

    afterSpec {
        unmockkAll()
        File(root).deleteRecursively()
    }

    context("Test Rejecting Challenges") {
        var formValidation : FormValidation

        test("No Reason given") {
            formValidation = ActionUtil.doRejectChallenge(job, "", "")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.NO_REASON
        }

        test("No User signed in") {
            every { User.current() } returns null
            formValidation = ActionUtil.doRejectChallenge(job, "", " ")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.NO_USER_SIGNED_IN
        }

        test("No Property") {
            every { User.current() } returns user
            every { user.getProperty(GameUserProperty::class.java) } returns null
            formValidation = ActionUtil.doRejectChallenge(job, "", "reason")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.RETRIEVING_PROPERTY
        }

        test("Reject challenge that does not exist") {
            every { user.getProperty(GameUserProperty::class.java) } returns userProperty
            every { userProperty.getCurrentChallenges(any()) } returns CopyOnWriteArrayList()
            formValidation = ActionUtil.doRejectChallenge(job, "", "reason")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.NO_CHALLENGE_EXISTS
        }

        test("Successful rejection") {
            mockkStatic(ActionUtil::class)
            every { ActionUtil.generateChallengeAfterRejection(any(), any(), any(), any()) } returns ""
            every { userProperty.getCurrentChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))
            ActionUtil.doRejectChallenge(job, stringChallenge, "reason").kind shouldBe FormValidation.Kind.OK
        }
    }

    context("Store Challenges") {
        var formValidation : FormValidation

        test("No User signed in") {
            every { User.current() } returns null
            formValidation = ActionUtil.doStoreChallenge(job, "")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.NO_USER_SIGNED_IN
        }

        test("No Property") {
            every { User.current() } returns user
            every { user.getProperty(GameUserProperty::class.java) } returns null
            formValidation = ActionUtil.doStoreChallenge(job, "")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.RETRIEVING_PROPERTY
        }

        test("Store Challenge that does not exist") {
            every { user.getProperty(GameUserProperty::class.java) } returns userProperty
            every { userProperty.getCurrentChallenges(any()) } returns CopyOnWriteArrayList()
            formValidation = ActionUtil.doStoreChallenge(job, "")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.NO_CHALLENGE_EXISTS
        }

        val job = mockkClass(AbstractProject::class)
        test("Storage Limit reached") {
            mockkStatic(ActionUtil::class)
            every { ActionUtil.generateChallengeAfterRejection(any(), any(), any(), any()) } returns ""
            every { userProperty.getCurrentChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))
            every { userProperty.getStoredChallenges(any()).size } returns 1
            every { job.fullName } returns "test-project"
            every { job.save() } returns Unit
            val gameProperty = mockkClass(GameJobProperty::class)
            every { gameProperty.currentStoredChallengesCount } returns 1
            every { job.getProperty(any()) } returns gameProperty
            formValidation = ActionUtil.doStoreChallenge(job, stringChallenge)
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.STORAGE_LIMIT
        }

        test("Successful Storage") {
            every { userProperty.getStoredChallenges(any()).size } returns 0
            ActionUtil.doStoreChallenge(job, stringChallenge).kind shouldBe FormValidation.Kind.OK
        }
    }

    test("Undo Store Challenges") {
        var formValidation : FormValidation

        every { User.current() } returns null
        formValidation = ActionUtil.doUndoStoreChallenge(job, "")
        formValidation.kind shouldBe FormValidation.Kind.ERROR
        formValidation.message shouldBe Constants.Error.NO_USER_SIGNED_IN

        every { User.current() } returns user
        every { user.getProperty(GameUserProperty::class.java) } returns null
        formValidation = ActionUtil.doUndoStoreChallenge(job, "")
        formValidation.kind shouldBe FormValidation.Kind.ERROR
        formValidation.message shouldBe Constants.Error.RETRIEVING_PROPERTY

        every { user.getProperty(GameUserProperty::class.java) } returns userProperty
        every { userProperty.getCurrentChallenges(any()) } returns CopyOnWriteArrayList()
        every { userProperty.getStoredChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))
        formValidation = ActionUtil.doUndoStoreChallenge(job, "")
        formValidation.kind shouldBe FormValidation.Kind.ERROR
        formValidation.message shouldBe Constants.Error.NO_CHALLENGE_EXISTS

        ActionUtil.doUndoStoreChallenge(job, stringChallenge).kind shouldBe FormValidation.Kind.OK
    }

    test("Generate Challenges after Rejection") {
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
        every { PublisherUtil.retrieveLastChangedClasses(any(), any()) } returns arrayListOf()
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
        every { PublisherUtil.retrieveLastChangedClasses(any(), any()) } returns
                arrayListOf(mockkClass(SourceFileDetails::class))
        ActionUtil.generateChallengeAfterRejection(challenge, user, userProperty, job1) shouldBe
                ": New Challenge generated"
    }

    test("Send Challenges") {
        var formValidation : FormValidation

        every { userProperty.getStoredChallenges(any()) } returns CopyOnWriteArrayList(listOf(challenge))

        every { User.current() } returns user
        formValidation = ActionUtil.doSendChallenge(job, "", "")
        formValidation.kind shouldBe FormValidation.Kind.ERROR
        formValidation.message shouldBe Constants.Error.NO_CHALLENGE_EXISTS

        every { User.current() } returns null
        formValidation = ActionUtil.doSendChallenge(job, "", "")
        formValidation.kind shouldBe FormValidation.Kind.ERROR
        formValidation.message shouldBe Constants.Error.NO_USER_SIGNED_IN

        every { User.current() } returns user
        every { user.getProperty(GameUserProperty::class.java) } returns null
        formValidation = ActionUtil.doSendChallenge(job, "", "")
        formValidation.kind shouldBe FormValidation.Kind.ERROR
        formValidation.message shouldBe Constants.Error.RETRIEVING_PROPERTY

        every { user.getProperty(GameUserProperty::class.java) } returns userProperty
        every { User.get("User1", false, any()) } returns null
        every { User.get("User0", false, any()) } returns user

        formValidation = ActionUtil.doSendChallenge(job, stringChallenge, "User1")
        formValidation.kind shouldBe FormValidation.Kind.ERROR
        formValidation.message shouldBe Constants.Error.USER_NOT_FOUND

        formValidation = ActionUtil.doSendChallenge(job, stringChallenge, "User0")
        formValidation.kind shouldBe FormValidation.Kind.ERROR
        formValidation.message shouldBe Constants.Error.RECEIVER_IS_SELF

        val user1 = mockkClass(User::class)
        every { User.get("User1", false, any()) } returns user1
        every { user1.save() } returns Unit
        every { user1.getProperty(GameUserProperty::class.java) } returns null
        formValidation = ActionUtil.doSendChallenge(job, stringChallenge, "User1")
        formValidation.kind shouldBe FormValidation.Kind.ERROR
        formValidation.message shouldBe Constants.Error.RETRIEVING_PROPERTY

        val userProperty1 = mockkClass(GameUserProperty::class)
        every { user1.getProperty(GameUserProperty::class.java) } returns userProperty1
        every { userProperty1.getStoredChallenges(any()).size } returns 1
        val job = mockkClass(AbstractProject::class)
        every { job.fullName } returns "test-project"
        every { job.save() } returns Unit
        val gameProperty = mockkClass(GameJobProperty::class)
        every { gameProperty.currentStoredChallengesCount } returns 1
        every { job.getProperty(any()) } returns gameProperty

        formValidation = ActionUtil.doSendChallenge(job, stringChallenge, "User1")
        formValidation.kind shouldBe FormValidation.Kind.ERROR
        formValidation.message shouldBe Constants.Error.STORAGE_LIMIT

        every { userProperty1.getStoredChallenges(any()).size } returns 0
        every { userProperty.removeStoredChallenge(any(), any()) } returns Unit
        every { userProperty1.addStoredChallenge(any(), any()) } returns Unit

        mockkStatic(Mailer::class)
        every { Mailer.descriptor() } returns null

        every { userProperty1.getNotifications() } returns false


        ActionUtil.doSendChallenge(job, stringChallenge, "User1").kind shouldBe FormValidation.Kind.OK

    }
})