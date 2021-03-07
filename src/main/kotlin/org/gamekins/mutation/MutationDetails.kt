package org.gamekins.mutation

import java.io.Serializable

data class MutationDetails(
    val methodInfo: Map<String, String>,
    val instructionIndices: List<Int>,
    val mutationOperatorName: String,
    val mutatorID: String,
    val fileName: String,
    val loc: Int,
    val mutationDescription: String,
    val instructionOrder: List<Int>
) : Serializable