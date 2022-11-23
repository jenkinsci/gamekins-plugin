package org.gamekins.util

import java.io.Serializable

data class Pair<out A, out B>(
    val first: A,
    val second: B
) : Serializable {

    /**
     * Returns string representation of the [Pair] including its [first] and [second] values.
     */
    override fun toString(): String = "($first, $second)"
}