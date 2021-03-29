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

package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import kotlin.collections.HashMap

/**
 * Generated [Challenge] if the user has not developed something in the last commits. Only for information purposes.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
class DummyChallenge(private var constants: HashMap<String, String>) : Challenge {

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is DummyChallenge) return false
        return true
    }

    override fun getConstants(): HashMap<String, String> {
        return constants
    }

    /**
     * Only for dummy purposes, no need for further information.
     *
     * @see Challenge
     */
    override fun getCreated(): Long {
        return 0
    }

    override fun getName(): String {
        return "Dummy"
    }

    /**
     * Only for dummy purposes, no need for further information.
     *
     * @see Challenge
     */
    override fun getScore(): Int {
        return 0
    }

    /**
     * Only for dummy purposes, no need for further information.
     *
     * @see Challenge
     */
    override fun getSolved(): Long {
        return 0
    }

    override fun hashCode(): Int {
        return constants.hashCode()
    }

    /**
     * Only for dummy purposes, always solvable.
     *
     * @see Challenge
     */
    override fun isSolvable(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                            workspace: FilePath): Boolean {
        return true
    }

    /**
     * Only for dummy purposes, reevaluated every build.
     *
     * @see Challenge
     */
    override fun isSolved(constants: HashMap<String, String>, run: Run<*, *>, listener: TaskListener,
                          workspace: FilePath): Boolean {
        return true
    }

    /**
     * Only for dummy purposes, only returns the type of [Challenge].
     *
     * @see Challenge
     */
    override fun printToXML(reason: String, indentation: String): String {
        return "$indentation<DummyChallenge>"
    }

    /**
     * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
     */
    @Suppress("unused", "SENSELESS_COMPARISON")
    private fun readResolve(): Any {
        if (constants == null) constants = hashMapOf()
        return this
    }

    /**
     * Only for dummy purposes, no need for further information.
     *
     * @see Challenge
     */
    override fun toString(): String {
        return "You haven't developed anything lately"
    }

    override fun toEscapedString(): String {
        return toString()
    }
}
