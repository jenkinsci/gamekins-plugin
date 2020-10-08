package io.jenkins.plugins.gamekins.statistics

import hudson.model.*
import io.jenkins.plugins.gamekins.GameUserProperty
import io.jenkins.plugins.gamekins.util.JacocoUtil
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import java.text.Collator
import java.util.*

class Statistics(job: AbstractItem) {
    private val projectName: String = job.name
    private val runEntries: ArrayList<RunEntry>?
    private var fullyInitialized: Boolean = false
    val isNotFullyInitialized: Boolean
        get() = !fullyInitialized || runEntries == null

    fun printToXML(): String {
        val print = StringBuilder()
        print.append("<Statistics project=\"").append(projectName).append("\">\n")
        val users: ArrayList<User> = ArrayList<User>(User.getAll())
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
        return print.toString()
    }

    private fun generateRunEntries(job: AbstractItem): ArrayList<RunEntry> {
        val entries = ArrayList<RunEntry>()
        when (job) {
            is WorkflowMultiBranchProject -> {
                val master = job.items.stream()
                        .filter { item: WorkflowJob -> item.name == "master" }.findFirst()
                if (master.isPresent) {
                    val list = master.get().builds
                    list.reverse()
                    for (workflowRun in list) {
                        entries.add(RunEntry(
                                workflowRun!!.getNumber(),
                                "master",
                                workflowRun.result,
                                workflowRun.startTimeInMillis,
                                0,
                                0,
                                JacocoUtil.getTestCount(null, workflowRun),
                                0.0))
                    }
                } else {
                    var count = 0
                    for (workflowJob in job.items) {
                        if (count >= Companion.RUN_TOTAL_COUNT) break
                        val list = workflowJob.builds
                        list.reverse()
                        for (workflowRun in list) {
                            if (count >= Companion.RUN_TOTAL_COUNT) break
                            entries.add(RunEntry(
                                    workflowRun!!.getNumber(),
                                    workflowJob.name,
                                    workflowRun.result,
                                    workflowRun.startTimeInMillis,
                                    0,
                                    0,
                                    JacocoUtil.getTestCount(null, workflowRun),
                                    0.0))
                            count++
                        }
                    }
                }
            }
            is WorkflowJob -> {
                val list = job.builds
                list.reverse()
                for (workflowRun in list) {
                    entries.add(RunEntry(
                            workflowRun!!.getNumber(),
                            "",
                            workflowRun.result,
                            workflowRun.startTimeInMillis,
                            0,
                            0,
                            JacocoUtil.getTestCount(null, workflowRun),
                            0.0))
                }
            }
            is AbstractProject<*, *> -> {
                val list = job.builds
                list.reverse()
                for (abstractBuild in list) {
                    entries.add(RunEntry(
                            abstractBuild!!.getNumber(),
                            "",
                            abstractBuild.result,
                            abstractBuild.startTimeInMillis,
                            0,
                            0,
                            JacocoUtil.getTestCount(null, abstractBuild),
                            0.0))
                }
            }
        }
        entries.sortWith { obj: RunEntry, o: RunEntry -> obj.compareTo(o) }
        return entries
    }

    fun addRunEntry(job: AbstractItem, branch: String, entry: RunEntry, listener: TaskListener) {
        addPreviousEntries(job, branch, entry.runNumber - 1, listener)
        runEntries!!.add(entry)
        listener.logger.println(entry.printToXML(""))
        runEntries.sortWith(Comparator { obj: RunEntry, o: RunEntry -> obj.compareTo(o) })
    }

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
                for (workflowJob in job.items) {
                    if (workflowJob.name == branch) {
                        for (workflowRun in workflowJob.builds) {
                            if (workflowRun.getNumber() == number) {
                                runEntries.add(RunEntry(
                                        workflowRun.getNumber(),
                                        workflowJob.name,
                                        workflowRun.result,
                                        workflowRun.startTimeInMillis,
                                        0,
                                        0,
                                        JacocoUtil.getTestCount(null, workflowRun),
                                        0.0))
                                return
                            }
                        }
                    }
                }
            }
            is WorkflowJob -> {
                for (workflowRun in job.builds) {
                    if (workflowRun.getNumber() == number) {
                        runEntries.add(RunEntry(
                                workflowRun.getNumber(),
                                "",
                                workflowRun.result,
                                workflowRun.startTimeInMillis,
                                0,
                                0,
                                JacocoUtil.getTestCount(null, workflowRun),
                                0.0))
                        return
                    }
                }
            }
            is AbstractProject<*, *> -> {
                for (abstractBuild in job.builds) {
                    if (abstractBuild.getNumber() == number) {
                        runEntries.add(RunEntry(
                                abstractBuild.getNumber(),
                                "",
                                abstractBuild.result,
                                abstractBuild.startTimeInMillis,
                                0,
                                0,
                                JacocoUtil.getTestCount(null, abstractBuild),
                                0.0))
                        return
                    }
                }
            }
        }
    }

    class RunEntry(val runNumber: Int, val branch: String, val result: Result?, val startTime: Long,
                   private val generatedChallenges: Int, private val solvedChallenges: Int, val testCount: Int, val coverage: Double) : Comparable<RunEntry> {
        fun printToXML(indentation: String): String {
            return (indentation + "<Run number=\"" + runNumber + "\" branch=\"" + branch +
                    "\" result=\"" + (result?.toString() ?: "NULL") + "\" startTime=\"" +
                    startTime + "\" generatedChallenges=\"" + generatedChallenges +
                    "\" solvedChallenges=\"" + solvedChallenges + "\" tests=\"" + testCount
                    + "\" coverage=\"" + coverage + "\"/>")
        }

        override fun compareTo(o: RunEntry): Int {
            val result = Collator.getInstance().compare(branch, o.branch)
            return if (result == 0) runNumber.compareTo(o.runNumber) else result
        }
    }

    init {
        runEntries = generateRunEntries(job)
        fullyInitialized = true
    }

    companion object {
        private const val RUN_TOTAL_COUNT = 200
    }
}
