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

package org.gamekins.achievement

import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockkStatic
import io.mockk.unmockkAll

class AchievementInitializerTest: FeatureSpec({

    beforeSpec {
        mockkStatic(AchievementInitializer::class)
    }

    afterSpec {
        unmockkAll()
    }

    feature("initializeAchievement") {
        val achievement = AchievementInitializer.initializeAchievement("solve_challenge.json")
        achievement.badgePath shouldBe "/plugin/gamekins/icons/trophy.png"
        achievement.description shouldBe "Solve your first Challenge"
        achievement.title shouldBe "I took the first Challenge"
        achievement.fullyQualifiedFunctionName shouldBe "org.gamekins.util.AchievementUtil::solveXChallenges"
        achievement.secret shouldBe false
        achievement.additionalParameters["solveNumber"] shouldBe "1"
    }

    feature("initializeAchievements") {
        AchievementInitializer.initializeAchievements("solve_x_challenges.json") shouldHaveSize 8
    }
})