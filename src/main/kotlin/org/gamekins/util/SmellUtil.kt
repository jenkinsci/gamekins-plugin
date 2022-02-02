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

package org.gamekins.util

import hudson.model.TaskListener
import org.gamekins.file.FileDetails
import org.gamekins.smells.GamekinsClientLogOutput
import org.gamekins.smells.GamekinsIssueListener
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration
import org.sonarsource.sonarlint.core.commons.Language
import org.sonarsource.sonarlint.core.commons.log.ClientLogOutput
import java.io.File

/**
 * Util object for code and test smells.
 *
 * @author Philipp Straubinger
 * @since 0.5
 */
object SmellUtil {

    fun getSmellsOfFile(file: FileDetails, listener: TaskListener = TaskListener.NULL): List<Issue> {
        val globalConfig = StandaloneGlobalConfiguration.builder()
            .addEnabledLanguage(Language.JAVA)
            .addPlugin(Constants.SONAR_JAVA_PLUGIN)
            .build()
        val engine = StandaloneSonarLintEngineImpl(globalConfig)
        val analysisConfig = StandaloneAnalysisConfiguration.builder()
            .addInputFile(file)
            .setBaseDir(File(file.parameters.workspace.remote).toPath())
            .build()
        val issueListener = GamekinsIssueListener()
        val logOutput = GamekinsClientLogOutput()

        engine.analyze(analysisConfig, issueListener, logOutput, null)
        if (issueListener.issues.isEmpty()) {
            logOutput.messages.forEach {
                if (it.second == ClientLogOutput.Level.ERROR)
                    listener.logger.println("SonarLint ${it.second}: ${it.third}")
            }
        }
        return issueListener.issues
    }

    /**
     * @param startLine Starts with 1 not 0!
     */
    fun getLineContent(file: FileDetails, startLine: Int?, endLine: Int?): String {
        if (startLine == null || endLine == null) return file.contents()
        return file.contents()
            .split("\n")
            .filterIndexed { index, _ ->
                if (index < (startLine - 1)) {
                    false
                } else index <= (endLine - 1) }
            .reduce { acc, s -> "$acc\n$s" }
    }
}