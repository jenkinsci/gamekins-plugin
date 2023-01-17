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
import io.kotest.core.spec.style.FeatureSpec
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


class GitUtilTest : FeatureSpec({

    lateinit var root : String
    lateinit var path : FilePath
    val parameters = Parameters()
    val user = mockkClass(GitUtil.GameUser::class)
    val name = "Philipp Straubinger"
    val mail = "philipp.straubinger@uni-passau.de"
    val id = "philipp.straubinger"
    val headHash = "cf02809f4d3500d33c7bcb5120c1e9d11a95bd14"

    beforeSpec {
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory!!.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
        path = FilePath(null, root)

        parameters.projectName = "test-project"
        parameters.jacocoResultsPath = "**/target/site/jacoco/"
        parameters.jacocoCSVPath = "**/target/site/jacoco/jacoco.csv"
        parameters.workspace = path

        every { user.id } returns id
        every { user.fullName } returns name
        every { user.mail } returns mail
        every { user.gitNames } returns hashSetOf(id, name)

        mockkStatic(GitUtil::class)
    }

    afterSpec {
        unmockkAll()
        File(root).deleteRecursively()
    }

    feature("getBranch") {
        GitUtil.getBranch(path) shouldBe "master"
    }

    feature("getHead / HeadCommitCallable.call") {
        val builder = FileRepositoryBuilder()
        val repo = builder.setGitDir(File(path.remote + "/.git")).setMustExist(true).build()
        val head = GitUtil.getHead(repo)

        scenario("getHead")
        {
            head shouldNotBe null
            head.fullMessage shouldBe "UPDATE: Test for Complex\n"
            head.authorIdent.name shouldBe name
            head.name shouldBe headHash
        }

        scenario("HeadCommitCallable.call")
        {
            GitUtil.HeadCommitCallable(path.remote).call() shouldBe head
        }
    }

    feature("getLastChangedClasses") {
        scenario("Search last commits")
        {
            GitUtil.getLastChangedClasses("", parameters, TaskListener.NULL, arrayListOf(user)).size shouldBe 8
        }

        parameters.searchCommitCount = 1
        scenario("Search one commit")
        {
            GitUtil.getLastChangedClasses("", parameters, TaskListener.NULL, arrayListOf(user)) should beEmpty()
        }

        parameters.searchCommitCount = 50

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

        scenario("No Commit to search to specified")
        {
            GitUtil.getLastChangedClasses("", parameters, TaskListener.NULL, GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 8
        }

        scenario("Commit to search to is head")
        {
            GitUtil.getLastChangedClasses(headHash, parameters, TaskListener.NULL, GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 0
        }

        val firstCommit = "d3f574e28542876d4cd243c2ac730a6b9eed8b2c"
        scenario("Search to first commit")
        {
            //TODO: Should be 8, but see GitUtil.getDiffOfCommit()
            GitUtil.getLastChangedClasses(firstCommit, parameters, TaskListener.NULL, GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 7
        }

        val commitHash = "02c7398664cc9a15508a2d96c2b10f341f1fa4de"

        scenario("Search to specified commit")
        {
            GitUtil.getLastChangedClasses(commitHash, parameters, TaskListener.NULL, GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 3
        }
    }

    feature("getLastChangedFiles") {
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

        GitUtil.getLastChangedFiles("", parameters, arrayListOf(GitUtil.GameUser(user1)), TaskListener.NULL).size shouldBe 14
    }


    feature("getLastChangedTestsOfUser") {
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

        scenario("No commit to search to specified")
        {
            GitUtil.getLastChangedTestsOfUser("", parameters, TaskListener.NULL,  GitUtil.GameUser(user1), GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 4
        }

        scenario("Commit to search to is head")
        {
            GitUtil.getLastChangedTestsOfUser(headHash, parameters, TaskListener.NULL,  GitUtil.GameUser(user1), GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 0
        }

        val firstCommit = "d3f574e28542876d4cd243c2ac730a6b9eed8b2c"
        scenario("Search to first commit")
        {
            GitUtil.getLastChangedTestsOfUser(firstCommit, parameters, TaskListener.NULL,  GitUtil.GameUser(user1), GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 4
        }

        val commitHash = "da1e195773389f37ff5898b10e1708707e7208ac"
        scenario("Search to specified commit")
        {
            GitUtil.getLastChangedTestsOfUser(commitHash, parameters, TaskListener.NULL,  GitUtil.GameUser(user1), GitUtil.mapUsersToGameUsers(listOf(user1))).size shouldBe 3
        }
    }

    feature("mapGameUser") {
        val ident = mockkClass(PersonIdent::class)
        every { ident.name } returns name
        every { ident.emailAddress } returns mail

        scenario("No GameUsers to map to")
        {
            GitUtil.mapGameUser(ident, arrayListOf()) shouldBe null
        }

        val user1 = mockkClass(GitUtil.GameUser::class)
        every { user1.gitNames } returns hashSetOf()
        every { user1.fullName } returns ""
        every { user1.mail } returns ""
        scenario("Only GameUser in List is empty")
        {
            GitUtil.mapGameUser(ident, arrayListOf(user1)) shouldBe null
        }

        val user2 = mockkClass(GitUtil.GameUser::class)
        every { user2.gitNames } returns hashSetOf(name)
        every { user2.fullName } returns ""
        every { user2.mail } returns ""
        scenario("Searched GameUser has git name")
        {
            GitUtil.mapGameUser(ident, arrayListOf(user1, user2)) shouldBe user2
        }

        val user3 = mockkClass(GitUtil.GameUser::class)
        every { user3.gitNames } returns hashSetOf()
        every { user3.fullName } returns name
        every { user3.mail } returns ""
        scenario("Searched GameUser has full name")
        {
            GitUtil.mapGameUser(ident, arrayListOf(user1, user3)) shouldBe user3
        }

        val user4 = mockkClass(GitUtil.GameUser::class)
        every { user4.gitNames } returns hashSetOf()
        every { user4.fullName } returns ""
        every { user4.mail } returns mail
        scenario("Searched GameUser has mail address")
        {
            GitUtil.mapGameUser(ident, arrayListOf(user1, user4)) shouldBe user4
        }
    }

    feature("mapUser") {
        val ident = mockkClass(PersonIdent::class)
        every { ident.name } returns name
        every { ident.emailAddress } returns mail

        scenario("No Users to map to")
        {
            GitUtil.mapUser(ident, listOf()) shouldBe null
        }

        val user1 = mockkClass(hudson.model.User::class)
        every { user1.properties } returns mapOf()
        scenario("Only User has no properties")
        {
            GitUtil.mapUser(ident, listOf(user1)) shouldBe null
        }

        val user2 = mockkClass(hudson.model.User::class)
        every { user2.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        every { user2.getProperty(org.gamekins.GameUserProperty::class.java) } returns null
        every { user2.fullName } returns ""
        every { user2.getProperty(UserProperty::class.java) } returns null
        scenario("User is real User, but has no GameUserProperty")
        {
            GitUtil.mapUser(ident, listOf(user1, user2)) shouldBe null
        }

        val user3 = mockkClass(hudson.model.User::class)
        every { user3.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        val property3 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property3.getGitNames() } returns CopyOnWriteArraySet(listOf(name))
        every { user3.getProperty(org.gamekins.GameUserProperty::class.java) } returns property3
        every { user3.fullName } returns ""
        every { user3.getProperty(UserProperty::class.java) } returns null
        scenario("Searched user has git name")
        {
            GitUtil.mapUser(ident, listOf(user1, user3)) shouldBe user3
        }

        val user4 = mockkClass(hudson.model.User::class)
        every { user4.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        val property4 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property4.getGitNames() } returns CopyOnWriteArraySet(listOf())
        every { user4.getProperty(org.gamekins.GameUserProperty::class.java) } returns property4
        every { user4.fullName } returns name
        every { user4.getProperty(UserProperty::class.java) } returns null
        scenario("Searched user has full name")
        {
            GitUtil.mapUser(ident, listOf(user1, user4)) shouldBe user4
        }

        val user5 = mockkClass(hudson.model.User::class)
        every { user5.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        val property5 = mockkClass(org.gamekins.GameUserProperty::class)
        every { property5.getGitNames() } returns CopyOnWriteArraySet(listOf())
        every { user5.getProperty(org.gamekins.GameUserProperty::class.java) } returns property5
        every { user5.fullName } returns ""
        every { user5.getProperty(UserProperty::class.java) } returns null
        scenario("No User fits Ident")
        {
            GitUtil.mapUser(ident, listOf(user1, user5)) shouldBe null
        }

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
        scenario("Searched user has mail address")
        {
            GitUtil.mapUser(ident, listOf(user1, user6)) shouldBe user6
        }

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
        scenario("User has UserProperty, but it is empty")
        {
            GitUtil.mapUser(ident, listOf(user1, user7)) shouldBe null
        }
    }

    feature("mapUsersToGameUsers") {
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

    feature("GameUser") {
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

        scenario("Should be equal self")
        {
            (gameUser1 == gameUser1) shouldBe true
        }
        scenario("Should not be equal to null")
        {
            gameUser1.equals(null) shouldBe false
        }
        scenario("Should not be equal to object of other class")
        {
            gameUser1.equals(user1) shouldBe false
        }
        scenario("Should be equal to object with identical values")
        {
            (gameUser1 == user) shouldBe true
        }

        mockkStatic(User::class)
        every { User.getAll() } returns listOf(user1)
        scenario("Find affiliated User")
        {
            gameUser1.getUser() shouldBe user1
        }

        every { user1.properties } returns mapOf()
        scenario("Affiliated User not real User")
        {
            gameUser1.getUser() shouldBe null
        }

        every { user1.properties } returns mapOf(
                mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details.DescriptorImpl::class) to
                        mockkClass(hudson.security.HudsonPrivateSecurityRealm.Details::class))
        every { user1.id } returns "sample"
        scenario("No affiliated user found")
        {
            gameUser1.getUser() shouldBe null
        }
    }

    feature("DiffFromHeadCallable") {
        unmockkAll()
        mockkObject(GitUtil)
        val temp = GitUtil.DiffFromHeadCallable(path,
            "123", "org.example")
        every { GitUtil.getChangedClsSinceLastStoredCommit(path, "123",
            "org.example") } returns listOf("abc")
        temp.call() shouldBe listOf("abc")
    }

    feature("GetChangedClsSinceLastStoredCommit") {
        unmockkAll()
        scenario("Commit with specified ID exists")
        {
            GitUtil.getChangedClsSinceLastStoredCommit(path, "4a642f65855c8a6d28a1602258ebfde143df52e4",
                "org.example") shouldBe listOf()
        }

        scenario("No commit with specified ID exists")
        {
            GitUtil.getChangedClsSinceLastStoredCommit(path, "123",
                "org.example") shouldBe null
        }
    }
})