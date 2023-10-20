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

package org.gamekins.util

import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.GameUserProperty
import org.gamekins.challenge.BranchCoverageChallenge
import org.gamekins.challenge.BuildChallenge
import org.gamekins.challenge.CoverageChallenge
import org.gamekins.file.FileDetails
import org.gamekins.util.Constants.Parameters
import java.util.HashMap
import kotlin.math.max

/**
 * Object to check whether an achievement is solved or not.
 *
 * @author Philipp Straubinger
 * @since 0.2
 */
@Suppress("UNUSED_PARAMETER", "unused")
object AchievementUtil {

    /**
     * Solves the achievements with description: Solve a LineCoverageChallenge with at least X branches in the
     * required line. Needs the key 'branches' in the map [additionalParameters] with a positive Int value.
     * Optional parameter is 'maxBranches' with a positive and exclusive Int value.
     */
    fun coverLineWithXBranches(classes: ArrayList<FileDetails>, parameters: Parameters,
                               run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                               additionalParameters: HashMap<String, String>): Boolean {
        return property.getCompletedChallenges(parameters.projectName)
            .filterIsInstance<BranchCoverageChallenge>()
            .any {
                (it.getMaxCoveredBranchesIfFullyCovered() >= (additionalParameters["branches"]?.toInt()
                    ?: Int.MAX_VALUE)
                        && it.getMaxCoveredBranchesIfFullyCovered() < (additionalParameters["maxBranches"]?.toInt()
                    ?: Int.MAX_VALUE))
            }
    }

    /**
     * Searches for a solved LineCoverageChallenge and returns its amount of covered branches.
     */
    fun getBranchesInLine(classes: ArrayList<FileDetails>, parameters: Parameters,
                       run: Run<*, *>, property: GameUserProperty, listener: TaskListener): Int {
        return property.getCompletedChallenges(parameters.projectName)
            .filterIsInstance<BranchCoverageChallenge>()
            .maxOfOrNull { it.getMaxCoveredBranchesIfFullyCovered() }?.toInt() ?: 0
    }

    /**
     * Returns the number of lines of code (LOC). Excludes blank lines and comments.
     */
    fun getLinesOfCode(file: FilePath): Int {
        if (!file.exists()) return 0
        val content = file.readToString().split("\n")

        return content
            .map { it.trim() }
            .count { it.isNotEmpty() && !it.startsWith("/") && !it.startsWith("*") }
    }

    /**
     * Solves the achievements with description: Start a successful build with more than X minutes duration. Needs the
     * key 'more' in the map [additionalParameters] with a Boolean value and the key 'duration' with a Long value.
     * Optional parameters are 'minDuration' and 'maxDuration', only one at a time and value exclusively.
     */
    fun haveBuildWithXSeconds(classes: ArrayList<FileDetails>, parameters: Parameters,
                              run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                              additionalParameters: HashMap<String, String>): Boolean {
        if (additionalParameters["more"].isNullOrEmpty()) return false
        if (run.result != Result.SUCCESS) return false
        val duration = if (run.duration != 0L) run.duration else
            max(0, System.currentTimeMillis() - run.startTimeInMillis)
        var retVal = if (additionalParameters["more"].toBoolean()) {
            duration > (additionalParameters["duration"]?.toLong()?.times(1000) ?: Long.MAX_VALUE)
        } else {
            duration < (additionalParameters["duration"]?.toLong()?.times(1000) ?: 0)
        }

        if (!additionalParameters["maxDuration"].isNullOrEmpty()) {
            retVal = retVal && duration < additionalParameters["maxDuration"]!!.toLong()
        } else if (!additionalParameters["minDuration"].isNullOrEmpty()) {
            retVal = retVal && duration > additionalParameters["minDuration"]!!.toLong()
        }

        return retVal
    }

    /**
     * Returns the duration of the build in seconds
     */
    fun getBuildDurationInSeconds(classes: ArrayList<FileDetails>, parameters: Parameters,
                                  run: Run<*, *>, property: GameUserProperty, listener: TaskListener): Long {
        return (if (run.duration != 0L) run.duration else
            max(0, System.currentTimeMillis() - run.startTimeInMillis)) / 1000
    }

    /**
     * Solves the achievements with description: Solve a CoverageChallenge with at least X% coverage in the required
     * class. Needs the key 'haveCoverage' in the map [additionalParameters] with a positive Double value.
     */
    fun haveClassWithXCoverage(classes: ArrayList<FileDetails>, parameters: Parameters,
                               run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                               additionalParameters: HashMap<String, String>): Boolean {

        return property.getCompletedChallenges(parameters.projectName)
            .filterIsInstance<CoverageChallenge>()
            .any { it.solvedCoverage >= (additionalParameters["haveCoverage"]?.toDouble() ?: Double.MAX_VALUE) }
    }

    /**
     * Returns the coverage of the most CoverageChallenge with the highest coverage as rounded percentage.
     */
    @JvmStatic
    fun getMaxClassCoverage(classes: ArrayList<FileDetails>, parameters: Parameters,
                            run: Run<*, *>, property: GameUserProperty, listener: TaskListener): Int {

        return property.getCompletedChallenges(parameters.projectName)
            .filterIsInstance<CoverageChallenge>()
            .maxOfOrNull { (it.solvedCoverage * 100) }?.toInt() ?: 0
    }

    /**
     * Solves the achievements with description: Solve X CoverageChallenges with Y% coverage in the required class.
     * Needs the keys 'haveCoverage' and 'classesCount' in the map [additionalParameters] with a positive
     * Double/Int value.
     */
    fun haveXClassesWithYCoverage(classes: ArrayList<FileDetails>, parameters: Parameters,
                                  run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                                  additionalParameters: HashMap<String, String>): Boolean {

        return property.getCompletedChallenges(parameters.projectName)
            .filterIsInstance<CoverageChallenge>()
            .count { it.solvedCoverage >= (additionalParameters["haveCoverage"]?.toDouble() ?: Double.MAX_VALUE) } >=
                (additionalParameters["classesCount"]?.toInt() ?: Int.MAX_VALUE)
    }

    /**
     * Solves the achievements with description: Solve X CoverageChallenges with Y% coverage in the required class
     * with at least Z LOC. Needs the keys 'haveCoverage', 'classesCount' 'linesCount' and  in the map
     * [additionalParameters] with a positive Double/Int/Int value.
     */
    fun haveXClassesWithYCoverageAndZLines(classes: ArrayList<FileDetails>, parameters: Parameters,
                                           run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                                           additionalParameters: HashMap<String, String>): Boolean {

        val path = parameters.remote.removeSuffix("/")
        return property.getCompletedChallenges(parameters.projectName)
            .filterIsInstance<CoverageChallenge>()
            .filter { it.solvedCoverage >= (additionalParameters["haveCoverage"]?.toDouble() ?: Double.MAX_VALUE) }
            .count {
                getLinesOfCode(
                    FilePath(parameters.workspace.channel, path + it.details.filePath)
                ) >= (additionalParameters["linesCount"]?.toInt() ?: Int.MAX_VALUE)
            } >= (additionalParameters["classesCount"]?.toInt() ?: Int.MAX_VALUE)
    }

    /**
     * Solves the achievements with description: Fail the build with X failed test. Needs the key 'failedTests'
     * in the map [additionalParameters] with a positive Int value. 'failedTests' with the value '0' means all tests.
     */
    fun haveXFailedTests(classes: ArrayList<FileDetails>, parameters: Parameters,
                         run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                         additionalParameters: HashMap<String, String>): Boolean {
        val testCount = JUnitUtil.getTestCount(parameters.workspace, run)
        if (additionalParameters["failedTests"]?.toInt() == 0 && testCount != 0) {
            return testCount == JUnitUtil.getTestFailCount(parameters.workspace, run)
        } else if (run.result == Result.FAILURE) {
            return JUnitUtil.getTestFailCount(parameters.workspace, run) == additionalParameters["failedTests"]?.toInt()
        }

        return false
    }

    /**
     * Solves the achievements with description: Have X% project coverage. Needs the key 'haveCoverage'
     * in the map [additionalParameters] with a positive Double value.
     */
    fun haveXProjectCoverage(classes: ArrayList<FileDetails>, parameters: Parameters,
                             run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                             additionalParameters: HashMap<String, String>): Boolean {

        return parameters.projectCoverage >= (additionalParameters["haveCoverage"]?.toDouble() ?: Double.MAX_VALUE)
    }

    /**
     * Returns the project coverage.
     */
    fun getProjectCoverage(classes: ArrayList<FileDetails>, parameters: Parameters,
                           run: Run<*, *>, property: GameUserProperty, listener: TaskListener): Int {

        return (parameters.projectCoverage * 100).toInt()
    }

    /**
     * Solves the achievements with description: Have X tests in your project. Needs the key 'haveTests'
     * in the map [additionalParameters] with a positive Int value.
     */
    fun haveXProjectTests(classes: ArrayList<FileDetails>, parameters: Parameters,
                          run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                          additionalParameters: HashMap<String, String>): Boolean {

        return parameters.projectTests >= (additionalParameters["haveTests"]?.toInt() ?: Int.MAX_VALUE)
    }

    /**
     * Returns the amount of tests in the project.
     */
    fun getProjectTestCount(classes: ArrayList<FileDetails>, parameters: Parameters,
                        run: Run<*, *>, property: GameUserProperty, listener: TaskListener): Int {

        return parameters.projectTests
    }

    /**
     * Solves the achievements with description: Improve the coverage of a class with a CoverageChallenge by X%.
     * Needs the key 'haveCoverage' in the map [additionalParameters] with a positive Double value.
     * Optional parameter 'maxCoverage' with a positive and exclusive Int value.
     */
    fun improveClassCoverageByX(classes: ArrayList<FileDetails>, parameters: Parameters,
                                run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                                additionalParameters: HashMap<String, String>): Boolean {
        return property.getCompletedChallenges(parameters.projectName)
            .filterIsInstance<CoverageChallenge>()
            .any {
                (it.solvedCoverage.toBigDecimal() - it.coverage.toBigDecimal() >=
                        (additionalParameters["haveCoverage"]?.toBigDecimal() ?: Double.MAX_VALUE.toBigDecimal())
                        && it.solvedCoverage.toBigDecimal() - it.coverage.toBigDecimal() <
                        (additionalParameters["maxCoverage"]?.toBigDecimal() ?: Double.MAX_VALUE.toBigDecimal()))
            }
    }

    /**
     * Solves the achievements with description: Improve the coverage of the project by X%. Needs the
     * key 'haveCoverage' in the map [additionalParameters] with a positive Double value.
     * Optional parameter 'maxCoverage' with a positive and exclusive Int value.
     */
    fun improveProjectCoverageByX(classes: ArrayList<FileDetails>, parameters: Parameters,
                                  run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                                  additionalParameters: HashMap<String, String>): Boolean {
        val mapUser: User? = GitUtil.mapUser(
            parameters.workspace.act(
                GitUtil.HeadCommitCallable(parameters.remote)
            ).authorIdent, User.getAll()
        )
        if (mapUser == property.getUser()) {
            val lastRun = PropertyUtil.retrieveGamePropertyFromRun(run)?.getStatistics()
                ?.getLastRun(parameters.branch)
            if (lastRun != null) {
                return (parameters.projectCoverage.toBigDecimal().minus(lastRun.coverage.toBigDecimal())
                        >= (additionalParameters["haveCoverage"]?.toBigDecimal() ?: Double.MAX_VALUE.toBigDecimal())
                        && parameters.projectCoverage.toBigDecimal().minus(lastRun.coverage.toBigDecimal())
                        < (additionalParameters["maxCoverage"]?.toBigDecimal() ?: Double.MAX_VALUE.toBigDecimal()))
            }
        }
        return false
    }

    /**
     * Solves the achievements with description: Solve a Challenge a maximum of X hours after generation. Needs the
     * key 'timeDifference' and 'minTimeDifference' in the map [additionalParameters] with a positive Long value.
     */
    fun solveChallengeInXSeconds(classes: ArrayList<FileDetails>, parameters: Parameters,
                                 run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                                 additionalParameters: HashMap<String, String>): Boolean {
        return property.getCompletedChallenges(parameters.projectName)
            .any {
                ((it.getSolved() - it.getCreated()).div(1000) <=
                        (additionalParameters["timeDifference"]?.toLong() ?: 0)
                        && (it.getSolved() - it.getCreated()).div(1000) >
                        (additionalParameters["minTimeDifference"]?.toLong() ?: Long.MAX_VALUE))
            }
    }

    /**
     * Solves the achievement Fixing my own mistake: Let the build pass after it failed with one of
     * your commits as head.
     */
    fun solveFirstBuildFail(classes: ArrayList<FileDetails>, parameters: Parameters,
                            run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                            additionalParameters: HashMap<String, String>): Boolean {

        return property.getCompletedChallenges(parameters.projectName).filterIsInstance<BuildChallenge>().isNotEmpty()
    }

    /**
     * Solves the achievements with description: Solve X Challenges. Needs the key 'solveNumber'
     * in the map [additionalParameters] with a positive Int value.
     */
    @JvmStatic
    fun solveXChallenges(classes: ArrayList<FileDetails>, parameters: Parameters,
                         run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                         additionalParameters: HashMap<String, String>): Boolean {

        return property.getCompletedChallenges(parameters.projectName).size >=
                (additionalParameters["solveNumber"]?.toInt() ?: Int.MAX_VALUE)
    }

    /**
     * Returns the amount of solved challenges in the project.
     */
    fun getSolvedChallengesCount(classes: ArrayList<FileDetails>, parameters: Parameters,
                                 run: Run<*, *>, property: GameUserProperty, listener: TaskListener): Int {

        return property.getCompletedChallenges(parameters.projectName).size
    }

    /**
     * Solves the achievements with description: Solve X Challenges with one Jenkins build. Needs the key 'solveNumber'
     * in the map [additionalParameters] with a positive Int value.
     */
    fun solveXAtOnce(classes: ArrayList<FileDetails>, parameters: Parameters,
                     run: Run<*, *>, property: GameUserProperty, listener: TaskListener,
                     additionalParameters: HashMap<String, String>): Boolean {

        return parameters.solved >= (additionalParameters["solveNumber"]?.toInt() ?: Int.MAX_VALUE)
    }

    /**
     * Returns the amount of challenges solved in this run
     */
    fun getSolvedChallengesSimultaneouslyCount(classes: ArrayList<FileDetails>, parameters: Parameters, run: Run<*, *>,
                                               property: GameUserProperty, listener: TaskListener): Int {

        return parameters.solved
    }
}