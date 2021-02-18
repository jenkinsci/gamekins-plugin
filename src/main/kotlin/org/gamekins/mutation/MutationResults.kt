package org.gamekins.mutation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

data class MutationResults(val entries: Map<String, List<MutationInfo>>) {

    companion object {
        var retrievedResults: MutationResults? = null
        val mapper = jacksonObjectMapper()

        fun retrievedMutationsFromJson(
            path: String?
        ): MutationResults? {
            try {
                if (retrievedResults == null ) {
                    retrievedResults = mapper.readValue(File(path!!), MutationResults::class.java)
                }
                return retrievedResults
            } catch (e: Exception) {
                println(e.printStackTrace())
                throw RuntimeException("Error while reading mutation results from json file")
            }
        }
    }
}