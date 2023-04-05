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

import hudson.model.Job
import hudson.model.User
import hudson.security.HudsonPrivateSecurityRealm.Details
import hudson.util.FormValidation
import org.gamekins.GameUserPropertyDescriptor
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll

class PropertyUtilTest : FeatureSpec({

    val job1 = mockkClass(Job::class)
    val job2 = mockkClass(Job::class)
    val property1 = mockkClass(org.gamekins.property.GameJobProperty::class)
    val property2 = mockkClass(org.gamekins.property.GameJobProperty::class)
    val user1 = mockkClass(User::class)
    val user2 = mockkClass(User::class)
    val user3 = mockkClass(User::class)
    val userProperty1 = mockkClass(org.gamekins.GameUserProperty::class)
    val detailsProperty = mockkClass(Details::class)
    val team1 = "Team1"
    val team2 = "Team2"
    val noTeam = "No Team"
    val userName1 = "User1"
    val userName2 = "User2"
    val userName3 = "User3"
    val id1 = "ID1"
    val id2 = "ID2"
    val id3 = "ID3"
    val projectName1 = "Project1"
    val projectName2 = "Project2"

    beforeSpec {
        every { job1.fullName } returns projectName1
        every { job2.fullName } returns projectName2

        every { property1.addTeam(any()) } returns Unit
        every { property2.addTeam(any()) } returns Unit
        every { property1.getTeams() } returns arrayListOf(team1, team2, noTeam)
        every { property2.getTeams() } returns arrayListOf(team1, noTeam)
        every { property1.removeTeam(any()) } returns Unit
        every { property2.removeTeam(any()) } returns Unit
        every { property1.resetStatistics(any()) } returns Unit
        every { property2.resetStatistics(any()) } returns Unit

        every { user1.fullName } returns userName1
        every { user2.fullName } returns userName2
        every { user3.fullName } returns userName3
        every { user1.id } returns id1
        every { user2.id } returns id2
        every { user3.id } returns id3
        every { user1.getProperty(org.gamekins.GameUserProperty::class.java) } returns userProperty1
        every { user2.getProperty(org.gamekins.GameUserProperty::class.java) } returns null
        every { user3.getProperty(org.gamekins.GameUserProperty::class.java) } returns null
        every { user1.getProperty(Details::class.java) } returns detailsProperty
        every { user2.getProperty(Details::class.java) } returns detailsProperty
        every { user3.getProperty(Details::class.java) } returns null
        every { user1.save() } returns Unit
        every { user1.properties } returns mapOf(mockkClass(Details.DescriptorImpl::class)
                to mockkClass(Details::class))
        every { user2.properties } returns mapOf(mockkClass(Details.DescriptorImpl::class)
                to mockkClass(Details::class))
        every { user3.properties } returns mapOf(mockkClass(Details.DescriptorImpl::class)
                to mockkClass(Details::class))

        every { userProperty1.getTeamName(projectName2) } returns team1
        every { userProperty1.isParticipating(projectName1) } returns false
        every { userProperty1.isParticipating(projectName2) } returns true
        every { userProperty1.isParticipating(projectName1, team1) } returns false
        every { userProperty1.isParticipating(projectName1, team2) } returns true
        every { userProperty1.isParticipating(projectName2, team1) } returns false
        every { userProperty1.isParticipating(projectName1, noTeam) } returns false
        every { userProperty1.isParticipating(projectName2, noTeam) } returns false
        every { userProperty1.setParticipating(any(), any()) } returns Unit
        every { userProperty1.removeParticipation(any()) } returns Unit
        every { userProperty1.reset(any()) } returns Unit

        mockkStatic(User::class)
        every { User.getAll() } returns listOf(user1, user2, user3)
    }

    afterSpec {
        unmockkAll()
    }

    feature("doAddTeam") {
        var formValidation: FormValidation

        scenario("No Teamname given") {
            formValidation = PropertyUtil.doAddTeam(property2, " ")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.NO_TEAM
        }

        scenario("No property given") {
            formValidation = PropertyUtil.doAddTeam(null, team1)
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.UNEXPECTED
        }

        scenario("Teamname already in use") {
            formValidation = PropertyUtil.doAddTeam(property2, team1)
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.TEAM_NAME_TAKEN
        }

        scenario("Successful Action") {
            PropertyUtil.doAddTeam(property2, team2).kind shouldBe FormValidation.Kind.OK
        }
    }

    feature("doAddUserToTeam") {
        var formValidation : FormValidation

        scenario("No Teamname specified") {
            formValidation = PropertyUtil.doAddUserToTeam(job2, " ", "")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.NO_TEAM
        }

        scenario("No parent job given") {
            formValidation = PropertyUtil.doAddUserToTeam(null, team1, "")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.PARENT
        }

        scenario("User is already in a team") {
            formValidation = PropertyUtil.doAddUserToTeam(job2, team1, "$id3 ($userName3)")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.USER_ALREADY_IN_TEAM
        }

        scenario("User is already in a team") {
            formValidation = PropertyUtil.doAddUserToTeam(job2, team1, "$id2 ($userName2)")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.USER_ALREADY_IN_TEAM
        }

        scenario("User already in a team") {
            formValidation = PropertyUtil.doAddUserToTeam(job2, team1, "$id1 ($userName1)")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.USER_ALREADY_IN_TEAM
        }

        scenario("Specified User does not exist") {
            formValidation = PropertyUtil.doAddUserToTeam(job2, team1, "")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.UNKNOWN_USER
        }

        scenario("Successful Action") {
            PropertyUtil.doAddUserToTeam(job1, team1, "$id1 ($userName1)").kind shouldBe FormValidation.Kind.OK
        }
    }

    feature("doDeleteTeam") {
        var formValidation : FormValidation

        scenario("No Team specified") {
            formValidation = PropertyUtil.doDeleteTeam(projectName1, property2, " ")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.NO_TEAM
        }

        scenario("No Parent Property") {
            formValidation = PropertyUtil.doDeleteTeam(projectName1, null, team1)
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.UNEXPECTED
        }

        scenario("Successful Action") {
            PropertyUtil.doDeleteTeam(projectName1, property2, team1).kind shouldBe FormValidation.Kind.OK
        }

        scenario("Successful Action") {
            PropertyUtil.doDeleteTeam(projectName1, property1, team2).kind shouldBe FormValidation.Kind.OK
        }
    }

    feature("doFillTeamsBoxItems") {
        scenario("Include Pseudo-Team")
        {
            PropertyUtil.doFillTeamsBoxItems(property1, false).size shouldBe 2
        }
        scenario("Do not include Pseudo-Team")
        {
            PropertyUtil.doFillTeamsBoxItems(property1, true).size shouldBe 3
        }
    }

    feature("doFillUsersBoxItems") {
        scenario("Project1")
        {
            PropertyUtil.doFillUsersBoxItems(projectName1).size shouldBe 1
        }

        scenario("Project2")
        {
            PropertyUtil.doFillUsersBoxItems(projectName2).size shouldBe 1
        }
    }

    feature("doRemoveUserFromTeam") {
        var formValidation : FormValidation

        scenario("No Team specified") {
            formValidation = PropertyUtil.doRemoveUserFromTeam(job2, " ", "")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.NO_TEAM
        }

        scenario("No parent job given") {
            formValidation = PropertyUtil.doRemoveUserFromTeam(null, team1, "")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.PARENT
        }

        scenario("User already not in Team") {
            formValidation = PropertyUtil.doRemoveUserFromTeam(job2, team1, "$id3 ($userName3)")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.USER_NOT_IN_TEAM
        }

        scenario("User already not in Team") {
            formValidation = PropertyUtil.doRemoveUserFromTeam(job2, team1, "$id2 ($userName2)")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.USER_NOT_IN_TEAM
        }

        scenario("User already not in Team") {
            formValidation = PropertyUtil.doRemoveUserFromTeam(job2, team1, "$id1 ($userName1)")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.USER_NOT_IN_TEAM
        }

        scenario("User does not exist") {
            formValidation = PropertyUtil.doRemoveUserFromTeam(job2, team1, "")
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.UNKNOWN_USER
        }

        scenario("Successful Action") {
            PropertyUtil.doRemoveUserFromTeam(job1, team2, "$id1 ($userName1)").kind shouldBe FormValidation.Kind.OK
        }
    }

    feature("doReset") {
        var formValidation : FormValidation

        scenario("No parent job given") {
            formValidation = PropertyUtil.doReset(null, null)
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.PARENT
        }

        scenario("Parent job has no property") {
            formValidation = PropertyUtil.doReset(job1, null)
            formValidation.kind shouldBe FormValidation.Kind.ERROR
            formValidation.message shouldBe Constants.Error.PARENT_WITHOUT_PROPERTY
        }

        scenario("Successful Action") {
            PropertyUtil.doReset(job1, property1).kind shouldBe FormValidation.Kind.OK
        }

        scenario("Successful Action") {
            PropertyUtil.doReset(job2, property2).kind shouldBe FormValidation.Kind.OK
        }
    }

    feature("doShowTeamMemberships") {
        scenario("Test on Job1") {
            PropertyUtil.doShowTeamMemberships(job1, property1) shouldBe "{\"No Team\":[],\"Team2\":[],\"Team1\":[]}"
        }

        scenario("Test on Job2") {
            PropertyUtil.doShowTeamMemberships(job2, property2) shouldBe "{\"No Team\":[],\"Team1\":[\"User1\"]}"
        }
    }

    feature("realUser") {
        scenario("Successful Action") {
            PropertyUtil.realUser(user1) shouldBe true
        }

        scenario("User without Login information") {
        val user4 = mockkClass(User::class)
            every { user4.properties } returns mapOf(mockkClass(GameUserPropertyDescriptor::class)
                    to mockkClass(org.gamekins.GameUserProperty::class))
            PropertyUtil.realUser(user4) shouldBe false
        }
    }

    //Does not make sense to test JacocoUtil.reconfigure(), since a real Jenkins would be needed for it.
})