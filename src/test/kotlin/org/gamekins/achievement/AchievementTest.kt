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

package org.gamekins.achievement

import hudson.model.Run
import hudson.model.TaskListener
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.GameUserProperty
import org.gamekins.file.FileDetails
import org.gamekins.util.AchievementUtil
import org.gamekins.util.Constants.Parameters

class AchievementTest: FeatureSpec({

    lateinit var achievement: Achievement
    val files = arrayListOf<FileDetails>()
    val parameters = Parameters()
    val run = mockkClass(Run::class)
    val property = mockkClass(GameUserProperty::class)

    beforeSpec {
        mockkStatic(AchievementInitializer::class)
        mockkStatic(AchievementUtil::class)

        achievement = AchievementInitializer.initializeAchievements("solve_x_challenges.json").first()
    }

    afterSpec {
        unmockkAll()
    }

    feature("clone") {
        achievement.clone() shouldBe achievement
    }

    feature("getSolvedTimeString") {
        scenario("Achievement after Initialization (not solved)")
        {
            achievement.solvedTimeString shouldBe "Not solved"
        }

        every { AchievementUtil.solveXChallenges(any(), any(), any(), any(), any(), any()) } returns true
        scenario("Achievement solved")
        {
            achievement.isSolved(files, parameters, run, property, TaskListener.NULL) shouldBe true
            achievement.solvedTimeString shouldNotBe "Not solved"
        }
    }

    feature("testEquals") {
        scenario("Null")
        {
            achievement.equals(null) shouldBe false
        }

        scenario("Different class")
        {
            achievement.equals(files) shouldBe false
        }

        val achievement2 = mockkClass(Achievement::class)
        every { achievement2.description } returns ""
        every { achievement2.title } returns ""
        scenario("No title, no description")
        {
            (achievement == achievement2) shouldBe false
        }

        every { achievement2.description } returns "Solve your first Challenge"
        scenario("No title")
        {
            (achievement == achievement2) shouldBe false
        }

        every { achievement2.description } returns ""
        every { achievement2.title } returns "I took the first Challenge"
        scenario("No description, same title")
        {
            (achievement == achievement2) shouldBe false
        }

        every { achievement2.description } returns "Solve your first Challenge"
        scenario("Same title, same description")
        {
            (achievement == achievement2) shouldBe true
        }
    }

    feature("isSolved") {
        every { AchievementUtil.solveXChallenges(any(), any(), any(), any(), any(), any()) } returns false
        scenario("Requirements not met")
        {
            achievement.isSolved(files, parameters, run, property, TaskListener.NULL) shouldBe false
        }

        every { AchievementUtil.solveXChallenges(any(), any(), any(), any(), any(), any()) } returns true
        scenario("Requirements met")
        {
            achievement.isSolved(files, parameters, run, property, TaskListener.NULL) shouldBe true
        }
    }

    feature("printToXML") {
        achievement.printToXML("") shouldMatch  "<Achievement title=\"I took the first Challenge\" " +
                "description=\"Solve your first Challenge\" secret=\"false\" solved=\"[0-9]*\"/>"
    }

    feature("testToString") {
        achievement.toString() shouldBe "I took the first Challenge: Solve your first Challenge"
    }
})