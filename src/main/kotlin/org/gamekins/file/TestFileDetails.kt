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

package org.gamekins.file

import hudson.FilePath
import hudson.model.TaskListener
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.JUnitUtil
import org.gamekins.util.JacocoUtil
import java.io.File

/**
 * The internal representation of a test file received from git.
 *
 * @author Philipp Straubinger
 * @since 0.4
 */
class TestFileDetails(parameters: Parameters,
                      filePath: String,
                      listener: TaskListener = TaskListener.NULL)
    : FileDetails(parameters, filePath) {

    private val junitFile: File
    val testCount: Int
    private val testFailCount: Int
    private val testNames: HashSet<String>

    init {
        val files: List<FilePath> = parameters.workspace.act(
            JacocoUtil.FilesOfAllSubDirectoriesCallable(
                parameters.workspace, "TEST-$packageName\\.$fileName\\.xml")
        )

        if (files.isEmpty()) {
            listener.logger.println("[Gamekins] JUnit file of test $fileName does not exist!")
            junitFile = File("")
            testCount = -1
            testFailCount = -1
            testNames = hashSetOf()
        } else {
            junitFile = File(files.first().remote)
            testCount = JUnitUtil.getTestCountOfSingleJUnitResult(files.first())
            testFailCount = JUnitUtil.getTestCountOfSingleJUnitResult(files.first())
            testNames = JUnitUtil.getTestNames(files.first())
        }
    }

    override fun isTest(): Boolean {
        return true
    }
}