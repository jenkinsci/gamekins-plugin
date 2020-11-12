package io.jenkins.plugins.gamekins.util

import hudson.model.Job
import hudson.model.User
import hudson.security.HudsonPrivateSecurityRealm.Details
import hudson.util.FormValidation
import io.jenkins.plugins.gamekins.GameUserPropertyDescriptor
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic

class PropertyUtilTest : AnnotationSpec() {

    private val job1 = mockkClass(Job::class)
    private val job2 = mockkClass(Job::class)
    private val property1 = mockkClass(io.jenkins.plugins.gamekins.property.GameJobProperty::class)
    private val property2 = mockkClass(io.jenkins.plugins.gamekins.property.GameJobProperty::class)
    private val user1 = mockkClass(User::class)
    private val user2 = mockkClass(User::class)
    private val user3 = mockkClass(User::class)
    private val userProperty1 = mockkClass(io.jenkins.plugins.gamekins.GameUserProperty::class)
    private val detailsProperty = mockkClass(Details::class)
    private val team1 = "Team1"
    private val team2 = "Team2"
    private val userName1 = "User1"
    private val userName2 = "User2"
    private val userName3 = "User3"
    private val projectName1 = "Project1"
    private val projectName2 = "Project2"

    @BeforeAll
    fun initAll() {
        every { job1.name } returns projectName1
        every { job2.name } returns projectName2

        every { property1.addTeam(any()) } returns Unit
        every { property2.addTeam(any()) } returns Unit
        every { property1.getTeams() } returns arrayListOf(team1, team2)
        every { property2.getTeams() } returns arrayListOf(team1)
        every { property1.removeTeam(any()) } returns Unit
        every { property2.removeTeam(any()) } returns Unit
        every { property1.resetStatistics(any()) } returns Unit
        every { property2.resetStatistics(any()) } returns Unit

        every { user1.fullName } returns userName1
        every { user2.fullName } returns userName2
        every { user3.fullName } returns userName3
        every { user1.getProperty(io.jenkins.plugins.gamekins.GameUserProperty::class.java) } returns userProperty1
        every { user2.getProperty(io.jenkins.plugins.gamekins.GameUserProperty::class.java) } returns null
        every { user3.getProperty(io.jenkins.plugins.gamekins.GameUserProperty::class.java) } returns null
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

        every { userProperty1.isParticipating(projectName1) } returns false
        every { userProperty1.isParticipating(projectName2) } returns true
        every { userProperty1.isParticipating(projectName1, team1) } returns false
        every { userProperty1.isParticipating(projectName1, team2) } returns true
        every { userProperty1.isParticipating(projectName2, team1) } returns false
        every { userProperty1.setParticipating(any(), any()) } returns Unit
        every { userProperty1.removeParticipation(any()) } returns Unit
        every { userProperty1.reset(any()) } returns Unit

        mockkStatic(User::class)
        every { User.getAll() } returns listOf(user1, user2, user3)
    }

    @Test
    fun doAddTeam() {
        PropertyUtil.doAddTeam(property2, " ").kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doAddTeam(null, team1).kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doAddTeam(property2, team1).kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doAddTeam(property2, team2).kind shouldBe FormValidation.Kind.OK
    }

    @Test
    fun doAddUserToTeam() {
        PropertyUtil.doAddUserToTeam(job2, " ", "").kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doAddUserToTeam(null, team1, "").kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doAddUserToTeam(job2, team1, userName3).kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doAddUserToTeam(job2, team1, userName2).kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doAddUserToTeam(job2, team1, userName1).kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doAddUserToTeam(job1, team1, userName1).kind shouldBe FormValidation.Kind.OK
    }

    @Test
    fun doDeleteTeam() {
        PropertyUtil.doDeleteTeam(projectName1, property2, " ").kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doDeleteTeam(projectName1, null, team1).kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doDeleteTeam(projectName1, property2, team1).kind shouldBe FormValidation.Kind.OK

        PropertyUtil.doDeleteTeam(projectName1, property1, team2).kind shouldBe FormValidation.Kind.OK
    }

    @Test
    fun doFillTeamsBoxItems() {
        PropertyUtil.doFillTeamsBoxItems(property1).size shouldBe 2
    }

    @Test
    fun doFillUsersBoxItems() {
        PropertyUtil.doFillUsersBoxItems(projectName1).size shouldBe 1

        PropertyUtil.doFillUsersBoxItems(projectName2).size shouldBe 1
    }

    @Test
    fun doRemoveUserFromTeam() {
        PropertyUtil.doRemoveUserFromTeam(job2, " ", "").kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doRemoveUserFromTeam(null, team1, "").kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doRemoveUserFromTeam(job2, team1, userName3).kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doRemoveUserFromTeam(job2, team1, userName2).kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doRemoveUserFromTeam(job2, team1, userName1).kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doRemoveUserFromTeam(job1, team2, userName1).kind shouldBe FormValidation.Kind.OK
    }

    @Test
    fun doReset() {
        PropertyUtil.doReset(null, null).kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doReset(job1, null).kind shouldBe FormValidation.Kind.ERROR

        PropertyUtil.doReset(job1, property1).kind shouldBe FormValidation.Kind.OK

        PropertyUtil.doReset(job2, property2).kind shouldBe FormValidation.Kind.OK
    }

    @Test
    fun realUser() {
        PropertyUtil.realUser(user1) shouldBe true

        val user4 = mockkClass(User::class)
        every { user4.properties } returns mapOf(mockkClass(GameUserPropertyDescriptor::class)
                to mockkClass(io.jenkins.plugins.gamekins.GameUserProperty::class))
        PropertyUtil.realUser(user4) shouldBe false
    }

    //Does not make sense to test JacocoUtil.reconfigure(), since a real Jenkins would be needed for it.
}