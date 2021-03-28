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

package org.gamekins.mutation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import hudson.FilePath
import hudson.model.TaskListener
import jenkins.security.MasterToSlaveCallable
import java.io.IOException
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Retrieve collected mutation results from remote
 *
 * Return result from remote is a map from class name to list of its corresponding collected mutations
 *
 * @author Tran Phan
 * @since 0.3
 */
data class MutationResults(val entries: Map<String, Set<MutationInfo>>, val runID: String) : Serializable {

    class GetMutationResultsCallable(private val jsonFilePath: FilePath) :
        MasterToSlaveCallable<String, IOException?>() {

        /**
         * Get json moco content from remote and return as string
         * or throws an exception.
         */
        override fun call(): String {
            return String(Files.readAllBytes(Paths.get(jsonFilePath.toURI())))
        }
    }

    companion object {
        private var retrievedResults: MutationResults? = null
        var mapper = jacksonObjectMapper()
        var mocoJSONAvailable = true

        fun retrievedMutationsFromJson(
            remotePath: FilePath, listener: TaskListener
        ): MutationResults? {
            return try {
                // Get json contents as string
                val jsonString: String = remotePath.act(GetMutationResultsCallable(remotePath))
                retrievedResults = mapper.readValue(jsonString, MutationResults::class.java)
                retrievedResults
            } catch (e: Exception) {
                listener.logger.println("Error while reading mutation results from json file, please check MoCo JSON path")
                null
            }
        }
    }
}