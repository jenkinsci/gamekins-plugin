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

package org.gamekins.statistics

import hudson.model.*
import org.gamekins.GameUserProperty
import org.gamekins.util.JUnitUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import java.text.Collator
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

/**
 * Class for evaluation purposes. Displays the information about the users and the runs in an XML format.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class Statistics(job: AbstractItem) {

    private var fullyInitialized: Boolean = false
    private val projectName: String = job.name
    private val runEntries: MutableList<RunEntry>?

    init {
        runEntries = generateRunEntries(job)
        fullyInitialized = true
    }

    companion object {
        private const val RUN_TOTAL_COUNT = 200
    }

    /**
     * Adds the number of [additionalGenerated] Challenges after rejection to the current run of [branch].
     */
    fun addGeneratedAfterRejection(branch: String, additionalGenerated: Int) {
        val entry = runEntries!!.lastOrNull { it.branch == branch }
        entry?.generatedChallenges = entry?.generatedChallenges?.plus(additionalGenerated)!!
    }

    /**
     * If Gamekins is not enabled when the [job] is created, this method adds the previous entries to the [Statistics]
     * according to the [branch] (only if [job] is of type [WorkflowMultiBranchProject]) recursively with the [number]
     * to the [runEntries]. It can happen that a job is aborted or fails and Gamekins is not executed. For this
     * problem, this method also adds old runs until the point where it last stopped. The [listener] reports the events
     * to the console output of Jenkins.
     */
    private fun addPreviousEntries(job: AbstractItem, branch: String, number: Int, listener: TaskListener) {
        if (number <= 0) return
        for (entry in runEntries!!) {
            if (entry.branch == branch && entry.runNumber == number) return
        }
        addPreviousEntries(job, branch, number - 1, listener)
        if (runEntries.size > 0) {
            listener.logger.println(runEntries[runEntries.size - 1].printToXML(""))
        }
        when (job) {
            is WorkflowMultiBranchProject -> {
                addPreviousEntriesWorkflowMultiBranchProject(job, branch, number)
            }
            is WorkflowJob -> {
                addPreviousEntriesWorkflowJob(job, number)
            }
            is AbstractProject<*, *> -> {
                addPreviousEntriesAbstractProject(job, number)
            }
        }
    }

    /**
     * Adds the build with [number] of the [job] to the [runEntries].
     */
    private fun addPreviousEntriesAbstractProject(job: AbstractProject<*, *>, number: Int) {

        for (abstractBuild in job.builds) {
            if (abstractBuild.getNumber() == number) {
                runEntries?.add(RunEntry(
                    abstractBuild.getNumber(),
                    "",
                    abstractBuild.result,
                    abstractBuild.startTimeInMillis,
                    0,
                    0,
                    0,
                    JUnitUtil.getTestCount(null, abstractBuild),
                    0.0))
                return
            }
        }
    }

    /**
     * Adds the build with [number] of the [job] to the [runEntries].
     */
    private fun addPreviousEntriesWorkflowJob(job: WorkflowJob, number: Int) {

        for (workflowRun in job.builds) {
            if (workflowRun.getNumber() == number) {
                runEntries?.add(RunEntry(
                    workflowRun.getNumber(),
                    "",
                    workflowRun.result,
                    workflowRun.startTimeInMillis,
                    0,
                    0,
                    0,
                    JUnitUtil.getTestCount(null, workflowRun),
                    0.0))
                return
            }
        }
    }

    /**
     * Adds the build with [number] of the [job] and [branch] to the [runEntries].
     */
    private fun addPreviousEntriesWorkflowMultiBranchProject(job: WorkflowMultiBranchProject, branch: String,
                                                             number: Int) {

        for (workflowJob in job.items) {
            if (workflowJob.name == branch) {
                for (workflowRun in workflowJob.builds) {
                    if (workflowRun.getNumber() == number) {
                        runEntries?.add(RunEntry(
                            workflowRun.getNumber(),
                            workflowJob.name,
                            workflowRun.result,
                            workflowRun.startTimeInMillis,
                            0,
                            0,
                            0,
                            JUnitUtil.getTestCount(null, workflowRun),
                            0.0))
                        return
                    }
                }
            }
        }
    }

    /**
     * Adds a new [entry] for a [job] and [branch] to the [Statistics]. Checks and optionally adds previous entries.
     * The [listener] reports the events to the console output of Jenkins.
     */
    fun addRunEntry(job: AbstractItem, branch: String, entry: RunEntry, listener: TaskListener) {
        addPreviousEntries(job, branch, entry.runNumber - 1, listener)
        runEntries!!.add(entry)
        listener.logger.println(entry.printToXML(""))
        runEntries.sortWith(Comparator { obj: RunEntry, o: RunEntry -> obj.compareTo(o) })
    }

    /**
     * Generates the [runEntries] during the creation of the the property of the [job]. Only adds the entries for the
     * branch master of a [WorkflowMultiBranchProject] to the [runEntries].
     */
    private fun generateRunEntries(job: AbstractItem): ArrayList<RunEntry> {
        val entries = ArrayList<RunEntry>()
        val list = mutableListOf<Run<*, *>>()
        var branch = ""
        when (job) {
            is WorkflowMultiBranchProject -> {
                val master = job.items.firstOrNull { it.name == "master" }
                if (master != null) {
                    master.builds.forEach { list.add(it) }
                    branch = "master"
                }
            }
            is WorkflowJob -> {
                job.builds.forEach { list.add(it) }
            }
            is AbstractProject<*, *> -> {
                job.builds.forEach { list.add(it) }
            }
        }

        if (list.isEmpty() && job is WorkflowMultiBranchProject) {
            entries.addAll(generateRunEntriesWorkflowMultiBranchProject(job))
        } else {
            list.reverse()
            for (run in list) {
                entries.add(RunEntry(
                    run.getNumber(),
                    branch,
                    run.result,
                    run.startTimeInMillis,
                    0,
                    0,
                    0,
                    JUnitUtil.getTestCount(null, run),
                    0.0))
            }
        }

        entries.sortedWith(compareBy({it.branch}, {it.runNumber}))
        return entries
    }

    /**
     * Generates [RUN_TOTAL_COUNT] entries from the [job] if no master branch was found.
     */
    private fun generateRunEntriesWorkflowMultiBranchProject(job: WorkflowMultiBranchProject): ArrayList<RunEntry> {
        val entries = ArrayList<RunEntry>()
        var count = 0
        for (workflowJob in job.items) {
            if (count >= RUN_TOTAL_COUNT) break
            val runList = mutableListOf<Run<*, *>>()
            workflowJob.builds.forEach { runList.add(it) }
            runList.reverse()
            for (workflowRun in runList) {
                if (count >= RUN_TOTAL_COUNT) break
                entries.add(RunEntry(
                    workflowRun.getNumber(),
                    workflowJob.name,
                    workflowRun.result,
                    workflowRun.startTimeInMillis,
                    0,
                    0,
                    0,
                    JUnitUtil.getTestCount(null, workflowRun),
                    0.0))
                count++
            }
        }

        return entries
    }

    /**
     * Returns the last run of a [branch].
     */
    fun getLastRun(branch: String): RunEntry? {
        return runEntries?.filter { it.branch == branch }?.maxByOrNull {it.runNumber }
    }

    /**
     * If the [Statistics] is interrupted during initialization, it is triggered again.
     */
    fun isNotFullyInitialized(): Boolean {
        return !fullyInitialized || runEntries == null
    }

    /**
     * Returns an XML representation of the [Statistics].
     */
    fun printToXML(): String {
        val print = StringBuilder()
        print.append("<Statistics project=\"").append(projectName).append("\">\n")

        val users: ArrayList<User> = ArrayList(User.getAll())
        users.removeIf { user: User -> !user.getProperty(GameUserProperty::class.java).isParticipating(projectName) }
        print.append("    <Users count=\"").append(users.size).append("\">\n")
        for (user in users) {
            val property = user.getProperty(GameUserProperty::class.java)
            if (property != null && property.isParticipating(projectName)) {
                print.append(property.printToXML(projectName, "        ")).append("\n")
            }
        }
        print.append("    </Users>\n")

        runEntries!!.removeIf { obj: RunEntry? -> Objects.isNull(obj) }
        print.append("    <Runs count=\"").append(runEntries.size).append("\">\n")
        for (entry in runEntries) {
            print.append(entry.printToXML("        ")).append("\n")
        }
        print.append("    </Runs>\n")

        print.append("</Statistics>")

        return replaceClassesInString(print.toString())
    }

    fun replaceClassesInString(file: String): String {
        var regex = "class=\"[^\"]+\"".toRegex()
        var matchResults = regex.findAll(file)
        var map = hashMapOf<String, String>()
        var resultString = file

        for (result in matchResults) {
            map.putIfAbsent(result.value, UUID.randomUUID().toString())
            resultString = resultString.replace(result.value, "class=\"${map[result.value]}\"")
        }

        regex = "project=\"[^\"]+\"".toRegex()
        matchResults = regex.findAll(file)
        map = hashMapOf()

        for (result in matchResults) {
            map.putIfAbsent(result.value, UUID.randomUUID().toString())
            resultString = resultString.replace(result.value, "project=\"${map[result.value]}\"")
        }

        regex = "branch=\"[^\"]+\"".toRegex()
        matchResults = regex.findAll(file)
        map = hashMapOf()
        map["branch=\"master\""] = "master"
        map["branch=\"main\""] = "main"

        for (result in matchResults) {
            map.putIfAbsent(result.value, UUID.randomUUID().toString())
            resultString = resultString.replace(result.value, "branch=\"${map[result.value]}\"")
        }

        return resultString
    }

    /**
     * Represents a run of Jenkins job.
     *
     * @author Philipp Straubinger
     * @since 1.0
     */
    class RunEntry(val runNumber: Int, val branch: String, val result: Result?, val startTime: Long,
                   var generatedChallenges: Int, val solvedChallenges: Int, var solvedAchievements: Int,
                   val testCount: Int, val coverage: Double)
        : Comparable<RunEntry> {

        /**
         * Returns an XML representation of the [RunEntry].
         */
        fun printToXML(indentation: String): String {
            return (indentation + "<Run number=\"" + runNumber + "\" branch=\"" + branch +
                    "\" result=\"" + (result?.toString() ?: "NULL") + "\" startTime=\"" +
                    startTime + "\" generatedChallenges=\"" + generatedChallenges +
                    "\" solvedChallenges=\"" + solvedChallenges + "\" solvedAchievements=\"" + solvedAchievements +
                    "\" tests=\"" + testCount + "\" coverage=\"" + coverage + "\"/>")
        }

        /**
         * Compares two [RunEntry] with first [branch] and second [runNumber].
         *
         * @see [Comparable.compareTo]
         */
        override fun compareTo(other: RunEntry): Int {
            val result = Collator.getInstance().compare(branch, other.branch)
            return if (result == 0) runNumber.compareTo(other.runNumber) else result
        }

        /**
         * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
         */
        @Suppress("unused", "SENSELESS_COMPARISON")
        private fun readResolve(): Any {
            if (solvedAchievements == null) solvedAchievements = 0
            return this
        }
    }
}
