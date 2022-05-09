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

import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.util.Constants.Parameters

/**
 * Specific [Challenge] to motivate the user to fix a failing build in Jenkins.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class BuildChallenge(private var parameters: Parameters) : Challenge {

    private val created = System.currentTimeMillis()
    private var solved: Long = 0

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is BuildChallenge) return false
        return true
    }

    override fun getParameters(): Parameters {
        return parameters
    }

    override fun getCreated(): Long {
        return created
    }

    override fun getName(): String {
        return "Build"
    }

    override fun getScore(): Int {
        return 1
    }

    override fun getSolved(): Long {
        return solved
    }

    override fun hashCode(): Int {
        var result = parameters.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + solved.hashCode()
        return result
    }

    /**
     * A [BuildChallenge] is always solvable since it depends on the status of the Jenkins [run].
     */
    override fun isSolvable(parameters: Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        return true
    }

    /**
     * A [BuildChallenge] is only solved if the status of the current [run] is [Result.SUCCESS].
     */
    override fun isSolved(parameters: Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (run.result == Result.SUCCESS) {
            solved = System.currentTimeMillis()
            return true
        }
        return false
    }

    override fun printToXML(reason: String, indentation: String): String {
        var print = "$indentation<BuildChallenge created=\"$created\" solved=\"$solved"
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
        return "Let the Build run successfully"
    }
}
