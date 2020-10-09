package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import io.jenkins.plugins.gamekins.util.GitUtil.getLastChangedTestFilesOfUser
import io.jenkins.plugins.gamekins.util.JacocoUtil.getTestCount
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import java.util.*

class TestChallenge(private val currentCommit: String, private val testCount: Int, private val user: User,
                    private val branch: String) : Challenge {

    private val created = System.currentTimeMillis()
    private var solved: Long = 0
    private var testCountSolved = 0

    override fun getCreated(): Long {
        return created
    }

    override fun getScore(): Int {
        return 1
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                            workspace: FilePath): Boolean {
        if (run.parent.parent is WorkflowMultiBranchProject) {
            for (workflowJob in (run.parent.parent as WorkflowMultiBranchProject).items) {
                if (workflowJob.name == branch) return true
            }
        } else {
            return true
        }
        return false
    }

    override fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                          workspace: FilePath): Boolean {
        if (branch != constants["branch"]) return false
        try {
            val testCountSolved = getTestCount(workspace, run)
            if (testCountSolved <= testCount) {
                return false
            }
            val lastChangedFilesOfUser = getLastChangedTestFilesOfUser(
                    workspace, user, 0, currentCommit, User.getAll())
            if (lastChangedFilesOfUser.isNotEmpty()) {
                solved = System.currentTimeMillis()
                this.testCountSolved = testCountSolved
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
        }
        return false
    }

    override fun printToXML(reason: String, indentation: String): String {
        var print = (indentation + "<TestChallenge created=\"" + created + "\" solved=\"" + solved
                + "\" tests=\"" + testCount + "\" testsAtSolved=\"" + testCountSolved)
        if (reason.isNotEmpty()) {
            print += "\" reason=\"$reason"
        }
        print += "\"/>"
        return print
    }

    override fun toString(): String {
        return "Write a new test in branch $branch"
    }
}
