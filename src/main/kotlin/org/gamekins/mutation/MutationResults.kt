package org.gamekins.mutation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import hudson.FilePath
import hudson.model.TaskListener
import jenkins.security.MasterToSlaveCallable
import java.io.IOException
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Paths

data class MutationResults(val entries: Map<String, List<MutationInfo>>) : Serializable {

    class GetMutationResultsCallable(private val jsonFilePath: FilePath)
        : MasterToSlaveCallable<String, IOException?>() {

        /**
         * Get json moco content from remote and return as string
         * or throws some exception.
         */
        override fun call(): String {
            return String(Files.readAllBytes(Paths.get(jsonFilePath.toURI())))
        }
    }

    companion object {
        var retrievedResults: MutationResults? = null
        val mapper = jacksonObjectMapper()

        fun retrievedMutationsFromJson(
            remotePath: FilePath, listener: TaskListener
        ): MutationResults? {
            return try {
                listener.logger.println(remotePath.toURI())
                listener.logger.println(remotePath)

                // Get json contents as string
                val jsonString: String = remotePath.act(GetMutationResultsCallable(remotePath))
                if (retrievedResults == null) {
                    retrievedResults = mapper.readValue(jsonString, MutationResults::class.java)
                }
                retrievedResults
            } catch (e: Exception) {
                listener.logger.println("Error while reading mutation results from json file, please check MoCo JSON path")
                listener.logger.println(e.printStackTrace())
                null
            }
        }
    }
}