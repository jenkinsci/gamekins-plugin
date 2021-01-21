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

package org.gamekins.util

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.GameUserProperty
import org.gamekins.challenge.BuildChallenge
import java.util.HashMap

/**
 * Object to check whether an achievement is solved or not.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
@Suppress("UNUSED_PARAMETER", "unused")
object AchievementUtil {

    /**
     * Solves the achievement Fixing my own mistake: Let the build pass after it failed with one of your commits as head
     */
    fun solveFirstBuildFail(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                            run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener)
    : Boolean {

        return property.getCompletedChallenges(constants["projectName"]).filterIsInstance<BuildChallenge>().isNotEmpty()
    }

    /**
     * Solve the achievement I took the first Challenge: Solve your first Challenge
     */
    fun solveFirstChallenge(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                            run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener)
    : Boolean {

        return property.getCompletedChallenges(constants["projectName"]).isNotEmpty()
    }

    /**
     * Solves the achievement Impossible Multitasker: Solve four Challenges with one Jenkins build
     */
    fun solveFourAtOnce(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                        run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener)
    : Boolean {

        return constants["solved"]?.toInt() ?: 0 >= 4
    }

    /**
     * Solves the achievement Multitasker: Solve three Challenges with one Jenkins build
     */
    fun solveThreeAtOnce(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                        run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener)
    : Boolean {

        return constants["solved"]?.toInt() ?: 0 >= 3
    }
}