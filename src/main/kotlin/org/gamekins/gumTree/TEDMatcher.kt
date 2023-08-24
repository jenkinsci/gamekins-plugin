package org.gamekins.gumTree

/**
 * Implementation of the "Simple Fast Algorithms for the Editing Distance Between Trees and Related Problems"
 * algorithm by Zhang and Sasha using dynamic programming.
 * Inspired by the implementation of jgrapht
 * (https://github.com/jgrapht/jgrapht/blob/master/jgrapht-core/src/main/java/org/jgrapht/alg/similarity/ZhangShashaTreeEditDistance.java)
 *
 * @author Michael Gruener
 * @since versionNumber
 */
class TEDMatcher(
    private val deleteCost: Double,
    private val insertCost: Double,
    private val changeCost: Double,
    private val sourceRoot: TEDNodeWrapper,
    private val destinationRoot: TEDNodeWrapper,
    private val sourceCount: Int,
    private val destinationCount: Int) {

    private lateinit var treeDistances: Array<DoubleArray>
    private lateinit var editOperationLists: Array<Array<ArrayList<EditOperation>?>>

    private val keyRootsSource = mutableMapOf<Int, TEDNodeWrapper>()
    private val keyRootsDestination = mutableMapOf<Int, TEDNodeWrapper>()
    private val sourceNodes = Array<TEDNodeWrapper?>(sourceCount + 1) {null}
    private val destinationNodes = Array<TEDNodeWrapper?>(destinationCount + 1) {null}


    /**
     * Calculates the tree edit distance between the source and destination tree using dynamic programming.
     * Adds the mappings to the mappings list.
     */
    fun findMappings(mappings: MutableList<Mapping>) {
        preprocessing(sourceRoot, keyRootsSource, sourceNodes)
        preprocessing(destinationRoot, keyRootsDestination, destinationNodes)
        treeDistances = Array(sourceCount) { DoubleArray(destinationCount) }
        editOperationLists = Array(sourceCount) { Array(destinationCount) { null } }

        val sourceKeyRootsIterator = keyRootsSource.values.asSequence().sortedBy { it.ordering }
        val destinationKeyRootsIterator = keyRootsDestination.values.asSequence().sortedBy { it.ordering }

        //calculate forest dist for all keyRoots
        for (i in sourceKeyRootsIterator) {
            for (j in destinationKeyRootsIterator) {
                treeDistance(i.ordering, j.ordering)
            }
        }
        //restore edit operations for smallest tree edit distance to get the mapping
        val finalEditOperations = editOperationLists[sourceCount - 1][destinationCount - 1]
        for (editOperation in finalEditOperations!!) {
            if (editOperation.operationType == OperationType.CHANGE
                && !editOperation.firstOperand!!.nodeWrapper.mapped
                && !editOperation.secondOperand!!.nodeWrapper.mapped ) {
                val sourceNode = editOperation.firstOperand.nodeWrapper
                val destinationNode = editOperation.secondOperand.nodeWrapper
                mappings.add(Mapping(sourceNode, destinationNode))

                sourceNode.mapped = true
                sourceNode.gotMatchedChildren()
                destinationNode.mapped = true
                destinationNode.gotMatchedChildren()
            }
        }
    }

    /**
     * Determines the ordering, l(i) and keyRoots for the algorithm.
     */
    private fun preprocessing(tree: TEDNodeWrapper,
                              keyRoots: MutableMap<Int, TEDNodeWrapper>,
                              nodes: Array<TEDNodeWrapper?>) {
        var i = 1
        val treeIterator = tree.getDescendantsPostOrder().iterator()
        while (treeIterator.hasNext()) {
            val currentNode = treeIterator.next()
            currentNode.ordering = i
            nodes[i] = currentNode

            //determine leftmost leaf
            if (currentNode.children.isEmpty()) currentNode.leftMostLeaf = i
            else currentNode.leftMostLeaf = currentNode.children.first().leftMostLeaf

            keyRoots[currentNode.leftMostLeaf] = currentNode

            i++
        }
    }

    /**
     * Calculates the tree edit distance between the source and destination tree using dynamic programming.
     */
    private fun treeDistance(i: Int, j: Int) {
        val li = sourceNodes[i]!!.leftMostLeaf
        val lj = destinationNodes[j]!!.leftMostLeaf

        val m = i - li + 2
        val n = j - lj + 2
        val forestdist = Array(m) { DoubleArray(n)}

        val cachedOperations: Array<Array<CacheEntry?>> = Array(m) { Array(n) { null } }

        val iOffset = li - 1
        val jOffset = lj - 1

        //initialize first column
        for (i1 in li..i) {
            val i1Node: TEDNodeWrapper = sourceNodes[i1]!!
            val iIndex = i1 - iOffset
            forestdist[iIndex][0] = forestdist[iIndex - 1][0] + deleteCost
            val entry = CacheEntry(
                iIndex - 1, 0, EditOperation(i1Node, null, OperationType.DELETE)
            )
            cachedOperations[iIndex][0] = entry
        }

        //initialize first row
        for (j1 in lj..j) {
            val j1Node: TEDNodeWrapper = destinationNodes[j1]!!
            val jIndex = j1 - jOffset
            forestdist[0][jIndex] = forestdist[0][jIndex - 1] + insertCost
            val entry = CacheEntry(
                0, jIndex - 1, EditOperation(null, j1Node, OperationType.INSERT)
            )
            cachedOperations[0][jIndex] = entry
        }

        for (i1 in li..i) {
            val i1Node: TEDNodeWrapper = sourceNodes[i1]!!
            val li1: Int = i1Node.leftMostLeaf
            for (j1 in lj..j) {
                val j1Node: TEDNodeWrapper = destinationNodes[j1]!!
                val lj1: Int = j1Node.leftMostLeaf
                val iIndex = i1 - iOffset
                val jIndex = j1 - jOffset
                if (li1 == li && lj1 == lj) {
                    val dist1: Double = forestdist[iIndex - 1][jIndex] + deleteCost
                    val dist2: Double = forestdist[iIndex][jIndex - 1] + insertCost
                    val dist3: Double = (forestdist[iIndex - 1][jIndex - 1]
                            + calculateChangeCost(i1Node, j1Node))
                    val result = dist1.coerceAtMost(dist2.coerceAtMost(dist3))
                    val entry: CacheEntry = when (result) {
                        dist1 -> { // remove operation
                            CacheEntry(
                                iIndex - 1, jIndex,
                                EditOperation(i1Node, null, OperationType.DELETE)
                            )
                        }
                        dist2 -> { // insert operation
                            CacheEntry(
                                iIndex, jIndex - 1,
                                EditOperation(null, j1Node, OperationType.INSERT)
                            )
                        }
                        else -> { // change operation
                            CacheEntry(
                                iIndex - 1, jIndex - 1,
                                EditOperation(i1Node, j1Node, OperationType.CHANGE)
                            )
                        }
                    }
                    cachedOperations[iIndex][jIndex] = entry
                    forestdist[iIndex][jIndex] = result
                    treeDistances[i1 - 1][j1 - 1] = result
                    editOperationLists[i1 - 1][j1 - 1] = restoreOperationsList(cachedOperations, iIndex, jIndex)
                } else {
                    val li1Index = li1 - 1 - iOffset
                    val lj1Index = lj1 - 1 - jOffset
                    val dist1: Double = forestdist[iIndex - 1][jIndex] + deleteCost
                    val dist2: Double = forestdist[iIndex][jIndex - 1] + insertCost
                    val dist3 = forestdist[li1Index][lj1Index] + treeDistances[i1 - 1][j1 - 1]
                    val result = dist1.coerceAtMost(dist2.coerceAtMost(dist3))
                    forestdist[iIndex][jIndex] = result
                    var entry: CacheEntry
                    when (result) {
                        dist1 -> {
                            entry = CacheEntry(
                                iIndex - 1, jIndex,
                                EditOperation(i1Node, null, OperationType.DELETE)
                            )
                        }
                        dist2 -> {
                            entry = CacheEntry(
                                iIndex, jIndex - 1,
                                EditOperation(null, j1Node, OperationType.INSERT)
                            )
                        }
                        else -> {
                            entry = CacheEntry(li1Index, lj1Index, null)
                            entry.treeDistanceI = i1 - 1
                            entry.treeDistanceJ = j1 - 1
                        }
                    }
                    cachedOperations[iIndex][jIndex] = entry
                }
            }
        }
    }

    /**
     * Calculates the cost of renaming one node to another.
     */
    private fun calculateChangeCost(source: TEDNodeWrapper, destination: TEDNodeWrapper): Double {
        if (source == destination) return 0.0
        return changeCost
    }


    /**
     * Restores the list of edit operations from the cached operations for the given indices.
     */
    private fun restoreOperationsList(
        cachedOperations: Array<Array<CacheEntry?>>, i: Int, j: Int
    ): ArrayList<EditOperation> {
        val result = ArrayList<EditOperation>()
        var op = cachedOperations[i][j]
        while (op != null) {
            if (op.editOperation == null) {
                result.addAll(editOperationLists[op.treeDistanceI][op.treeDistanceJ]!!)
            } else {
                result.add(op.editOperation!!)
            }
            op = cachedOperations[op.previousI][op.previousJ]
        }
        return result
    }

    /**
     * Stores the information of the EditOperation.
     */
    class EditOperation(val firstOperand: TEDNodeWrapper?,
                        val secondOperand: TEDNodeWrapper?,
                        val operationType: OperationType) {

        override fun toString(): String {
            return operationType.toString() + ":" + firstOperand.toString() + "->" + secondOperand.toString()
        }
    }

    /**
     * Type of edit operation
     */
    enum class OperationType {
        INSERT,
        DELETE,
        CHANGE
    }

    /**
     * Cache entry for the cached editOperations.
     */
    private class CacheEntry(
        val previousI: Int,
        val previousJ: Int,
        val editOperation: EditOperation?,
        var treeDistanceI: Int = -1,
        var treeDistanceJ: Int = -1
    )
}