/*
 * Copyright 2023 Gamekins contributors
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

package org.gamekins.event

import hudson.model.FreeStyleProject
import hudson.model.ItemGroup
import hudson.model.Run
import hudson.model.User
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.achievement.Achievement
import org.gamekins.achievement.BadgeAchievement
import org.gamekins.achievement.ProgressAchievement
import org.gamekins.challenge.Challenge
import org.gamekins.event.build.BuildStartedEvent
import org.gamekins.event.user.*

class EventHandlerTest : FeatureSpec({

    val projectName = "test"
    val branch = "master"
    val user = mockkClass(User::class)
    val achievement = mockkClass(Achievement::class)
    val badgeAchievement = mockkClass(BadgeAchievement::class)
    val progressAchievement = mockkClass(ProgressAchievement::class)
    val challengeSolved = mockkClass(Challenge::class)
    val challengeUnsolvable = mockkClass(Challenge::class)
    val challengeGenerated = mockkClass(Challenge::class)
    val run = mockkClass(Run::class)
    val project = mockkClass(FreeStyleProject::class)
    val projectParent = mockkClass(ItemGroup::class)

    beforeSpec {
        mockkStatic(EventHandler::class)
        EventHandler.events.clear()
        every { user.fullName } returns "User"
        every { user.absoluteUrl } returns "http://localhost:8080/jenkins/user/user"
        //Numbers other than 0 trigger a bug (?) in MockK in EventHandler.generateMailText()
        every { run.getNumber() } returns 0
        every { run.parent } returns project
        every { project.absoluteUrl } returns "http://localhost:8080/jenkins/job/test/"
        every { project.parent } returns projectParent
        every { project.getProperty(any()) } returns null
        every { achievement.toString() } returns "Achievement"
	    every { achievement.title } returns "Achievement Title"
        every { badgeAchievement.toString() } returns "BadgeAchievement"
        every { progressAchievement.toString() } returns "ProgressAchievement"
        every { challengeSolved.toEscapedString() } returns "Challenge1"
        every { challengeUnsolvable.toEscapedString() } returns "Challenge2"
        every { challengeGenerated.toEscapedString() } returns "Challenge3"
    }

    afterSpec {
        unmockkAll()
    }

    feature("addEvent") {
        EventHandler.addEvent(AchievementSolvedEvent(projectName, branch, user, achievement))
        scenario("One Event happened")
        {
            EventHandler.events shouldHaveSize 1
        }

        EventHandler.addEvent(ChallengeSolvedEvent(projectName, branch, user, challengeSolved))
        scenario("Two Events happened")
        {
            EventHandler.events shouldHaveSize 2
        }

        EventHandler.addEvent(ChallengeUnsolvableEvent(projectName, branch, user, challengeUnsolvable))
        scenario("Three Events happened")
        {
            EventHandler.events shouldHaveSize 3
        }

        EventHandler.addEvent(ChallengeGeneratedEvent(projectName, branch, user, challengeGenerated))
        scenario("Four Events happened")
        {
            EventHandler.events shouldHaveSize 4
        }

        EventHandler.addEvent(BuildStartedEvent(projectName, branch, run, arrayListOf()))
        Thread.sleep(1000)
        scenario("Added BuildStartedEvent (removes other events)")
        {
            EventHandler.events shouldHaveSize 1
        }
    }

    feature("generateMailText") {
        val text = "Hello User,\n" +
                "\n" +
                "here are your Gamekins results from run 0 of project test:\n" +
                "\n" +
                "Challenge(s) solved:\n" +
                "- Challenge1\n" +
                "\n" +
                "New unsolvable Challenge(s):\n" +
                "- Challenge2\n" +
                "\n" +
                "Challenge(s) generated:\n" +
                "- Challenge3\n" +
                "\n" +
                "Achievement(s) solved:\n" +
                "- Achievement\n" +
                "\n" +
                "View the build on http://localhost:8080/jenkins/job/test/0/\n" +
                "View the leaderboard on http://localhost:8080/jenkins/job/test/leaderboard/\n" +
                "View your achievements on http://localhost:8080/jenkins/user/user/achievements/"
        val textEmpty = "Hello User,\n" +
                "\n" +
                "here are your Gamekins results from run 0 of project test:\n" +
                "\n" +
                "View the build on http://localhost:8080/jenkins/job/test/0/\n" +
                "View the leaderboard on http://localhost:8080/jenkins/job/test/leaderboard/\n" +
                "View your achievements on http://localhost:8080/jenkins/user/user/achievements/"

        scenario("Nothing has happened recently")
        {
            EventHandler.generateMailText(projectName, run, user, arrayListOf()) shouldBe textEmpty
        }

        val list = arrayListOf(
            AchievementSolvedEvent(projectName, branch, user, achievement),
            ChallengeSolvedEvent(projectName, branch, user, challengeSolved),
            ChallengeUnsolvableEvent(projectName, branch, user, challengeUnsolvable),
            ChallengeGeneratedEvent(projectName, branch, user, challengeGenerated)
        )
        every { user.id } returns "user"
        scenario("List of events happened recently")
        {
            EventHandler.generateMailText(projectName, run, user, list) shouldBe text
        }
    }
})
