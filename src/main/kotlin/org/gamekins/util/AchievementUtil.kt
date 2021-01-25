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
import org.gamekins.challenge.CoverageChallenge
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
     * Returns the number of lines of code (LOC). Excludes blank lines and comments.
     */
    fun getLinesOfCode(file: FilePath): Int {
        val content = file.readToString().split("\n")

        return content
            .map { it.trim() }
            .count { it.isNotEmpty() && !it.startsWith("/") && !it.startsWith("*") }
    }

    /**
     * Solves the achievements with description: Solve a CoverageChallenge with at least X% coverage in the required
     * class. Needs the key 'haveCoverage' in the map [additionalParameters] with a positive Double value.
     */
    fun haveClassWithXCoverage(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                               run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener,
                               additionalParameters: HashMap<String, String>): Boolean {

        return property.getCompletedChallenges(constants["projectName"])
            .filterIsInstance<CoverageChallenge>()
            .any { it.solvedCoverage >= additionalParameters["haveCoverage"]?.toDouble() ?: Double.MAX_VALUE }
    }

    /**
     * Solves the achievements with description: Solve X CoverageChallenges with Y% coverage in the required class.
     * Needs the keys 'haveCoverage' and 'classesCount' in the map [additionalParameters] with a positive
     * Double/Int value.
     */
    fun haveXClassesWithYCoverage(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                                  run: Run<*, *>, property: GameUserProperty, workspace: FilePath,
                                  listener: TaskListener, additionalParameters: HashMap<String, String>): Boolean {

        return property.getCompletedChallenges(constants["projectName"])
            .filterIsInstance<CoverageChallenge>()
            .count { it.solvedCoverage >= additionalParameters["haveCoverage"]?.toDouble() ?: Double.MAX_VALUE } >=
                additionalParameters["classesCount"]?.toInt() ?: Int.MAX_VALUE
    }

    /**
     * Solves the achievements with description: Solve X CoverageChallenges with Y% coverage in the required class
     * with at least Z LOC. Needs the keys 'haveCoverage', 'classesCount' 'linesCount' and  in the map
     * [additionalParameters] with a positive Double/Int/Int value.
     */
    fun haveXClassesWithYCoverageAndZLines(classes: ArrayList<JacocoUtil.ClassDetails>,
                                           constants: HashMap<String, String>, run: Run<*, *>,
                                           property: GameUserProperty, workspace: FilePath, listener: TaskListener,
                                           additionalParameters: HashMap<String, String>): Boolean {

        val path = workspace.remote.removeSuffix("/")
        return property.getCompletedChallenges(constants["projectName"])
            .filterIsInstance<CoverageChallenge>()
            .filter { it.solvedCoverage >= additionalParameters["haveCoverage"]?.toDouble() ?: Double.MAX_VALUE }
            .count {
                getLinesOfCode(
                    FilePath(workspace.channel, path + it.classDetails.sourceFilePath)
                ) >= additionalParameters["linesCount"]?.toInt() ?: Int.MAX_VALUE
            } >= additionalParameters["classesCount"]?.toInt() ?: Int.MAX_VALUE
    }

    /**
     * Solves the achievements with description: Have X% project coverage. Needs the key 'haveCoverage'
     * in the map [additionalParameters] with a positive Double value.
     */
    fun haveXProjectCoverage(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                             run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener,
                             additionalParameters: HashMap<String, String>): Boolean {

        return constants["projectCoverage"]!!.toDouble() >=
                additionalParameters["haveCoverage"]?.toDouble() ?: Double.MAX_VALUE
    }

    /**
     * Solves the achievements with description: Have X tests in your project. Needs the key 'haveTests'
     * in the map [additionalParameters] with a positive Int value.
     */
    fun haveXProjectTests(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                             run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener,
                             additionalParameters: HashMap<String, String>): Boolean {

        return constants["projectTests"]!!.toInt() >=
                additionalParameters["haveTests"]?.toInt() ?: Int.MAX_VALUE
    }

    /**
     * Solves the achievement Fixing my own mistake: Let the build pass after it failed with one of
     * your commits as head.
     */
    fun solveFirstBuildFail(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                            run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener,
                            additionalParameters: HashMap<String, String>): Boolean {

        return property.getCompletedChallenges(constants["projectName"]).filterIsInstance<BuildChallenge>().isNotEmpty()
    }

    /**
     * Solves the achievements with description: Solve X Challenges. Needs the key 'solveNumber'
     * in the map [additionalParameters] with a positive Int value.
     */
    fun solveXChallenges(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                            run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener,
                            additionalParameters: HashMap<String, String>): Boolean {

        return property.getCompletedChallenges(constants["projectName"]).size >=
                additionalParameters["solveNumber"]?.toInt() ?: Int.MAX_VALUE
    }

    /**
     * Solves the achievements with description: Solve X Challenges with one Jenkins build. Needs the key 'solveNumber'
     * in the map [additionalParameters] with a positive Int value.
     */
    fun solveXAtOnce(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                        run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener,
                        additionalParameters: HashMap<String, String>): Boolean {

        return constants["solved"]?.toInt() ?: 0 >= additionalParameters["solveNumber"]?.toInt() ?: Int.MAX_VALUE
    }
}