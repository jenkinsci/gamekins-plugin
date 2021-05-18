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

package org.gamekins.util

import hudson.FilePath
import hudson.model.Run
import hudson.tasks.junit.TestResultAction
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

/**
 * Util object for interaction with JUnit.
 *
 * @author Philipp Straubinger
 * @since 0.2
 */
object JUnitUtil {

    /**
     * Returns the number of tests of a project in the [workspace] according to the JUnit results.
     */
    @JvmStatic
    fun getTestCount(workspace: FilePath): Int {
        try {
            val files: List<FilePath> = workspace.act(
                JacocoUtil.FilesOfAllSubDirectoriesCallable(workspace, "TEST-.+\\.xml")
            )
            var testCount = 0
            for (file in files) {
                testCount += getTestCountOfSingleJUnitResult(file)
            }
            return testCount
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * Returns the number of tests of a project run or according to the JUnit results.
     */
    @JvmStatic
    fun getTestCount(workspace: FilePath?, run: Run<*, *>?): Int {
        if (run != null) {
            val action = run.getAction(TestResultAction::class.java)
            if (action != null) {
                return action.totalCount
            }
        }
        return if (workspace == null) 0 else getTestCount(workspace)
    }

    /**
     * Returns the number of test of a specific test class, specified by the JUnit result [file].
     */
    fun getTestCountOfSingleJUnitResult(file: FilePath): Int {
        val document = Jsoup.parse(file.readToString(), "", Parser.xmlParser())
        val elements = document.select("testsuite")
        return elements.first().attr("tests").toInt()
    }

    /**
     * Returns the number of failed tests of a project in the [workspace] according to the JUnit results.
     */
    @JvmStatic
    fun getTestFailCount(workspace: FilePath): Int {
        try {
            val files: List<FilePath> = workspace.act(
                JacocoUtil.FilesOfAllSubDirectoriesCallable(workspace, "TEST-.+\\.xml")
            )
            var testCount = 0
            for (file in files) {
                testCount += getTestFailCountOfSingleJUnitResult(file)
            }
            return testCount
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * Returns the number of failed tests of a project run or according to the JUnit results.
     */
    @JvmStatic
    fun getTestFailCount(workspace: FilePath?, run: Run<*, *>?): Int {
        if (run != null) {
            val action = run.getAction(TestResultAction::class.java)
            if (action != null) {
                return action.failCount
            }
        }
        return if (workspace == null) 0 else getTestFailCount(workspace)
    }

    /**
     * Returns the number of failed test of a specific test class, specified by the JUnit result [file].
     */
    fun getTestFailCountOfSingleJUnitResult(file: FilePath): Int {
        val document = Jsoup.parse(file.readToString(), "", Parser.xmlParser())
        val elements = document.select("testsuite")
        return elements.first().attr("failures").toInt() + elements.first().attr("errors").toInt()
    }

    /**
     * Returns the list of test names (= methods) of a specific test class, specified by the JUnit result [file].
     */
    fun getTestNames(file: FilePath): HashSet<String> {
        val document = Jsoup.parse(file.readToString(), "", Parser.xmlParser())
        val elements = document.select("testcase")
        val set = hashSetOf<String>()
        elements.forEach { set.add(it.attr("name")) }
        return set
    }
}