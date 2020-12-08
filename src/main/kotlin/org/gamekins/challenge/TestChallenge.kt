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

package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.util.GitUtil
import org.gamekins.util.JacocoUtil
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import kotlin.collections.HashMap

/**
 * Specific [Challenge] to motivate the user to write a new test.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class TestChallenge(private val currentCommit: String, private val testCount: Int, private val user: User,
                    private val branch: String, private var constants: HashMap<String, String>) : Challenge {

    private val created = System.currentTimeMillis()
    private var solved: Long = 0
    private var testCountSolved = 0

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is TestChallenge) return false
        return true
    }

    override fun getConstants(): HashMap<String, String> {
        return constants
    }

    override fun getCreated(): Long {
        return created
    }

    override fun getScore(): Int {
        return 1
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun hashCode(): Int {
        var result = currentCommit.hashCode()
        result = 31 * result + testCount
        result = 31 * result + user.hashCode()
        result = 31 * result + branch.hashCode()
        result = 31 * result + constants.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + solved.hashCode()
        result = 31 * result + testCountSolved
        return result
    }

    /**
     * A [TestChallenge] is always solvable if the branch (taken form the [constants]), where it has been generated,
     * still exists in the project.
     */
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

    /**
     * A [TestChallenge] can only be solved in the branch (taken form the [constants]) where it has been generated,
     * because there can be different amounts of tests in different branches. The [TestChallenge] is solved if the
     * [testCount] during generation was less than the current amount of tests and the [user] has written a test since
     * the last commit ([currentCommit]).
     */
    override fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                          workspace: FilePath): Boolean {
        if (branch != constants["branch"]) return false
        try {
            val testCountSolved = JacocoUtil.getTestCount(workspace, run)
            if (testCountSolved <= testCount) {
                return false
            }
            val lastChangedFilesOfUser = GitUtil.getLastChangedTestFilesOfUser(
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

    /**
     * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
     */
    @Suppress("unused", "SENSELESS_COMPARISON")
    private fun readResolve(): Any {
        if (constants == null) constants = hashMapOf()
        return this
    }

    override fun toString(): String {
        return "Write a new test in branch $branch"
    }
}
