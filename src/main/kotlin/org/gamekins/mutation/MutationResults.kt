package org.gamekins.mutation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

data class MutationResults(val entries: Map<String, List<MutationInfo>>) {

    companion object {
        fun retrievedMutationResultsFromJson(
            path: String = "/Users/phantran/Study/Passau/Thesis/gamekins/target/moco.json"
        ): MutationResults {
            val mapper = jacksonObjectMapper()
            try {
                return mapper.readValue(File(path), MutationResults::class.java)
            } catch (e: Exception) {
                println(e.printStackTrace())
                throw RuntimeException("Error while reading mutation results from csv file")
            }
        }
    }
}