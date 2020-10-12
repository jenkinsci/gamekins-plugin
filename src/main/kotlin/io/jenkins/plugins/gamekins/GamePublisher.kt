package io.jenkins.plugins.gamekins

import hudson.FilePath
import hudson.Launcher
import hudson.model.*
import hudson.security.HudsonPrivateSecurityRealm.Details
import hudson.tasks.BuildStep
import hudson.tasks.BuildStepMonitor
import hudson.tasks.Notifier
import hudson.tasks.Publisher
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
import io.jenkins.plugins.gamekins.util.GitUtil.getBranch
import io.jenkins.plugins.gamekins.util.GitUtil.mapUser
import io.jenkins.plugins.gamekins.util.GitUtil.mapUsersToGameUsers
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import io.jenkins.plugins.gamekins.util.JacocoUtil.getProjectCoverage
import io.jenkins.plugins.gamekins.util.JacocoUtil.getTestCount
import io.jenkins.plugins.gamekins.util.PublisherUtil.doCheckJacocoCSVPath
import io.jenkins.plugins.gamekins.util.PublisherUtil.doCheckJacocoResultsPath
import jenkins.tasks.SimpleBuildStep
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import java.io.IOException
import java.util.*
import javax.annotation.Nonnull

class GamePublisher @DataBoundConstructor constructor(@set:DataBoundSetter var jacocoResultsPath: String,
                                                      @set:DataBoundSetter var jacocoCSVPath: String,
                                                      searchCommitCount: Int)
    : Notifier(), SimpleBuildStep {

    @set:DataBoundSetter
    var searchCommitCount: Int = if (searchCommitCount > 0) searchCommitCount else GitUtil.DEFAULT_SEARCH_COMMIT_COUNT

    private fun executePublisher(run: Run<*, *>, constants: HashMap<String, String>, result: Result?,
                                 listener: TaskListener, workspace: FilePath?) {
        if (!doCheckJacocoResultsPath(workspace!!, jacocoResultsPath)) {
            listener.logger.println("[Gamekins] JaCoCo folder is not correct")
            return
        }
        if (!doCheckJacocoCSVPath(workspace, jacocoCSVPath)) {
            listener.logger.println("[Gamekins] JaCoCo csv file could not be found")
            return
        }
        constants["jacocoResultsPath"] = jacocoResultsPath
        constants["jacocoCSVPath"] = jacocoCSVPath
        if (run.getParent().getParent() is WorkflowMultiBranchProject) {
            constants["branch"] = run.getParent().getName()
        } else {
            constants["branch"] = getBranch(workspace)
        }
        listener.logger.println("[Gamekins] Start")
        listener.logger.println("[Gamekins] Solve Challenges and generate new Challenges")
        val classes: ArrayList<ClassDetails>
        try {
            classes = workspace.act(LastChangedClassesCallable(searchCommitCount, constants,
                    listener, mapUsersToGameUsers(User.getAll()), workspace))
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
        var solved = 0
        var generated = 0
        for (user in User.getAll()) {
            if (user.getProperty(Details::class.java) == null) continue
            val property = user.getProperty(GameUserProperty::class.java)
            if (property != null && property.isParticipating(constants["projectName"]!!)) {
                try {
                    if (result != null && result != Result.SUCCESS) {
                        val challenge = BuildChallenge()
                        val mapUser: User? = mapUser(
                                workspace.act(HeadCommitCallable(workspace.remote))
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
                for (challenge in property.getCurrentChallenges(constants["projectName"])) {
                    if (!challenge.isSolvable(constants, run, listener, workspace)) {
                        property.rejectChallenge(constants["projectName"]!!, challenge, "Not solvable")
                        listener.logger.println("[Gamekins] Challenge " + challenge.toString()
                                + " can not be solved anymore")
                    }
                }
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
                                challenge = generateChallenge(user, constants, listener, userClasses,
                                        workspace)
                                listener.logger.println("[Gamekins] Generated challenge $challenge")
                                if (challenge is DummyChallenge) break
                                for (currentChallenge
                                in property.getCurrentChallenges(constants["projectName"])) {
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
        val property: GameProperty?
        val job: AbstractItem
        if (run.getParent().getParent() is WorkflowMultiBranchProject) {
            job = run.getParent().getParent() as AbstractItem
            property = (run.getParent().getParent() as WorkflowMultiBranchProject)
                    .properties.get(GameMultiBranchProperty::class.java)
        } else {
            job = run.getParent()
            property = run.getParent().getProperty(GameProperty::class.java.name) as GameProperty
        }
        if (property != null) {
            property.getStatistics()
                    .addRunEntry(
                            job,
                            constants["branch"]!!,
                            RunEntry(
                                    run.getNumber(),
                                    constants["branch"]!!,
                                    run.getResult(),
                                    run.getStartTimeInMillis(),
                                    generated,
                                    solved,
                                    getTestCount(workspace, run),
                                    getProjectCoverage(workspace,
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

    /**
     * Declares the scope of the synchronization monitor this [BuildStep] expects from outside.
     *
     *
     *
     * This method is introduced for preserving compatibility with plugins written for earlier versions of Hudson,
     * which never run multiple builds of the same job in parallel. Such plugins often assume that the outcome
     * of the previous build is completely available, which is no longer true when we do concurrent builds.
     *
     *
     *
     * To minimize the necessary code change for such plugins, [BuildStep] implementations can request
     * Hudson to externally perform synchronization before executing them. This behavior is as follows:
     *
     * <dl>
     * <dt>[BuildStepMonitor.BUILD]
    </dt> * <dd>
     * This [BuildStep] is only executed after the previous build is fully
     * completed (thus fully restoring the earlier semantics of one build at a time.)
     *
    </dd> * <dt>[BuildStepMonitor.STEP]
    </dt> * <dd>
     * This [BuildStep] is only executed after the same step in the previous build is completed.
     * For build steps that use a weaker assumption and only rely on the output from the same build step of
     * the early builds, this improves the concurrency.
     *
    </dd> * <dt>[BuildStepMonitor.NONE]
    </dt> * <dd>
     * No external synchronization is performed on this build step. This is the most efficient, and thus
     * **the recommended value for newer plugins**. Wherever necessary, you can directly use [CheckPoint]s
     * to perform necessary synchronizations.
    </dd></dl> *
     *
     * <h2>Migrating Older Implementation</h2>
     *
     *
     * If you are migrating [BuildStep] implementations written for earlier versions of Hudson,
     * here's what you can do:
     *
     *
     *  *
     * To demand the backward compatible behavior from Jenkins, leave this method unoverridden,
     * and make no other changes to the code. This will prevent users from reaping the benefits of concurrent
     * builds, but at least your plugin will work correctly, and therefore this is a good easy first step.
     *  *
     * If your build step doesn't use anything from a previous build (for example, if you don't even call
     * [Run.getPreviousBuild]), then you can return [BuildStepMonitor.NONE] without making further
     * code changes and you are done with migration.
     *  *
     * If your build step only depends on [Action]s that you added in the previous build by yourself,
     * then you only need [BuildStepMonitor.STEP] scope synchronization. Return it from this method
     * ,and you are done with migration without any further code changes.
     *  *
     * If your build step makes more complex assumptions, return [BuildStepMonitor.NONE] and use
     * [CheckPoint]s directly in your code. The general idea is to call [CheckPoint.block] before
     * you try to access the state from the previous build.
     *
     *
     * @since 1.319
     */
    override fun getRequiredMonitorService(): BuildStepMonitor {
        return BuildStepMonitor.STEP
    }

    /**
     * Return true if this [Publisher] needs to run after the build result is
     * fully finalized.
     *
     *
     *
     * The execution of normal [Publisher]s are considered within a part
     * of the build. This allows publishers to mark the build as a failure, or
     * to include their execution time in the total build time.
     *
     *
     *
     * So normally, that is the preferable behavior, but in a few cases
     * this is problematic. One of such cases is when a publisher needs to
     * trigger other builds, which in turn need to see this build as a
     * completed build. Those plugins that need to do this can return true
     * from this method, so that the [.perform]
     * method is called after the build is marked as completed.
     *
     *
     *
     * When [Publisher] behaves this way, note that they can no longer
     * change the build status anymore.
     *
     * @since 1.153
     */
    override fun needsToRunAfterFinalized(): Boolean {
        return true
    }

    /**
     * {@inheritDoc}
     *
     * @param build
     * @param launcher
     * @param listener
     * @return Delegates to [SimpleBuildStep.perform]
     * if possible, always returning true or throwing an error.
     */
    override fun perform(build: AbstractBuild<*, *>, launcher: Launcher, listener: BuildListener): Boolean {
        if (build.getProject() == null || build.getProject().getProperty(GameJobProperty::class.java) == null
                || !build.getProject().getProperty(GameJobProperty::class.java).activated) {
            listener.logger.println("[Gamekins] Not activated")
            return true
        }
        val constants = HashMap<String, String>()
        constants["projectName"] = build.getProject().getName()
        executePublisher(build, constants, build.getResult(), listener, build.getWorkspace())
        return true
    }

    /**
     * Run this step.
     *
     * @param run       a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param launcher  a way to start processes
     * @param listener  a place to send output
     */
    override fun perform(@Nonnull run: Run<*, *>, @Nonnull workspace: FilePath,
                         @Nonnull launcher: Launcher, @Nonnull listener: TaskListener) {
        val constants = HashMap<String, String>()
        if (run.getParent().getParent() is WorkflowMultiBranchProject) {
            val project = run.getParent().getParent() as WorkflowMultiBranchProject
            if (project.properties.get(GameMultiBranchProperty::class.java) == null
                    || !project.properties.get(GameMultiBranchProperty::class.java).activated) {
                listener.logger.println("[Gamekins] Not activated")
                return
            }
            constants["projectName"] = project.name
        } else {
            if (run.getParent().getProperty(GameJobProperty::class.java) == null
                    || !run.getParent().getProperty(GameJobProperty::class.java).activated) {
                listener.logger.println("[Gamekins] Not activated")
                return
            }
            constants["projectName"] = run.getParent().getName()
        }
        constants["jacocoResultsPath"] = jacocoResultsPath
        constants["jacocoCSVPath"] = jacocoCSVPath
        executePublisher(run, constants, run.getResult(), listener, workspace)
    }
}
