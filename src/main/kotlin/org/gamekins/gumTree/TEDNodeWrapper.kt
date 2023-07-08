package org.gamekins.gumTree

/**
 * A wrapper for the nodes of the ast to work with in the ted-algorithm.
 *
 * @author Michael Gruener
 * @since versionNumber
 */
class TEDNodeWrapper(
    var children: MutableList<TEDNodeWrapper>,
    var ordering: Int = -1,
    var leftMostLeaf: Int = -1,
    val nodeWrapper: NodeWrapper
) {

    /**
     * Returns the descendants of the node in post-order.
     */
    fun getDescendantsPostOrder(): Sequence<TEDNodeWrapper> {
        return sequence {
            for (child in children) {
                yieldAll(child.getDescendantsPostOrder())
            }
            yield(this@TEDNodeWrapper)
        }
    }

    override fun toString(): String {
        return "$nodeWrapper(ordering:$ordering,l:$leftMostLeaf)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TEDNodeWrapper

        return nodeWrapper.label.equals(other.nodeWrapper.label)
    }
}