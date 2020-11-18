package io.jenkins.plugins.gamekins.util

import hudson.FilePath
import hudson.model.*
import io.jenkins.plugins.gamekins.GameUserProperty
import io.jenkins.plugins.gamekins.challenge.ChallengeFactory
import io.jenkins.plugins.gamekins.challenge.DummyChallenge
import io.jenkins.plugins.gamekins.property.GameJobProperty
import io.jenkins.plugins.gamekins.property.GameMultiBranchProperty
import io.jenkins.plugins.gamekins.property.GameProperty
import io.jenkins.plugins.gamekins.statistics.Statistics
import io.jenkins.plugins.gamekins.util.JacocoUtil.FilesOfAllSubDirectoriesCallable
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import java.io.IOException
import java.util.ArrayList
import java.util.Comparator

/**
 * Util object for interaction with publishers.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
object PublisherUtil {

    /**
     * Checks the solved and solvable state of a [user] and generates new Challenges if needed. Returns a [HashMap]
     * with the number of generated and solved Challenges.
     */
    fun checkUser(user: User, run: Run<*, *>, classes: ArrayList<JacocoUtil.ClassDetails>,
                  constants: HashMap<String, String>, result: Result?, workspace: FilePath,
                  listener: TaskListener = TaskListener.NULL)
            : HashMap<String, Int> {

        var generated = 0
        var solved = 0
        if (!PropertyUtil.realUser(user)) return hashMapOf("generated" to 0, "solved" to 0)

        val property = user.getProperty(GameUserProperty::class.java)
        if (property != null && property.isParticipating(constants["projectName"]!!)) {

            //Generate BuildChallenge if the run has failed
            if (ChallengeFactory.generateBuildChallenge(result, user, workspace, property, constants, listener)) {
                generated ++
            }

            listener.logger.println("[Gamekins] Start checking solved status of challenges for user ${user.fullName}")

            //Check if a Challenges is solved
            for (challenge in property.getCurrentChallenges(constants["projectName"])) {
                if (challenge.isSolved(constants, run, listener, workspace)) {
                    property.completeChallenge(constants["projectName"]!!, challenge)
                    property.addScore(constants["projectName"]!!, challenge.getScore())
                    listener.logger.println("[Gamekins] Solved challenge $challenge")
                    if (challenge !is DummyChallenge) solved++
                }
            }

            listener.logger.println("[Gamekins] Start checking solvable state of challenges for user ${user.fullName}")

            //Check if the Challenges are still solvable
            for (challenge in property.getCurrentChallenges(constants["projectName"])) {
                if (!challenge.isSolvable(constants, run, listener, workspace)) {
                    property.rejectChallenge(constants["projectName"]!!, challenge, "Not solvable")
                    listener.logger.println("[Gamekins] Challenge $challenge can not be solved anymore")
                }
            }

            //Generate new Challenges if the user has less than three
            generated += ChallengeFactory.generateNewChallenges(user, property, constants, classes,
                    workspace, listener)

            try {
                user.save()
            } catch (e: IOException) {
                e.printStackTrace(listener.logger)
            }
        }

        return hashMapOf("generated" to generated, "solved" to solved)
    }

    /**
     * Checks whether the path of the JaCoCo csv file [jacocoCSVPath] exists in the [workspace].
     */
    @JvmStatic
    fun doCheckJacocoCSVPath(workspace: FilePath, jacocoCSVPath: String): Boolean {
        var csvPath = jacocoCSVPath
        if (csvPath.startsWith("**")) csvPath = csvPath.substring(2)
        val split = csvPath.split("/".toRegex())
        val files: List<FilePath>
        files = try {
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
     * Checks whether the path of the JaCoCo index.html file [jacocoResultsPath] exists in the [workspace].
     */
    @JvmStatic
    fun doCheckJacocoResultsPath(workspace: FilePath, jacocoResultsPath: String): Boolean {
        var resultsPath = jacocoResultsPath
        if (!resultsPath.endsWith("/")) resultsPath += "/"
        if (resultsPath.startsWith("**")) resultsPath = resultsPath.substring(2)
        val files: List<FilePath>
        files = try {
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
     * Retrieves all last changed classes and removes fully covered classes [removeFullCoveredClasses] and classes
     * without JaCoCo files [removeClassesWithoutJacocoFiles] and sorts them according to their coverage [sort] if
     * activated.
     */
    @JvmStatic
    fun retrieveLastChangedClasses(workspace: FilePath, searchCommitCount: Int, constants: HashMap<String, String>,
                                   users: Collection<User> = User.getAll(), listener: TaskListener = TaskListener.NULL,
                                   removeFullCoveredClasses: Boolean = true,
                                   removeClassesWithoutJacocoFiles: Boolean = true, sort: Boolean = true)
            : ArrayList<JacocoUtil.ClassDetails> {

        val classes: ArrayList<JacocoUtil.ClassDetails>
        try {
            classes = workspace.act(GitUtil.LastChangedClassesCallable(searchCommitCount, constants,
                    listener, GitUtil.mapUsersToGameUsers(users), workspace))
            listener.logger.println("[Gamekins] Found ${classes.size} last changed files")

            if (removeFullCoveredClasses) {
                classes.removeIf { classDetails: JacocoUtil.ClassDetails? -> classDetails!!.coverage == 1.0 }
            }
            listener.logger.println("[Gamekins] Found ${classes.size} last changed files without 100% coverage")

            if (removeClassesWithoutJacocoFiles) {
                classes.removeIf { classDetails: JacocoUtil.ClassDetails? -> !classDetails!!.filesExists() }
            }
            listener.logger.println("[Gamekins] Found ${classes.size} last changed files with " +
                    "existing coverage reports")

            if (sort) {
                classes.sortWith(Comparator.comparingDouble(JacocoUtil.ClassDetails::coverage))
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
    fun updateStatistics(run: Run<*, *>, constants: HashMap<String, String>, workspace: FilePath, generated: Int,
                         solved: Int, listener: TaskListener = TaskListener.NULL) {

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
                            constants["branch"]!!,
                            Statistics.RunEntry(
                                    run.getNumber(),
                                    constants["branch"]!!,
                                    run.result,
                                    run.startTimeInMillis,
                                    generated,
                                    solved,
                                    JacocoUtil.getTestCount(workspace, run),
                                    JacocoUtil.getProjectCoverage(workspace,
                                            constants["jacocoCSVPath"]!!
                                                    .split("/".toRegex())
                                                    [constants["jacocoCSVPath"]!!
                                                    .split("/".toRegex())
                                                    .size - 1])
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
