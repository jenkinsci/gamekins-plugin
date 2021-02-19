package org.gamekins.mutation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import hudson.model.TaskListener
import java.io.File

data class MutationResults(val entries: Map<String, List<MutationInfo>>) {

    companion object {
        var retrievedResults: MutationResults? = null
        val mapper = jacksonObjectMapper()

        fun retrievedMutationsFromJson(
            path: String?, listener: TaskListener
        ): MutationResults? {
            try {
                if (retrievedResults == null) {
                    retrievedResults = mapper.readValue(File(path!!), MutationResults::class.java)
                }
                return retrievedResults
            } catch (e: Exception) {
                listener.logger.println("Error while reading mutation results from json file, please check MoCo JSON path")
                listener.logger.println(e.printStackTrace())
                return null
            }
        }
    }
}