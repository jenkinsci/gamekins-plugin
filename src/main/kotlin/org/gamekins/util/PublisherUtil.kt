/*
 * Copyright 2021 Gamekins contributors
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
import hudson.model.*
import org.gamekins.GameUserProperty
import org.gamekins.challenge.ChallengeFactory
import org.gamekins.challenge.DummyChallenge
import org.gamekins.challenge.MutationTestChallenge
import org.gamekins.challenge.quest.QuestFactory
import org.gamekins.event.EventHandler
import org.gamekins.event.user.*
import org.gamekins.file.FileDetails
import org.gamekins.file.SourceFileDetails
import org.gamekins.property.GameJobProperty
import org.gamekins.property.GameMultiBranchProperty
import org.gamekins.property.GameProperty
import org.gamekins.statistics.Statistics
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.JacocoUtil.FilesOfAllSubDirectoriesCallable
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import java.io.IOException
import java.util.Comparator

/**
 * Util object for interaction with publishers.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
object PublisherUtil {

    /**
     * Checks whether one or more achievements are solved.
     */
    private fun checkAchievements(property: GameUserProperty, run: Run<*, *>, classes: ArrayList<SourceFileDetails>,
                                  parameters: Parameters, listener: TaskListener): Int {

        var solved = 0
        for (achievement in property.getUnsolvedAchievements(parameters.projectName)) {
            if (achievement.isSolved(classes, parameters, run, property, listener)) {
                property.completeAchievement(parameters.projectName, achievement)
                EventHandler.addEvent(AchievementSolvedEvent(parameters.projectName, parameters.branch,
                    property.getUser(), achievement))
                listener.logger.println("[Gamekins] Solved achievement $achievement")
                solved++
            }
        }

        return solved
    }

    /**
     * Checks whether the quests / quest steps of a user are solved or unsolvable and generates new ones if needed.
     */
    private fun checkQuests(run: Run<*, *>, property: GameUserProperty, parameters: Parameters, listener: TaskListener
    ): Int {

        var solved = 0
        for (quest in property.getCurrentQuests(parameters.projectName)) {
            if (quest.isCurrentStepSolved(parameters, run, listener)) {
                val questStep = quest.getLastStep()
                listener.logger.println("[Gamekins] Solved quest step $questStep of quest $quest")
                EventHandler.addEvent(
                    QuestStepSolvedEvent(parameters.projectName, parameters.branch, property.getUser(), quest)
                )
            }
            if (quest.isSolved()) {
                property.completeQuest(parameters.projectName, quest)
                EventHandler.addEvent(
                    QuestSolvedEvent(parameters.projectName, parameters.branch, property.getUser(), quest)
                )
                listener.logger.println("[Gamekins] Solved quest $quest")
                solved++
            }
        }

        for (quest in property.getCurrentQuests(parameters.projectName)) {
            if (!quest.isSolvable(parameters, run, listener)) {
                property.rejectQuest(parameters.projectName, quest, "One or more challenges are not solvable anymore")
                EventHandler.addEvent(
                    QuestUnsolvableEvent(parameters.projectName, parameters.branch, property.getUser(), quest)
                )
                listener.logger.println("[Gamekins] Quest $quest can not be solved anymore")
            }
        }

        return solved
    }

    /**
     * Checks whether one or more Challenges are unsolvable.
     */
    private fun checkSolvable(run: Run<*, *>, property: GameUserProperty, parameters: Parameters,
                              listener: TaskListener) {

        for (challenge in property.getCurrentChallenges(parameters.projectName)) {
            if (!challenge.isSolvable(parameters, run, listener)) {
                var reason = "Not solvable"
                if (challenge is MutationTestChallenge) reason = "Source file changed"
                property.rejectChallenge(parameters.projectName, challenge, reason)
                EventHandler.addEvent(ChallengeUnsolvableEvent(parameters.projectName, parameters.branch,
                    property.getUser(), challenge))
                listener.logger.println("[Gamekins] Challenge ${challenge?.toEscapedString()} " +
                        "can not be solved anymore")
            }
        }
    }

    /**
     * Checks whether one or more Challenges are solved.
     */
    private fun checkSolved(run: Run<*, *>, property: GameUserProperty, parameters: Parameters,
                            listener: TaskListener): Int {

        var solved = 0
        for (challenge in property.getCurrentChallenges(parameters.projectName)) {
            if (challenge is MutationTestChallenge && parameters.mocoJSONPath.isEmpty()) {
                listener.logger.println("[Gamekins] Cannot check this mutation test challenge is solved or not " +
                        "because moco.json can't be found - ${challenge.toEscapedString()}")
                continue
            }
            if (challenge.isSolved(parameters, run, listener)) {
                property.completeChallenge(parameters.projectName, challenge)
                property.addScore(parameters.projectName, challenge.getScore())
                EventHandler.addEvent(ChallengeSolvedEvent(parameters.projectName, parameters.branch,
                    property.getUser(), challenge))
                listener.logger.println("[Gamekins] Solved challenge ${challenge?.toEscapedString()}.")
                if (challenge !is DummyChallenge) solved++
            }
        }

        return solved
    }

    /**
     * Checks the solved and solvable state of a [user] and generates new Challenges if needed. Returns a [HashMap]
     * with the number of generated and solved Challenges.
     */
    fun checkUser(user: User, run: Run<*, *>, classes: ArrayList<SourceFileDetails>,
                  parameters: Parameters, result: Result?, listener: TaskListener = TaskListener.NULL)
            : HashMap<String, Int> {

        var generated = 0
        var solved = 0
        var solvedAchievements = 0
        var solvedQuests = 0
        var generatedQuests = 0
        if (!PropertyUtil.realUser(user)) return hashMapOf("generated" to generated, "solved" to solved,
            "solvedAchievements" to solvedAchievements, "solvedQuests" to solvedQuests,
            "generatedQuests" to generatedQuests)

        val property = user.getProperty(GameUserProperty::class.java)
        if (property != null && property.isParticipating(parameters.projectName)) {

            //Generate BuildChallenge if the run has failed
            if (ChallengeFactory.generateBuildChallenge(result, user, property, parameters, listener)) {
                generated ++
            }

            listener.logger.println("[Gamekins] Start checking solved status of challenges for user ${user.fullName}")

            //Check if a Challenges is solved
            val userSolved = checkSolved(run, property, parameters, listener)

            listener.logger.println("[Gamekins] Start checking solvable state of challenges for user ${user.fullName}")

            //Check if the Challenges are still solvable
            checkSolvable(run, property, parameters, listener)

            //Generate new Challenges if the user has less than three
            val userGenerated = ChallengeFactory.generateNewChallenges(
                user, property, parameters, classes, listener,
                maxChallenges = parameters.currentChallengesCount)

            //Check if an achievement is solved
            listener.logger.println("[Gamekins] Start checking solved status of achievements for user ${user.fullName}")
            parameters.solved = userSolved
            parameters.generated = userGenerated
            solvedAchievements = checkAchievements(property, run, classes, parameters, listener)

            listener.logger.println("[Gamekins] Start checking solved status of quests for user ${user.fullName}")
            solvedQuests = checkQuests(run, property, parameters, listener)
            generatedQuests = QuestFactory.generateNewQuests(user, property, parameters, listener,
                classes as ArrayList<FileDetails>, maxQuests = parameters.currentQuestsCount)

            solved += userSolved
            generated += userGenerated

            try {
                user.save()
            } catch (e: IOException) {
                e.printStackTrace(listener.logger)
            }
        }

        return hashMapOf("generated" to generated, "solved" to solved, "solvedAchievements" to solvedAchievements,
            "solvedQuests" to solvedQuests, "generatedQuests" to generatedQuests)
    }

    /**
     * Checks whether the path of the JaCoCo csv file [jacocoCSVPath] exists in the [workspace].
     */
    @JvmStatic
    fun doCheckJacocoCSVPath(workspace: FilePath, jacocoCSVPath: String): Boolean {
        var csvPath = jacocoCSVPath
        if (csvPath.startsWith("**")) csvPath = csvPath.substring(2)
        val split = csvPath.split("/".toRegex())
        val files: List<FilePath> = try {
            workspace.act(
                    FilesOfAllSubDirectoriesCallable(workspace, split[split.size - 1]))
        } catch (ignored: Exception) {
            return false
        }
        for (file in files) {
            if (file.remote.endsWith(csvPath)) {
                return true
            }
        }
        return false
    }

    /**
     * Checks whether the path of the moco json file [mocoJSONPath] exists in the [workspace].
     */
    @JvmStatic
    fun doCheckMocoJSONPath(workspace: FilePath, mocoJSONPath: String?): Boolean {
        if (mocoJSONPath.isNullOrEmpty()) {
            return false
        }
        var jsonPath = mocoJSONPath
        if (!jsonPath.endsWith(".json")) return false
        if (jsonPath.startsWith("**")) jsonPath = jsonPath.substring(2)
        val split = jsonPath.split("/".toRegex())
        val files: List<FilePath> = try {
            workspace.act(
                FilesOfAllSubDirectoriesCallable(workspace, split[split.size - 1]))
        } catch (ignored: Exception) {
            return false
        }
        for (file in files) {
            if (file.remote.endsWith(jsonPath)) {
                return true
            }
        }
        return false
    }


    /**
     * Checks whether the path of the JaCoCo index.html file [jacocoResultsPath] exists in the [workspace].
     */
    @JvmStatic
    fun doCheckJacocoResultsPath(workspace: FilePath, jacocoResultsPath: String): Boolean {
        var resultsPath = jacocoResultsPath
        if (!resultsPath.endsWith("/")) resultsPath += "/"
        if (resultsPath.startsWith("**")) resultsPath = resultsPath.substring(2)
        val files: List<FilePath> = try {
            workspace.act(FilesOfAllSubDirectoriesCallable(workspace, "index.html"))
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        for (file in files) {
            val path = file.remote
            if (path.substring(0, path.length - 10).endsWith(resultsPath)) {
                return true
            }
        }
        return false
    }

    /**
     * Retrieves all last changed classes and removes fully covered classes [removeFullyCoveredClasses] and classes
     * without JaCoCo files [removeClassesWithoutJacocoFiles] and sorts them according to their coverage [sort] if
     * activated.
     */
    @JvmStatic
    fun retrieveLastChangedClasses(searchCommitCount: Int, parameters: Parameters,
                                   users: Collection<User> = User.getAll(), listener: TaskListener = TaskListener.NULL,
                                   removeFullyCoveredClasses: Boolean = true,
                                   removeClassesWithoutJacocoFiles: Boolean = true, sort: Boolean = true)
            : List<SourceFileDetails> {

        val classes: ArrayList<SourceFileDetails>
        try {
            classes = ArrayList(GitUtil.getLastChangedClasses(searchCommitCount, "",
                parameters, listener, GitUtil.mapUsersToGameUsers(users)))
            listener.logger.println("[Gamekins] Found ${classes.size} last changed files")

            if (removeFullyCoveredClasses) {
                classes.removeIf { details: SourceFileDetails -> details.coverage == 1.0 }
            }
            listener.logger.println("[Gamekins] Found ${classes.size} last changed files without 100% coverage")

            if (removeClassesWithoutJacocoFiles) {
                classes.removeIf { details: SourceFileDetails -> !details.filesExists() }
            }
            listener.logger.println("[Gamekins] Found ${classes.size} last changed files with " +
                    "existing coverage reports")

            if (sort) {
                classes.sortWith(Comparator.comparingDouble(SourceFileDetails::coverage))
                classes.reverse()
            }
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return arrayListOf()
        }

        return classes
    }

    /**
     * Updates the [Statistics] after all users have been checked.
     */
    fun updateStatistics(run: Run<*, *>, parameters: Parameters, generated: Int, solved: Int,
                         solvedAchievements: Int, solvedQuests: Int, generatedQuests: Int,
                         listener: TaskListener = TaskListener.NULL) {

        //Get the current job and property
        val property: GameProperty?
        val job: AbstractItem
        if (run.parent.parent is WorkflowMultiBranchProject) {
            job = run.parent.parent as AbstractItem
            property = (run.parent.parent as WorkflowMultiBranchProject)
                    .properties.get(GameMultiBranchProperty::class.java)
        } else {
            job = run.parent
            property = run.parent.getProperty(GameJobProperty::class.java.name) as GameJobProperty
        }

        //Add a new entry to the Statistics
        if (property != null) {
            property.getStatistics()
                .addRunEntry(
                    job,
                    parameters.branch,
                    Statistics.RunEntry(
                        run.getNumber(),
                        parameters.branch,
                        run.result,
                        run.startTimeInMillis,
                        generated,
                        solved,
                        solvedAchievements,
                        solvedQuests,
                        generatedQuests,
                        parameters.projectTests,
                        parameters.projectCoverage
                    ), listener)

            try {
                property.getOwner().save()
            } catch (e: IOException) {
                e.printStackTrace(listener.logger)
            }
        } else {
            listener.logger.println("[Gamekins] No entry for Statistics added")
        }
    }
}
