package org.gamekins.gumTree

/**
 * Wrapper class for a mapping between two nodes.
 *
 * @author Michael Gruener
 * @since 0.6
 */
class Mapping(val sourceNode: NodeWrapper, val destinationNode: NodeWrapper) {
    override fun toString(): String {
        return "(" + sourceNode.lineNumber + "->" + destinationNode.lineNumber + ")"
    }
}