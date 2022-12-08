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

import hudson.model.TaskListener
import org.gamekins.util.Constants
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.JacocoUtil
import java.io.File

/**
 * The internal representation of a class file received from git.
 *
 * @author Philipp Straubinger
 * @since 0.4
 */
class SourceFileDetails(parameters: Parameters,
                        filePath: String,
                        listener: TaskListener = TaskListener.NULL)
    : FileDetails(parameters, filePath)  {

    val coverage: Double
    val jacocoCSVFile: File
    val jacocoMethodFile: File
    val jacocoSourceFile: File

    init {
        val pathSplit = filePath.split("/".toRegex())

        //Build the paths to the JaCoCo files
        val jacocoPath = StringBuilder(parameters.remote)
        var i = 0
        while (pathSplit[i] != "src") {
            if (pathSplit[i].isNotEmpty()) jacocoPath.append("/").append(pathSplit[i])
            i++
        }
        jacocoCSVFile = File(jacocoPath.toString() + parameters.jacocoCSVPath.substring(2))
        if (!jacocoCSVFile.exists()) {
            listener.logger.println("[Gamekins] JaCoCoCSVPath: " + jacocoCSVFile.absolutePath
                    + Constants.EXISTS + jacocoCSVFile.exists())
        }

        jacocoPath.append(parameters.jacocoResultsPath.substring(2))
        if (!jacocoPath.toString().endsWith("/")) jacocoPath.append("/")
        jacocoPath.append(packageName).append("/")
        jacocoMethodFile = File("$jacocoPath$fileName.html")
        if (!jacocoMethodFile.exists()) {
            listener.logger.println("[Gamekins] JaCoCoMethodPath: "
                    + jacocoMethodFile.absolutePath + Constants.EXISTS + jacocoMethodFile.exists())
        }

        jacocoSourceFile = File("$jacocoPath$fileName.$fileExtension.html")
        if (!jacocoSourceFile.exists()) {
            listener.logger.println("[Gamekins] JaCoCoSourcePath: "
                    + jacocoSourceFile.absolutePath + Constants.EXISTS + jacocoSourceFile.exists())
        }

        coverage = JacocoUtil.getCoverageInPercentageFromJacoco(
            fileName,
            JacocoUtil.calculateCurrentFilePath(parameters.workspace, jacocoCSVFile)
        )
    }

    override fun filesExists(): Boolean {
        return super.filesExists() && jacocoCSVFile.exists() && jacocoMethodFile.exists() && jacocoSourceFile.exists()
    }

    override fun isTest(): Boolean {
        return false
    }

    /**
     * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
     */
    @Suppress("unused", "SENSELESS_COMPARISON")
    private fun readResolve(): Any {
        if (parameters == null) parameters = Parameters()
        return this
    }
}