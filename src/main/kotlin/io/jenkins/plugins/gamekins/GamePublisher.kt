package io.jenkins.plugins.gamekins

import hudson.FilePath
import hudson.Launcher
import hudson.model.*
import hudson.security.HudsonPrivateSecurityRealm.Details
import hudson.tasks.BuildStepMonitor
import hudson.tasks.Notifier
import io.jenkins.plugins.gamekins.challenge.BuildChallenge
import io.jenkins.plugins.gamekins.challenge.Challenge
import io.jenkins.plugins.gamekins.challenge.ChallengeFactory.generateChallenge
import io.jenkins.plugins.gamekins.challenge.DummyChallenge
import io.jenkins.plugins.gamekins.property.GameJobProperty
import io.jenkins.plugins.gamekins.property.GameMultiBranchProperty
import io.jenkins.plugins.gamekins.property.GameProperty
import io.jenkins.plugins.gamekins.statistics.Statistics.RunEntry
import io.jenkins.plugins.gamekins.util.GitUtil
import io.jenkins.plugins.gamekins.util.GitUtil.GameUser
import io.jenkins.plugins.gamekins.util.GitUtil.HeadCommitCallable
import io.jenkins.plugins.gamekins.util.GitUtil.LastChangedClassesCallable
import io.jenkins.plugins.gamekins.util.JacocoUtil
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import io.jenkins.plugins.gamekins.util.PropertyUtil
import io.jenkins.plugins.gamekins.util.PublisherUtil
import jenkins.tasks.SimpleBuildStep
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import java.io.IOException
import java.util.*
import javax.annotation.Nonnull

/**
 * Class that is called after the build of a job in Jenkins is finished. This one executes the main functionality of
 * Gamekins by creating and soling [Challenge]s.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class GamePublisher @DataBoundConstructor constructor(@set:DataBoundSetter var jacocoResultsPath: String,
                                                      @set:DataBoundSetter var jacocoCSVPath: String,
                                                      searchCommitCount: Int)
    : Notifier(), SimpleBuildStep {

    @set:DataBoundSetter
    var searchCommitCount: Int = if (searchCommitCount > 0) searchCommitCount else GitUtil.DEFAULT_SEARCH_COMMIT_COUNT

    /**
     * Starts the execution of Gamekins for a specific [run] with its [result]. The [constants] contain needed Strings
     * like the paths to the JaCoCo files. The [workspace] is the folder with the code and execution rights, and the
     * [listener] reports the events to the console output of Jenkins.
     */
    private fun executePublisher(run: Run<*, *>, constants: HashMap<String, String>, result: Result?,
                                 listener: TaskListener, workspace: FilePath?) {
        //Checks whether the paths of the JaCoCo files are correct
        if (!PublisherUtil.doCheckJacocoResultsPath(workspace!!, jacocoResultsPath)) {
            listener.logger.println("[Gamekins] JaCoCo folder is not correct")
            return
        }
        if (!PublisherUtil.doCheckJacocoCSVPath(workspace, jacocoCSVPath)) {
            listener.logger.println("[Gamekins] JaCoCo csv file could not be found")
            return
        }
        constants["jacocoResultsPath"] = jacocoResultsPath
        constants["jacocoCSVPath"] = jacocoCSVPath

        //Extracts the branch
        if (run.parent.parent is WorkflowMultiBranchProject) {
            constants["branch"] = run.parent.name
        } else {
            constants["branch"] = GitUtil.getBranch(workspace)
        }

        listener.logger.println("[Gamekins] Start")
        listener.logger.println("[Gamekins] Solve Challenges and generate new Challenges")

        //Computes the last changed classes
        val classes: ArrayList<ClassDetails>
        try {
            classes = workspace.act(LastChangedClassesCallable(searchCommitCount, constants,
                    listener, GitUtil.mapUsersToGameUsers(User.getAll()), workspace))
            listener.logger.println("[Gamekins] Found " + classes.size + " last changed files")
            classes.removeIf { classDetails: ClassDetails? -> classDetails!!.coverage == 1.0 }
            listener.logger.println("[Gamekins] Found " + classes.size
                    + " last changed files without 100% coverage")
            classes.removeIf { classDetails: ClassDetails? -> !classDetails!!.filesExists() }
            listener.logger.println("[Gamekins] Found " + classes.size
                    + " last changed files with existing coverage reports")
            classes.sortWith(Comparator.comparingDouble(ClassDetails::coverage))
            classes.reverse()
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return
        }

        //Checks for each user his Challenges and generates new ones if needed
        var solved = 0
        var generated = 0
        for (user in User.getAll()) {
            if (!PropertyUtil.realUser(user)) continue

            val property = user.getProperty(GameUserProperty::class.java)
            if (property != null && property.isParticipating(constants["projectName"]!!)) {
                try {
                    if (result != null && result != Result.SUCCESS) {
                        val challenge = BuildChallenge()
                        val mapUser: User? = GitUtil.mapUser(workspace.act(HeadCommitCallable(workspace.remote))
                                .authorIdent, User.getAll())

                        if (mapUser == user
                                && !property.getCurrentChallenges(constants["projectName"]).contains(challenge)) {
                            property.newChallenge(constants["projectName"]!!, challenge)
                            listener.logger.println("[Gamekins] Generated new BuildChallenge")
                            generated++
                            user.save()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace(listener.logger)
                }

                listener.logger.println("[Gamekins] Start checking solved status of challenges for user "
                        + user.fullName)

                //Check if a Challenges is solved
                for (challenge in property.getCurrentChallenges(constants["projectName"])) {
                    if (challenge.isSolved(constants, run, listener, workspace)) {
                        property.completeChallenge(constants["projectName"]!!, challenge)
                        property.addScore(constants["projectName"]!!, challenge.getScore())
                        listener.logger.println("[Gamekins] Solved challenge $challenge")
                        if (challenge !is DummyChallenge) solved++
                    }
                }

                listener.logger.println("[Gamekins] Start checking solvable state of challenges for user "
                        + user.fullName)

                //Check if the Challenges are still solvable
                for (challenge in property.getCurrentChallenges(constants["projectName"])) {
                    if (!challenge.isSolvable(constants, run, listener, workspace)) {
                        property.rejectChallenge(constants["projectName"]!!, challenge, "Not solvable")
                        listener.logger.println("[Gamekins] Challenge " + challenge.toString()
                                + " can not be solved anymore")
                    }
                }

                //Generate new Challenges if the user has less than three
                if (property.getCurrentChallenges(constants["projectName"]).size < 3) {
                    listener.logger.println("[Gamekins] Start generating challenges for user "
                            + user.fullName)

                    val userClasses = ArrayList(classes)
                    userClasses.removeIf { classDetails: ClassDetails? ->
                        !classDetails!!.changedByUsers
                                .contains(GameUser(user))
                    }

                    listener.logger.println("[Gamekins] Found " + userClasses.size
                            + " last changed files of user " + user.fullName)

                    for (i in property.getCurrentChallenges(constants["projectName"]).size..2) {
                        if (userClasses.size == 0) {
                            property.newChallenge(constants["projectName"]!!, DummyChallenge())
                            break
                        }

                        try {
                            //Try to generate a new unique Challenge three times. because it can fail
                            var challenge: Challenge
                            var isChallengeUnique: Boolean
                            var count = 0
                            do {
                                if (count == 3) {
                                    challenge = DummyChallenge()
                                    break
                                }
                                isChallengeUnique = true

                                listener.logger.println("[Gamekins] Started to generate challenge")
                                challenge = generateChallenge(user, constants, listener, userClasses, workspace)

                                listener.logger.println("[Gamekins] Generated challenge $challenge")
                                if (challenge is DummyChallenge) break

                                for (currentChallenge in property.getCurrentChallenges(constants["projectName"])) {
                                    if (currentChallenge.toString() == challenge.toString()) {
                                        isChallengeUnique = false
                                        listener.logger.println("[Gamekins] Challenge is not unique")
                                        break
                                    }
                                }
                                count++
                            } while (!isChallengeUnique)

                            property.newChallenge(constants["projectName"]!!, challenge)
                            listener.logger.println("[Gamekins] Added challenge $challenge")
                            generated++
                        } catch (e: Exception) {
                            e.printStackTrace(listener.logger)
                        }
                    }
                }

                try {
                    user.save()
                } catch (e: IOException) {
                    e.printStackTrace(listener.logger)
                }
            }
        }

        listener.logger.println("[Gamekins] Solved " + solved + " Challenges and generated "
                + generated + " Challenges")
        listener.logger.println("[Gamekins] Update Statistics")

        //Get the current job and property
        val property: GameProperty?
        val job: AbstractItem
        if (run.parent.parent is WorkflowMultiBranchProject) {
            job = run.parent.parent as AbstractItem
            property = (run.parent.parent as WorkflowMultiBranchProject)
                    .properties.get(GameMultiBranchProperty::class.java)
        } else {
            job = run.parent
            property = run.parent.getProperty(GameProperty::class.java.name) as GameProperty
        }

        //Add a new entry to the Statistics
        if (property != null) {
            property.getStatistics()
                    .addRunEntry(
                            job,
                            constants["branch"]!!,
                            RunEntry(
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

        listener.logger.println("[Gamekins] Finished")
    }

    override fun getRequiredMonitorService(): BuildStepMonitor {
        return BuildStepMonitor.STEP
    }


    override fun needsToRunAfterFinalized(): Boolean {
        return true
    }

    /**
     * This method is called by jobs of type [AbstractProject] and starts the execution of Gamekins.
     *
     * @see Notifier.perform
     */
    override fun perform(build: AbstractBuild<*, *>, launcher: Launcher, listener: BuildListener): Boolean {
        if (build.project == null || build.project.getProperty(GameJobProperty::class.java) == null
                || !build.project.getProperty(GameJobProperty::class.java).activated) {
            listener.logger.println("[Gamekins] Not activated")
            return true
        }

        val constants = HashMap<String, String>()
        constants["projectName"] = build.project.name
        executePublisher(build, constants, build.result, listener, build.workspace)
        return true
    }

    /**
     * This method is called by jobs of type [WorkflowJob] and [WorkflowMultiBranchProject] and starts the
     * execution of Gamekins.
     *
     * @see SimpleBuildStep.perform
     */
    override fun perform(@Nonnull run: Run<*, *>, @Nonnull workspace: FilePath,
                         @Nonnull launcher: Launcher, @Nonnull listener: TaskListener) {
        val constants = HashMap<String, String>()
        if (run.parent.parent is WorkflowMultiBranchProject) {
            val project = run.parent.parent as WorkflowMultiBranchProject
            if (project.properties.get(GameMultiBranchProperty::class.java) == null
                    || !project.properties.get(GameMultiBranchProperty::class.java).activated) {
                listener.logger.println("[Gamekins] Not activated")
                return
            }
            constants["projectName"] = project.name
        } else {
            if (run.parent.getProperty(GameJobProperty::class.java) == null
                    || !run.parent.getProperty(GameJobProperty::class.java).activated) {
                listener.logger.println("[Gamekins] Not activated")
                return
            }
            constants["projectName"] = run.parent.name
        }

        constants["jacocoResultsPath"] = jacocoResultsPath
        constants["jacocoCSVPath"] = jacocoCSVPath
        executePublisher(run, constants, run.result, listener, workspace)
    }
}
