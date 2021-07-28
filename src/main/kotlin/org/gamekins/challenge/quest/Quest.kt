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

package org.gamekins.challenge.quest

import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.util.Constants

/**
 * A quest consists of a series of Challenge, which have to be solved one after the other.
 *
 * @author Philipp Straubinger
 * @since 0.4
 */
class Quest(val name: String, val steps: ArrayList<QuestStep>) {

    val created = System.currentTimeMillis()
    private var currentStep: Int = 0
    var solved: Long = 0

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Quest) return false

        return steps.map { it.challenge }.containsAll(other.steps.map { it.challenge })
    }

    /**
     * Returns the current [QuestStep] of the [Quest].
     */
    fun getCurrentStep(): QuestStep {
        return if (currentStep >= steps.size) steps[steps.size - 1] else steps[currentStep]
    }

    /**
     * Returns the number of the [currentStep].
     */
    fun getCurrentStepNumber(): Int {
        return currentStep
    }

    /**
     * Returns the last solved or first [QuestStep] of the [Quest].
     */
    fun getLastStep(): QuestStep {
        return if (currentStep == 0) steps[currentStep] else steps[currentStep - 1]
    }

    /**
     * Returns the score of the [Quest].
     */
    fun getScore(): Int {
        return steps.sumOf { it.challenge.getScore() } + steps.size
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + steps.hashCode()
        result = 31 * result + created.hashCode()
        result = 31 * result + currentStep
        result = 31 * result + solved.hashCode()
        return result
    }

    /**
     * Checks whether the current [QuestStep] is solved.
     */
    fun isCurrentStepSolved(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (steps.isEmpty()) return false
        if (steps[currentStep].challenge.isSolved(parameters, run, listener)) {
            currentStep++
            if (currentStep < steps.size) {
                steps[currentStep].challenge.update(parameters)
            }
            return true
        }

        return false
    }

    /**
     * Checks whether all Challenges of all [steps] are still solvable.
     */
    fun isSolvable(parameters: Constants.Parameters, run: Run<*, *>, listener: TaskListener): Boolean {
        if (steps.isEmpty()) return false
        for (i in currentStep until steps.size) {
            if (!steps[currentStep].challenge.isSolvable(parameters, run, listener)) return false
        }

        return true
    }

    /**
     * Checks whether all [steps] of the [Quest] are solved.
     */
    fun isSolved(): Boolean {
        if (steps.isEmpty()) return false
        if (currentStep >= steps.size) {
            solved = System.currentTimeMillis()
            return true
        }

        return false
    }

    /**
     * Returns the XML representation of the quest.
     */
    fun printToXML(reason: String, indentation: String): String {
        var print = "$indentation<Quest name=\"$name\" created=\"$created\" solved=\"$solved"
        if (reason.isNotEmpty()) {
            print += "\" reason=\"$reason"
        }
        print += "\">\n"
        print += "$indentation    <QuestSteps count=\"${steps.size}\">\n"
        for (step in steps) {
            print += step.printToXML("$indentation        ")
            print += "\n"
        }
        print += "$indentation    </QuestSteps>\n"
        print += "</Quest>"
        return print
    }

    override fun toString(): String {
        return name
    }
}