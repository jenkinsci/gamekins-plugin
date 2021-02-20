package org.gamekins.mutation

import java.io.Serializable

data class MutationInfo (
    val mutationDetails: MutationDetails,
    val result: String,
    val uniqueID: Int,
) : Serializable