package org.gamekins.gumTree

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.type.PrimitiveType
import java.util.*

/**
 * A wrapper for the nodes of the ast to work with in the gumTree-algorithm
 *
 * @author Michael Gruener
 * @since versionNumber
 */
class NodeWrapper(
    val node: Node, var height: Int = -1, val children: MutableList<NodeWrapper> = LinkedList(),
    var mapped: Boolean = false, var timesMapped: Int = 0, val parent: NodeWrapper? = null,
    var lineNumber: Int = -1, var hasMatchedChildren: Boolean = false,
    var label: String? = null, var identifier: String? = null
) {

    /**
     * Initializes the nodeWrapper and returns the height of the node.
     */
    fun init(): Int {
        return if (height != -1) {
            height
        } else {
            if (!node.begin.isEmpty) lineNumber = node.begin.get().line
            label = node.metaModel.typeName
            identifier = getIdentifier(this.node)
            height = if (children.isNotEmpty()) {
                children.maxOf { node -> node.init() } + 1
            } else {
                1
            }
            height
        }
    }

    /**
     * Returns the identifier of the node if it has one.
     */
    private fun getIdentifier(node: Node): String? {
        var label = ""
        if (node is Name) label = node.identifier
        if (node is SimpleName) label = node.identifier
        if (node is StringLiteralExpr) label = node.asString()
        if (node is BooleanLiteralExpr) label = java.lang.Boolean.toString(node.value)
        if (node is LiteralStringValueExpr) label = node.value
        if (node is PrimitiveType) label = node.asString()
        if (node is Modifier) label = node.keyword.asString()

        return if (label == "") null
        else label
    }

    /**
     * Adds the children of the node to the children list.
     */
    fun addChildren() {
        val childNodes = node.childNodes
        for (node in childNodes) {
            children.add(NodeWrapper(node, parent = this))
        }
        for (node in children) {
            node.addChildren()
        }
    }

    /**
     * Increments the counter for timesMapped by one.
     */
    fun incrementMapping() {
        timesMapped += 1
    }

    /**
     * Returns the descendants of the node in pre-order without the root.
     */
    fun getDescendantsPreOrderWithoutRoot(): Sequence<NodeWrapper> {
        return sequence {
            for (child in children) {
                yieldAll(child.returnDescendantsPreOrder())
            }
        }
    }

    /**
     * Returns the descendants of the node in pre-order.
     */
    private fun returnDescendantsPreOrder(): Sequence<NodeWrapper> {
        return sequence {
            yield(this@NodeWrapper)
            for (child in children) {
                yieldAll(child.returnDescendantsPreOrder())
            }
        }
    }

    /**
     * Returns the descendants of the node in post-order.
     */
    fun getDescendantsPostOrder(): Sequence<NodeWrapper> {
        return sequence {
            for (child in children) {
                yieldAll(child.getDescendantsPostOrder())
            }
            yield(this@NodeWrapper)
        }
    }

    /**
     * Sets the hasMatchedChildren on this node and all its parents to true.
     */
    fun gotMatchedChildren() {
        hasMatchedChildren = true
        if (parent != null && !parent.hasMatchedChildren) {
            parent.gotMatchedChildren()
        }
    }

    /**
     * Returns a copy of the nodeWrapper as a TEDNodeWrapper without the children that have been matched.
     */
    fun getTEDWrapperWithoutMatchedChildren(): TEDNodeWrapper {
        val clone = TEDNodeWrapper(
            children = mutableListOf(),
            nodeWrapper = this@NodeWrapper
        )
        for (child in children) {
            if (!child.mapped) {
                val clonedChild = child.getTEDWrapperWithoutMatchedChildren()
                clone.children.add(clonedChild)
            }
        }
        return clone
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NodeWrapper

        return node == other.node
    }

    override fun hashCode(): Int {
        return node.hashCode()
    }

    override fun toString(): String {
        return label + (identifier?.let { "($identifier)" } ?: "")
    }
}