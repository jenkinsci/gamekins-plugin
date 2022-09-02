/*
 * Copyright 2022 Gamekins contributors
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

import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.util.Constants
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.GitUtil
import org.gamekins.util.JUnitUtil
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject

/**
 * Specific [Challenge] to motivate the user to write a new test.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class TestChallenge(data: Challenge.ChallengeGenerationData) : Challenge {

    private var currentCommit: String = data.headCommitHash!!
    private var testCount: Int = data.testCount!!
    private val user: User = data.user
    private var parameters: Parameters = data.parameters
    private val created = System.currentTimeMillis()
    private var solved: Long = 0
    private var testCountSolved = 0

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is TestChallenge) return false
        return true
    }

    override fun getParameters(): Parameters {
        return parameters
    }

    override fun getCreated(): Long {
        return created
    }

    override fun getName(): String {
        return "Test"
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
        result = 31 * result + parameters.branch.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + solved.hashCode()
        result = 31 * result + testCountSolved
        return result
    }

    /**
     * A [TestChallenge] is always solvable if the branch (taken form the [parameters]), where it has been generated,
     * still exists in the project.
     */
    override fun isSolvable(parameters: Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (run.parent.parent is WorkflowMultiBranchProject) {
            for (workflowJob in (run.parent.parent as WorkflowMultiBranchProject).items) {
                if (workflowJob.name == this.parameters.branch) return true
            }
        } else {
            return true
        }
        return false
    }

    /**
     * A [TestChallenge] can only be solved in the branch (taken form the [parameters]) where it has been generated,
     * because there can be different amounts of tests in different branches. The [TestChallenge] is solved if the
     * [testCount] during generation was less than the current amount of tests and the [user] has written a test since
     * the last commit ([currentCommit]).
     */
    override fun isSolved(parameters: Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (this.parameters.branch != parameters.branch) return false
        try {
            val testCountSolved = JUnitUtil.getTestCount(parameters.workspace, run)
            if (testCountSolved <= testCount) {
                return false
            }
            val lastChangedFilesOfUser = GitUtil.getLastChangedTestsOfUser(
                currentCommit, parameters, listener, GitUtil.GameUser(user),
                GitUtil.mapUsersToGameUsers(User.getAll()))
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
        if (parameters == null) parameters = Parameters()
        return this
    }

    override fun toString(): String {
        return "Write a new test in branch ${parameters.branch}"
    }

    override fun update(parameters: Parameters) {
        this.testCount = parameters.projectTests
        this.currentCommit = parameters.workspace.act(GitUtil.HeadCommitCallable(parameters.workspace.remote)).name
    }
}
