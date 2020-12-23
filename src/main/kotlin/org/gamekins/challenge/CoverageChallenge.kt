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
import org.gamekins.util.JacocoUtil
import org.gamekins.util.JacocoUtil.ClassDetails

/**
 * Abstract class to generate basic information about the class used for generating a [CoverageChallenge].
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
abstract class CoverageChallenge(val classDetails: ClassDetails, workspace: FilePath?)
    : Challenge {

    protected val coverage: Double
    protected val fullyCoveredLines: Int
    protected val notCoveredLines: Int
    protected val partiallyCoveredLines: Int
    protected var solvedCoverage = 0.0
    private val created = System.currentTimeMillis()
    private var solved: Long = 0

    /**
     * Calculates the number of fully, partially and not covered lines, and the coverage of the class itself.
     */
    init {
        val document = JacocoUtil.generateDocument(JacocoUtil.calculateCurrentFilePath(workspace!!,
                classDetails.jacocoSourceFile, classDetails.workspace))
        fullyCoveredLines = JacocoUtil.calculateCoveredLines(document, "fc")
        partiallyCoveredLines = JacocoUtil.calculateCoveredLines(document, "pc")
        notCoveredLines = JacocoUtil.calculateCoveredLines(document, "nc")
        coverage = classDetails.coverage
    }

    override fun getConstants(): HashMap<String, String> {
        return classDetails.constants
    }

    override fun getCreated(): Long {
        return created
    }

    /**
     * Returns the name of the class of the current [CoverageChallenge].
     */
    abstract fun getName(): String

    override fun getSolved(): Long {
        return solved
    }

    override fun printToXML(reason: String, indentation: String): String {
        var print = (indentation + "<" + getName() + " created=\"" + created + "\" solved=\"" + solved
                + "\" class=\"" + classDetails.className + "\" coverage=\"" + coverage
                + "\" coverageAtSolved=\"" + solvedCoverage)
        if (reason.isNotEmpty()) {
            print += "\" reason=\"$reason"
        }
        print += "\"/>"
        return print
    }

    /**
     * Needed because of automatically generated getter and setter in Kotlin.
     */
    fun setSolved(newSolved: Long) {
        solved = newSolved
    }
}
