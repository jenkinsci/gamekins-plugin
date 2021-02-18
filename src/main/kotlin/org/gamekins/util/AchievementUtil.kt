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
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.GameUserProperty
import org.gamekins.challenge.BuildChallenge
import org.gamekins.challenge.CoverageChallenge
import org.gamekins.challenge.LineCoverageChallenge
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
     * Solves the achievements with description: Solve a LineCoverageChallenge with at least X branches in the
     * required line. Needs the key 'branches' in the map [additionalParameters] with a positive Int value.
     */
    fun coverLineWithXBranches(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                               run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener,
                               additionalParameters: HashMap<String, String>): Boolean {
        return property.getCompletedChallenges(constants["projectName"])
            .filterIsInstance<LineCoverageChallenge>()
            .any { it.getMaxCoveredBranchesIfFullyCovered() >= additionalParameters["branches"]?.toInt()
                    ?: Int.MAX_VALUE }
    }

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
     * Solves the achievements with description: Start a successful build with more than X minutes duration. Needs the
     * key 'more' in the map [additionalParameters] with a Boolean value and the key 'duration' with a Long value.
     */
    fun haveBuildWithXSeconds(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                              run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener,
                              additionalParameters: HashMap<String, String>): Boolean {
        if (additionalParameters["more"].isNullOrEmpty()) return false
        return if (additionalParameters["more"].toBoolean()) {
            run.duration > additionalParameters["duration"]?.toLong()?.times(1000) ?: Long.MAX_VALUE
        } else {
            run.duration < additionalParameters["duration"]?.toLong()?.times(1000) ?: 0
        }
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
     * Solves the achievements with description: Fail the build with X failed test. Needs the key 'failedTests'
     * in the map [additionalParameters] with a positive Int value. 'failedTests' with the value '0' means all tests.
     */
    fun haveXFailedTests(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                         run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener,
                         additionalParameters: HashMap<String, String>): Boolean {
        if (additionalParameters["failedTests"]?.toInt() == 0) {
            return JUnitUtil.getTestCount(workspace, run) == JUnitUtil.getTestFailCount(workspace, run)
        } else if (run.result == Result.FAILURE) {
            return JUnitUtil.getTestFailCount(workspace, run) == additionalParameters["failedTests"]?.toInt()
        }

        return false
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
     * Solves the achievements with description: Solve a Challenge a maximum of X hours after generation. Needs the
     * key 'timeDifference' in the map [additionalParameters] with a positive Long value.
     */
    fun solveChallengeInXSeconds(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                                 run: Run<*, *>, property: GameUserProperty, workspace: FilePath,
                                 listener: TaskListener, additionalParameters: HashMap<String, String>): Boolean {
        return property.getCompletedChallenges(constants["projectName"])
            .any { (it.getSolved() - it.getCreated()).div(1000) <=
                    additionalParameters["timeDifference"]?.toLong() ?: 0}
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
    @JvmStatic
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