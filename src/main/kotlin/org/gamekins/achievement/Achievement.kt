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

package org.gamekins.achievement

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.util.JacocoUtil
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import java.util.HashMap

/**
 * Class for holding an individual [Achievement]. Described in a json file in path /resources/achievements/ and
 * initialized with [AchievementInitializer].
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class Achievement(val badgePath: String, private val fullyQualifiedFunctionName: String,
                  val description: String, val title: String) {

    @Transient private lateinit var callClass: KClass<out Any>
    @Transient private lateinit var callFunction: KCallable<*>
    var solvedTime: Long = 0

    init {
        initCalls()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Achievement) return false
        return other.badgePath == this.badgePath && other.description == this.description && other.title == this.title
    }

    override fun hashCode(): Int {
        var result = badgePath.hashCode()
        result = 31 * result + fullyQualifiedFunctionName.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + callClass.hashCode()
        result = 31 * result + callFunction.hashCode()
        result = 31 * result + solvedTime.hashCode()
        return result
    }

    /**
     * Initializes the [callClass] and the [callFunction], which are both transient. The reason is that Kotlin classes
     * are not on the white list for serialisation by Jenkins. All of the needed classes could be added manually, but
     * that is not feasible for reflection types.
     */
    private fun initCalls() {
        val reference = fullyQualifiedFunctionName.split("::")
        callClass = Class.forName(reference[0]).kotlin
        callFunction = callClass.members.single { it.name == reference[1] }
    }

    /**
     * Checks whether the [Achievement] is solved. Adds all parameters to an array to be passed into a vararg
     * parameter and executes the [callFunction] of the [callClass].
     */
    fun isSolved(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>, run: Run<*, *>,
                 user: User, workspace: FilePath, listener: TaskListener = TaskListener.NULL): Boolean {
        val array = arrayOf(callClass.objectInstance, classes, constants, run, user, workspace, listener)
        val result: Boolean = callFunction.call(*array) as Boolean
        if (result) solvedTime = System.currentTimeMillis()
        return result
    }

    /**
     * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
     */
    @Suppress("unused", "SENSELESS_COMPARISON")
    private fun readResolve(): Any {
        initCalls()
        return this
    }
}