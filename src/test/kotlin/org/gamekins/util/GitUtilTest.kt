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
import hudson.model.TaskListener
import hudson.model.User
import hudson.tasks.Mailer.UserProperty
import org.gamekins.test.TestUtils
import org.gamekins.util.Constants.Parameters
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.*
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.*
import java.util.concurrent.CopyOnWriteArraySet


class GitUtilTest : AnnotationSpec() {

    private lateinit var root : String
    private lateinit var path : FilePath
    private val parameters = Parameters()
    private val user = mockkClass(GitUtil.GameUser::class)
    private val name = "Philipp Straubinger"
    private val mail = "philipp.straubinger@uni-passau.de"
    private val id = "philipp.straubinger"
    private val headHash = "cf02809f4d3500d33c7bcb5120c1e9d11a95bd14"

    @BeforeAll
    fun initAll() {
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
        path = FilePath(null, root)

        parameters.projectName = "test-project"
        parameters.jacocoResultsPath = "**/target/site/jacoco/"
        parameters.jacocoCSVPath = "**/target/site/jacoco/jacoco.csv"
        parameters.mocoJSONPath = "**/target/site/moco/mutation/"
        parameters.workspace = path

        every { user.id } returns id
        every { user.fullName } returns name
        every { user.mail } returns mail
        every { user.gitNames } returns hashSetOf(id, name)

        mockkStatic(GitUtil::class)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
        File(root).deleteRecursively()
    }

    @Test
    fun getBranch() {
        GitUtil.getBranch(path) shouldBe "master"
    }

    @Test
    fun getHead() {
        val builder = FileRepositoryBuilder()
        val repo = builder.setGitDir(File(path.remote + "/.git")).setMustExist(true).build()
        val head = GitUtil.getHead(repo)
        head shouldNotBe null
        head.fullMessage shouldBe "UPDATE: Test for Complex\n"
        head.authorIdent.name shouldBe name
        head.name shouldBe headHash

        GitUtil.HeadCommitCallable(path.remote).call() shouldBe head
    }

    @Test
    fun getLastChangedClasses() {
        GitUtil.getLastChangedClasses(50, "", parameters, TaskListener.NULL, arrayListOf(user)).size shouldBe 8
        GitUtil.getLastChangedClasses(1, "", parameters, TaskListener.NULL, arrayListOf(user)) should beEmpty()
    }

    @Test
    fun getLastChangedFiles() {
        val user1 = mockkClass(hudson.model.User::class)
        every { user1.properties } returns mapOf(
            mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                    mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))

        val property1 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property1.getGitNames() } returns CopyOnWriteArraySet(listOf(name, id))
        every { user1.getProperty(org.gamekins.GameUserProperty::class.java) } returns property1
        every { user1.fullName } returns name
        every { user1.id } returns id
        val mailProperty1 = mockkClass(UserProperty::class)
        every { mailProperty1.address } returns mail
        every { user1.getProperty(UserProperty::class.java) } returns mailProperty1

        GitUtil.getLastChangedFiles(50, "", parameters, arrayListOf(GitUtil.GameUser(user1)), TaskListener.NULL).size shouldBe 14
    }

    @Test
    fun getLastChangedSourceFilesOfUser() {
        val user1 = mockkClass(hudson.model.User::class)
        every { user1.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))

        val property1 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property1.getGitNames() } returns CopyOnWriteArraySet(listOf(name, id))
        every { user1.getProperty(org.gamekins.GameUserProperty::class.java) } returns property1
        every { user1.fullName } returns name
        every { user1.id } returns id
        val mailProperty1 = mockkClass(UserProperty::class)
        every { mailProperty1.address } returns mail
        every { user1.getProperty(UserProperty::class.java) } returns mailProperty1

        GitUtil.getLastChangedClasses(50, "", parameters, TaskListener.NULL, GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 8
        GitUtil.getLastChangedClasses(50, headHash, parameters, TaskListener.NULL, GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 0

        val firstCommit = "d3f574e28542876d4cd243c2ac730a6b9eed8b2c"
        //TODO: Should be 8, but see GitUtil.getDiffOfCommit()
        GitUtil.getLastChangedClasses(50, firstCommit, parameters, TaskListener.NULL, GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 7

        val commitHash = "02c7398664cc9a15508a2d96c2b10f341f1fa4de"
        GitUtil.getLastChangedClasses(50, commitHash, parameters, TaskListener.NULL, GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 3
    }

    @Test
    fun getLastChangedTestsOfUser() {
        val user1 = mockkClass(hudson.model.User::class)
        every { user1.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        val property1 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property1.getGitNames() } returns CopyOnWriteArraySet(listOf(name, id))
        every { user1.getProperty(org.gamekins.GameUserProperty::class.java) } returns property1
        every { user1.fullName } returns name
        every { user1.id } returns id
        val mailProperty1 = mockkClass(UserProperty::class)
        every { mailProperty1.address } returns mail
        every { user1.getProperty(UserProperty::class.java) } returns mailProperty1

        GitUtil.getLastChangedTestsOfUser(50, "", parameters, TaskListener.NULL,  GitUtil.GameUser(user1), GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 4
        GitUtil.getLastChangedTestsOfUser(50, headHash, parameters, TaskListener.NULL,  GitUtil.GameUser(user1), GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 0

        val firstCommit = "d3f574e28542876d4cd243c2ac730a6b9eed8b2c"
        GitUtil.getLastChangedTestsOfUser(50, firstCommit, parameters, TaskListener.NULL,  GitUtil.GameUser(user1), GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 4

        val commitHash = "da1e195773389f37ff5898b10e1708707e7208ac"
        GitUtil.getLastChangedTestsOfUser(50, commitHash, parameters, TaskListener.NULL,  GitUtil.GameUser(user1), GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 3
    }

    @Test
    fun mapGameUser() {
        val ident = mockkClass(PersonIdent::class)
        every { ident.name } returns name
        every { ident.emailAddress } returns mail

        GitUtil.mapUser(ident, arrayListOf<GitUtil.GameUser>()) shouldBe null

        val user1 = mockkClass(GitUtil.GameUser::class)
        every { user1.gitNames } returns hashSetOf()
        every { user1.fullName } returns ""
        every { user1.mail } returns ""
        GitUtil.mapUser(ident, arrayListOf(user1)) shouldBe null

        val user2 = mockkClass(GitUtil.GameUser::class)
        every { user2.gitNames } returns hashSetOf(name)
        every { user2.fullName } returns ""
        every { user2.mail } returns ""
        GitUtil.mapUser(ident, arrayListOf(user1, user2)) shouldBe user2

        val user3 = mockkClass(GitUtil.GameUser::class)
        every { user3.gitNames } returns hashSetOf()
        every { user3.fullName } returns name
        every { user3.mail } returns ""
        GitUtil.mapUser(ident, arrayListOf(user1, user3)) shouldBe user3

        val user4 = mockkClass(GitUtil.GameUser::class)
        every { user4.gitNames } returns hashSetOf()
        every { user4.fullName } returns ""
        every { user4.mail } returns mail
        GitUtil.mapUser(ident, arrayListOf(user1, user4)) shouldBe user4
    }

    @Test
    fun mapUser() {
        val ident = mockkClass(PersonIdent::class)
        every { ident.name } returns name
        every { ident.emailAddress } returns mail

        GitUtil.mapUser(ident, listOf()) shouldBe null

        val user1 = mockkClass(hudson.model.User::class)
        every { user1.properties } returns mapOf()
        GitUtil.mapUser(ident, listOf(user1)) shouldBe null

        val user2 = mockkClass(hudson.model.User::class)
        every { user2.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        every { user2.getProperty(org.gamekins.GameUserProperty::class.java) } returns null
        every { user2.fullName } returns ""
        every { user2.getProperty(UserProperty::class.java) } returns null
        GitUtil.mapUser(ident, listOf(user1, user2)) shouldBe null

        val user3 = mockkClass(hudson.model.User::class)
        every { user3.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        val property3 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property3.getGitNames() } returns CopyOnWriteArraySet(listOf(name))
        every { user3.getProperty(org.gamekins.GameUserProperty::class.java) } returns property3
        every { user3.fullName } returns ""
        every { user3.getProperty(UserProperty::class.java) } returns null
        GitUtil.mapUser(ident, listOf(user1, user3)) shouldBe user3

        val user4 = mockkClass(hudson.model.User::class)
        every { user4.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        val property4 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property4.getGitNames() } returns CopyOnWriteArraySet(listOf())
        every { user4.getProperty(org.gamekins.GameUserProperty::class.java) } returns property4
        every { user4.fullName } returns name
        every { user4.getProperty(UserProperty::class.java) } returns null
        GitUtil.mapUser(ident, listOf(user1, user4)) shouldBe user4

        val user5 = mockkClass(hudson.model.User::class)
        every { user5.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        val property5 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property5.getGitNames() } returns CopyOnWriteArraySet(listOf())
        every { user5.getProperty(org.gamekins.GameUserProperty::class.java) } returns property5
        every { user5.fullName } returns ""
        every { user5.getProperty(UserProperty::class.java) } returns null
        GitUtil.mapUser(ident, listOf(user1, user5)) shouldBe null

        val user6 = mockkClass(hudson.model.User::class)
        every { user6.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        val property6 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property6.getGitNames() } returns CopyOnWriteArraySet(listOf())
        every { user6.getProperty(org.gamekins.GameUserProperty::class.java) } returns property6
        every { user6.fullName } returns ""
        val mailProperty6 = mockkClass(UserProperty::class)
        every { mailProperty6.address } returns mail
        every { user6.getProperty(UserProperty::class.java) } returns mailProperty6
        GitUtil.mapUser(ident, listOf(user1, user6)) shouldBe user6

        val user7 = mockkClass(hudson.model.User::class)
        every { user7.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        val property7 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property7.getGitNames() } returns CopyOnWriteArraySet(listOf())
        every { user7.getProperty(org.gamekins.GameUserProperty::class.java) } returns property7
        every { user7.fullName } returns ""
        val mailProperty7 = mockkClass(UserProperty::class)
        every { mailProperty7.address } returns ""
        every { user7.getProperty(UserProperty::class.java) } returns mailProperty7
        GitUtil.mapUser(ident, listOf(user1, user7)) shouldBe null
    }

    @Test
    fun mapUsersToGameUsers() {
        val user1 = mockkClass(hudson.model.User::class)
        every { user1.properties } returns mapOf()
        GitUtil.mapUsersToGameUsers(listOf(user1)) should beEmpty()

        val user2 = mockkClass(hudson.model.User::class)
        every { user2.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        val property2 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property2.getGitNames() } returns CopyOnWriteArraySet(listOf(name, id))
        every { user2.getProperty(org.gamekins.GameUserProperty::class.java) } returns property2
        every { user2.fullName } returns name
        every { user2.id } returns id
        val mailProperty2 = mockkClass(UserProperty::class)
        every { mailProperty2.address } returns mail
        every { user2.getProperty(UserProperty::class.java) } returns mailProperty2
        val users = GitUtil.mapUsersToGameUsers(listOf(user1, user2))
        users.size shouldBe 1
        users[0].id shouldBe id
        users[0].fullName shouldBe name
        users[0].mail shouldBe mail
        users[0].gitNames shouldBe hashSetOf(name, id)
    }

    @Test
    fun testGameUser() {
        val user1 = mockkClass(hudson.model.User::class)
        every { user1.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        val property1 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property1.getGitNames() } returns CopyOnWriteArraySet(listOf(name, id))
        every { user1.getProperty(org.gamekins.GameUserProperty::class.java) } returns property1
        every { user1.fullName } returns name
        every { user1.id } returns id
        val mailProperty1 = mockkClass(UserProperty::class)
        every { mailProperty1.address } returns mail
        every { user1.getProperty(UserProperty::class.java) } returns mailProperty1
        val gameUser1 = GitUtil.mapUsersToGameUsers(listOf(user1, user1))[0]

        (gameUser1 == gameUser1) shouldBe true
        gameUser1.equals(null) shouldBe false
        gameUser1.equals(user1) shouldBe false
        (gameUser1 == user) shouldBe true

        mockkStatic(User::class)
        every { User.getAll() } returns listOf(user1)
        gameUser1.getUser() shouldBe user1

        every { user1.properties } returns mapOf()
        gameUser1.getUser() shouldBe null

        every { user1.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        every { user1.id } returns "sample"
        gameUser1.getUser() shouldBe null
    }

    @Test
    fun testDiffFromHeadCallable() {
        unmockkAll()
        mockkObject(GitUtil)
        val temp = GitUtil.DiffFromHeadCallable(path,
            "123", "org.example")
        every { GitUtil.getChangedClsSinceLastStoredCommit(path, "123",
            "org.example") } returns listOf("abc")
        temp.call() shouldBe listOf("abc")
    }

    @Test
    fun testGetChangedClsSinceLastStoredCommit() {
        unmockkAll()
        GitUtil.getChangedClsSinceLastStoredCommit(path, "4a642f65855c8a6d28a1602258ebfde143df52e4",
            "org.example") shouldBe listOf()
        GitUtil.getChangedClsSinceLastStoredCommit(path, "123",
            "org.example") shouldBe null
    }
}