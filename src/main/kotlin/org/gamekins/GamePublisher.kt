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

package org.gamekins

import hudson.FilePath
import hudson.Launcher
import hudson.model.*
import hudson.tasks.BuildStepMonitor
import hudson.tasks.Notifier
import jenkins.model.Jenkins
import org.gamekins.challenge.Challenge
import org.gamekins.property.GameJobProperty
import org.gamekins.property.GameMultiBranchProperty
import org.gamekins.util.GitUtil
import org.gamekins.util.PublisherUtil
import jenkins.tasks.SimpleBuildStep
import org.gamekins.event.EventHandler
import org.gamekins.event.build.BuildFinishedEvent
import org.gamekins.event.build.BuildStartedEvent
import org.gamekins.mutation.MutationResults
import org.gamekins.util.JUnitUtil
import org.gamekins.util.JacocoUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.kohsuke.stapler.DataBoundConstructor
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.StaplerProxy
import javax.annotation.Nonnull

/**
 * Class that is called after the build of a job in Jenkins is finished. This one executes the main functionality of
 * Gamekins by creating and solving [Challenge]s.
 *
 * [jacocoResultsPath], [jacocoCSVPath], and [mocoJSONPath] must be of type String?, as Jenkins wants to instantiate the
 * [GamePublisher] with null when Gamekins is not activated.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */

class GamePublisher @DataBoundConstructor constructor(@set:DataBoundSetter var jacocoResultsPath: String?,
                                                      @set:DataBoundSetter var jacocoCSVPath: String?,
                                                      @set:DataBoundSetter var mocoJSONPath: String?,
                                                      searchCommitCount: Int)
    : Notifier(), SimpleBuildStep, StaplerProxy {

    @set:DataBoundSetter
    var searchCommitCount: Int = if (searchCommitCount > 0) searchCommitCount else GitUtil.DEFAULT_SEARCH_COMMIT_COUNT

    companion object {
        private const val NOT_ACTIVATED = "[Gamekins] Not activated"
    }

    override fun getTarget(): Any {
        Jenkins.getInstanceOrNull()?.checkPermission(Item.CONFIGURE)
        return this
    }

    /**
     * Starts the execution of Gamekins for a specific [run] with its [result]. The [constants] contain needed Strings
     * like the paths to the JaCoCo files. The [workspace] is the folder with the code and execution rights, and the
     * [listener] reports the events to the console output of Jenkins.
     */
    private fun executePublisher(
        run: Run<*, *>, constants: HashMap<String, String>, result: Result?,
        listener: TaskListener, workspace: FilePath?
    ) {

        //Checks whether the paths of the JaCoCo files are correct
        if (!PublisherUtil.doCheckJacocoResultsPath(workspace!!, jacocoResultsPath!!)) {
            listener.logger.println("[Gamekins] JaCoCo folder is not correct")
            return
        }
        if (!PublisherUtil.doCheckJacocoCSVPath(workspace, jacocoCSVPath!!)) {
            listener.logger.println("[Gamekins] JaCoCo csv file could not be found")
            return
        }
        // mocoJSONPath is not specified, try the default path of MoCo plugin
        if (mocoJSONPath.isNullOrEmpty() && jacocoCSVPath!!.startsWith("**/")) {
            val buildFolder = jacocoCSVPath!!.substring(3).substringBefore("/")
            mocoJSONPath = "**/$buildFolder/moco/mutation/moco.json"
        }

        if (!PublisherUtil.doCheckMocoJSONPath(workspace, mocoJSONPath)) {
            constants["mocoJSONPath"] = ""
            MutationResults.mocoJSONAvailable = false
            listener.logger.println("[Gamekins] MoCo JSON file could not be found, mutation test challenge will " +
                    "not be created")
            listener.logger.println("[Gamekins] Please check moco.json file path configuration to enable mutation " +
                    "test challenge feature")
        } else {
            MutationResults.mocoJSONAvailable = true
            constants["mocoJSONPath"] = mocoJSONPath!!
        }

        constants["jacocoResultsPath"] = jacocoResultsPath!!
        constants["jacocoCSVPath"] = jacocoCSVPath!!
        constants["workspace"] = workspace.remote

        //Extracts the branch
        if (run.parent.parent is WorkflowMultiBranchProject) {
            constants["branch"] = run.parent.name
        } else {
            constants["branch"] = GitUtil.getBranch(workspace)
        }

        EventHandler.addEvent(BuildStartedEvent(constants["projectName"]!!, constants["branch"]!!, run))

        listener.logger.println("[Gamekins] Start")
        listener.logger.println("[Gamekins] Solve Challenges and generate new Challenges")

        //Computes the last changed classes
        val classes = PublisherUtil.retrieveLastChangedClasses(workspace, searchCommitCount, constants,
                listener = listener)

        //Generate some project statistics
        constants["projectCoverage"] = JacocoUtil.getProjectCoverage(workspace,
            constants["jacocoCSVPath"]!!.split("/".toRegex())
                    [constants["jacocoCSVPath"]!!.split("/".toRegex()).size - 1]).toString()
        constants["projectTests"] = JUnitUtil.getTestCount(workspace, run).toString()

        //Checks for each user his Challenges and generates new ones if needed
        var generated = 0
        var solved = 0
        var solvedAchievements = 0
        for (user in User.getAll()) {
            val results = PublisherUtil.checkUser(user, run, classes, constants, result, workspace, listener)
            generated += (if (results["generated"] != null) results["generated"] else 0)!!
            solved += (if (results["solved"] != null) results["solved"] else 0)!!
            solvedAchievements += (if (results["solvedAchievements"] != null) results["solvedAchievements"] else 0)!!
        }

        listener.logger.println("[Gamekins] Solved $solved Challenges and generated $generated Challenges")
        listener.logger.println("[Gamekins] Solved $solvedAchievements Achievements")
        listener.logger.println("[Gamekins] Update Statistics")

        //Updates the Statistics
        PublisherUtil.updateStatistics(run, constants, generated, solved, solvedAchievements, listener)

        EventHandler.addEvent(BuildFinishedEvent(constants["projectName"]!!, constants["branch"]!!, run))

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
            || !build.project.getProperty(GameJobProperty::class.java).activated
        ) {
            listener.logger.println(NOT_ACTIVATED)
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
    override fun perform(
        @Nonnull run: Run<*, *>, @Nonnull workspace: FilePath,
        @Nonnull launcher: Launcher, @Nonnull listener: TaskListener
    ) {
        val constants = HashMap<String, String>()
        if (run.parent.parent is WorkflowMultiBranchProject) {
            val project = run.parent.parent as WorkflowMultiBranchProject
            if (project.properties.get(GameMultiBranchProperty::class.java) == null
                || !project.properties.get(GameMultiBranchProperty::class.java).activated
            ) {
                listener.logger.println(NOT_ACTIVATED)
                return
            }
            constants["projectName"] = project.name
        } else {
            if (run.parent.getProperty(GameJobProperty::class.java) == null
                || !run.parent.getProperty(GameJobProperty::class.java).activated
            ) {
                listener.logger.println(NOT_ACTIVATED)
                return
            }
            constants["projectName"] = run.parent.name
        }

        constants["jacocoResultsPath"] = jacocoResultsPath!!
        constants["jacocoCSVPath"] = jacocoCSVPath!!
        executePublisher(run, constants, run.result, listener, workspace)
    }
}
