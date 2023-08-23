package org.gamekins.gumTree

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.*
import hudson.FilePath
import org.gamekins.util.Constants
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.MutationUtil.MutationData
import java.util.*
import kotlin.math.max
import kotlin.math.min


/**
 * Implementation of the "Fine-grained and Accurate Source Code Differencing" algorithm by Falleri et al.
 * with small adjustments for the TED distance.
 * Uses the javaParser and symbolSolver to generate the ASTs.
 *
 * @author Michael Gruener
 * @since versionNumber
 */
class GumTree {

    private val minHeight = 2
    private val minDice = 0.5
    private val maxSize = 100

    /**
     * Tries to find a suitable mapping with the information of the given mutationData.
     * Returns null if no suitable mapping was found.
     */
    fun findMapping(mutationData: MutationData, parameters: Parameters): MutationData? {
        if (mutationData.sourceCode.isEmpty()) return null
        val source = JavaParser.parse(mutationData.sourceCode)
        val destination = JavaParser.parse(mutationData.sourceFile, mutationData.mutatedClass, parameters)
        val mappings = map(source, destination)

        return updateMutationData(mutationData, mappings, destination, parameters)
    }

    /**
     * Tries to update the mutationData with the calculated mappings.
     * Returns null if an error occurred while updating the mutationData.
     */
    private fun updateMutationData(
        mutationData: MutationData,
        mappings: List<Mapping>,
        destinationCU: CompilationUnit,
        parameters: Parameters
    ): MutationData? {
        val lineNumber = findMostFrequentDestinationLineNumber(mappings, mutationData.lineNumber) ?: return null
        val methodDeclaration = getMethodDeclaration(lineNumber, mappings) ?: return null
        val classDeclaration = getClassDeclaration(methodDeclaration) ?: return null

        val mutatedClass = getClassName(classDeclaration)
        val mutatedMethod = if (methodDeclaration.node is ConstructorDeclaration) "&lt;init&gt;"
        else (methodDeclaration.node as MethodDeclaration).nameAsString
        var mutatedMethodDescription = if (methodDeclaration.node is ConstructorDeclaration) "()V"
        else MethodNameConverter().getByteCodeRepresentation(methodDeclaration.node as MethodDeclaration)
        if (mutatedMethodDescription == null || mutationData.methodDescription.contains("$")) {
            mutatedMethodDescription = retrieveMethodDescriptionThroughPitReport(lineNumber, parameters) ?: return null
        }

        return MutationData(
            mutationData.detected,
            mutationData.status,
            mutationData.numberOfTestsRun,
            mutationData.sourceFile,
            mutatedClass,
            mutatedMethod,
            mutatedMethodDescription,
            lineNumber,
            mutationData.mutator,
            mutationData.killingTest,
            mutationData.description,
            destinationCU.toString()
        )
    }

    /**
     * Searches for the closest methodDeclaration of the given mutated line of code.
     */
    private fun getMethodDeclaration(lineNumber: Int, mappings: List<Mapping>): NodeWrapper? {
        val mapping = mappings.find { it.destinationNode.lineNumber == lineNumber } ?: return null
        var nodeWrapper: NodeWrapper? = mapping.destinationNode
        while (nodeWrapper != null) {
            if (nodeWrapper.node is MethodDeclaration) {
                return nodeWrapper
            }
            nodeWrapper = nodeWrapper.parent
        }
        return null
    }

    /**
     * Searches for the closest classDeclaration of the given methodDeclaration.
     */
    private fun getClassDeclaration(methodDeclaration: NodeWrapper): NodeWrapper? {
        var node: NodeWrapper? = methodDeclaration
        while (node != null) {
            node = node.parent
            if (node != null && node.node is ClassOrInterfaceDeclaration) {
                return node
            }
        }
        return null
    }

    /**
     * Returns the fully qualified name of a class or enum.
     */
    private fun getClassName(declaration: NodeWrapper): String {
        var mutatedClass = ""
        if (declaration.node is ClassOrInterfaceDeclaration) mutatedClass = declaration.node.fullyQualifiedName.get()
        if (declaration.node is EnumDeclaration) mutatedClass = declaration.node.fullyQualifiedName.get()
        if (declaration.node.hasParentNode()
            && (declaration.node.parentNode.get() is ClassOrInterfaceDeclaration
                    || declaration.node.parentNode.get() is RecordDeclaration)
        ) {
            mutatedClass = replaceLastCharacter(mutatedClass, '.', '$')
        }
        return mutatedClass
    }

    /**
     * Finds the most frequent destination line number that corresponds to a given source line number
     * in a list of mappings.
     */
    private fun findMostFrequentDestinationLineNumber(mappings: List<Mapping>, sourceLineNumber: Int): Int? {
        val matchingMappings = mappings.filter { it.sourceNode.lineNumber == sourceLineNumber }
        if (matchingMappings.isEmpty()) {
            return null
        }

        val destinationLineNumbersCount = matchingMappings
            .groupBy { it.destinationNode.lineNumber }
            .mapValues { (_, group) -> group.size }

        val maxCount = destinationLineNumbersCount.values.maxOrNull() ?: 0

        val mostFrequentLineNumbers = destinationLineNumbersCount
            .filterValues { it == maxCount }
            .keys

        return mostFrequentLineNumbers.firstOrNull()
    }

    /**
     * Replaces the last occurrence of a given character with the new character.
     */
    private fun replaceLastCharacter(input: String, oldChar: Char, newChar: Char): String {
        val lastIndex = input.lastIndexOf(oldChar)
        return if (lastIndex >= 0) {
            input.substring(0, lastIndex) + newChar + input.substring(lastIndex + 1)
        } else {
            input
        }
    }

    /**
     * Tries to retrieve the methodDescription from the pit report for the given method and line number.
     */
    private fun retrieveMethodDescriptionThroughPitReport(lineNumber: Int, parameters: Parameters): String? {
        val mutationReport = FilePath(
            parameters.workspace.channel,
            parameters.workspace.remote + Constants.Mutation.REPORT_PATH
        )
        if (!mutationReport.exists()) return null
        val mutants = mutationReport.readToString().split("\n").filter { it.startsWith("<mutation ") }
        if (mutants.isEmpty()) return null
        for (mutant in mutants) {
            if (mutant.contains("<lineNumber>$lineNumber</lineNumber>"))
                return """<methodDescription>(.*)</methodDescription>""".toRegex().find(mutant)!!.groupValues[1]
        }
        return null
    }

    /**
     * Calculates the mapping for the compilationUnits using the gumTree algorithm.
     */
    private fun map(source: CompilationUnit, destination: CompilationUnit): List<Mapping> {

        val mappings = mutableListOf<Mapping>()

        //initialize Trees
        val sourceRoot = NodeWrapper(source.findRootNode())
        sourceRoot.addChildren()
        sourceRoot.init()
        val destinationRoot = NodeWrapper(destination.findRootNode())
        destinationRoot.addChildren()
        destinationRoot.init()

        topDown(sourceRoot, destinationRoot, mappings)
        bottomUp(sourceRoot, destinationRoot, mappings)

        return mappings
    }

    /**
     * Implements the top-down-phase of the algorithm.
     * The goal is to map the biggest common ancestors of the two trees.
     */
    private fun topDown(sourceRoot: NodeWrapper, destinationRoot: NodeWrapper, mappings: MutableList<Mapping>) {

        // initialize lists
        val priorityListSource = LinkedList<NodeWrapper>()
        val priorityListDestination = LinkedList<NodeWrapper>()
        val candidateMappings = mutableListOf<Mapping>()

        //start algorithm
        push(priorityListSource, sourceRoot)
        push(priorityListDestination, destinationRoot)

        //calculate mappings
        while (min(peekMax(priorityListSource), peekMax(priorityListDestination)) > minHeight) {
            val peekMaxSource = peekMax(priorityListSource)
            val peekMaxDestination = peekMax(priorityListDestination)

            if (peekMaxSource != peekMaxDestination) {
                if (peekMaxSource > peekMaxDestination) {
                    popAndOpenEach(priorityListSource)
                } else {
                    popAndOpenEach(priorityListDestination)
                }
            } else {
                calculateMappings(priorityListSource, priorityListDestination, mappings, candidateMappings)
            }
        }

        //sort and process candidate mappings
        candidateMappings.sortByDescending { dice(it.sourceNode.parent!!, it.destinationNode.parent!!) }
        while (candidateMappings.isNotEmpty()) {
            val mapping = candidateMappings.removeFirst()
            mapIsomorphicChildren(mappings, mapping.sourceNode, mapping.destinationNode)
            candidateMappings.removeIf {
                (it.sourceNode.hashCode() == mapping.sourceNode.hashCode()
                        && it.sourceNode.lineNumber == mapping.sourceNode.lineNumber)
                        || (it.destinationNode.hashCode() == mapping.destinationNode.hashCode()
                        && it.destinationNode.lineNumber == mapping.destinationNode.lineNumber)
            }
        }
    }


    /**
     * Calculates the mappings of the top-down-phase.
     */
    private fun calculateMappings(
        priorityListSource: LinkedList<NodeWrapper>,
        priorityListDestination: LinkedList<NodeWrapper>,
        mappings: MutableList<Mapping>,
        candidateMappings: MutableList<Mapping>
    ) {
        val sourceSet = pop(priorityListSource)
        val destinationSet = pop(priorityListDestination)
        val allMappings = mutableMapOf<NodeWrapper, MutableList<NodeWrapper>>()

        //add all possible mappings to allMappings
        for (sourceNode in sourceSet) {
            for (destinationNode in destinationSet) {
                if (sourceNode.node == destinationNode.node) {
                    sourceNode.incrementMapping()
                    destinationNode.incrementMapping()
                    if (allMappings.containsKey(sourceNode)) {
                        allMappings[sourceNode]?.add(destinationNode)
                    } else {
                        allMappings[sourceNode] = mutableListOf(destinationNode)
                    }
                }
            }
        }

        // classify into mappings and candidate-mappings
        for (mapping in allMappings) {
            if (mapping.value.size == 1 && mapping.value[0].timesMapped == 1) {
                mapIsomorphicChildren(mappings, mapping.key, mapping.value[0])
            } else {
                for (destinationNode in mapping.value) {
                    candidateMappings.add(Mapping(mapping.key, destinationNode))
                }
            }
        }

        // open all unmatched subtrees
        sourceSet.filter { it.timesMapped == 0 }.forEach { open(priorityListSource, it) }
        priorityListSource.sortByDescending { it.height }
        destinationSet.filter { it.timesMapped == 0 }.forEach { open(priorityListDestination, it) }
        priorityListDestination.sortByDescending { it.height }
    }

    /**
     * Maps all isomorphic children of the given nodes.
     */
    private fun mapIsomorphicChildren(
        mappings: MutableList<Mapping>, sourceParentNode: NodeWrapper, destinationParentNode: NodeWrapper
    ) {
        sourceParentNode.gotMatchedChildren()
        val sourceStream = sourceParentNode.getDescendantsPreOrderWithoutRoot().iterator()
        val destinationStream = destinationParentNode.getDescendantsPreOrderWithoutRoot().iterator()
        while (sourceStream.hasNext() && destinationStream.hasNext()) {
            val sourceNode = sourceStream.next()
            val destinationNode = destinationStream.next()
            if (sourceNode == destinationNode) {
                sourceNode.mapped = true
                destinationNode.mapped = true
                mappings.add(Mapping(sourceNode, destinationNode))
            }
        }
    }

    /**
     * Implements the bottom-up-phase of the algorithm.
     */
    private fun bottomUp(
        sourceRoot: NodeWrapper, destinationRoot: NodeWrapper, mappings: MutableList<Mapping>
    ) {

        val sourceStream = sourceRoot.getDescendantsPostOrder().filter { !it.mapped }
        for (sourceNode in sourceStream) {
            if (!sourceNode.hasMatchedChildren) {
                continue
            }
            val candidate = candidate(sourceNode, destinationRoot)
            if (candidate != null && dice(sourceNode, candidate) > minDice) {
                sourceNode.parent?.gotMatchedChildren()
                mappings.add(Mapping(sourceNode, candidate))
                sourceNode.mapped = true
                candidate.mapped = true

                //find recovery mappings in container
                val sourceClone = sourceNode.getTEDWrapperWithoutMatchedChildren()
                val destinationClone = candidate.getTEDWrapperWithoutMatchedChildren()
                val sourceCount = sourceClone.getDescendantsPostOrder().count()
                val destinationCount = destinationClone.getDescendantsPostOrder().count()
                if (max(sourceCount, destinationCount) < maxSize) {
                    val tedMatcher =
                        TEDMatcher(1.0, 1.0, 1.0,
                            sourceClone, destinationClone, sourceCount, destinationCount)
                    tedMatcher.findMappings(mappings)
                }
            }
        }
    }

    /**
     * Returns the node with the most common descendants.
     */
    private fun candidate(sourceNode: NodeWrapper, destinationRoot: NodeWrapper): NodeWrapper? {
        val candidates = destinationRoot.getDescendantsPostOrder().filter {
            !it.mapped && sourceNode.label == it.label && haveMatchingDescendants(sourceNode, it)
        }.sortedByDescending { dice(sourceNode, it) }
        return candidates.firstOrNull()
    }

    /**
     * Returns true if the given nodes have matching descendants.
     */
    private fun haveMatchingDescendants(sourceNode: NodeWrapper, destinationNode: NodeWrapper): Boolean {
        return dice(sourceNode, destinationNode) > 0
    }

    /**
     * Adds the given node to the given list.
     */
    private fun push(list: MutableList<NodeWrapper>, node: NodeWrapper) {
        list.add(node)
    }

    /**
     * Adds all children of the given node to the given list.
     */
    private fun open(list: MutableList<NodeWrapper>, node: NodeWrapper) {
        list.addAll(node.children)
    }

    /**
     * Removes the nodes with the highest height from the given list and returns them.
     */
    private fun pop(list: LinkedList<NodeWrapper>): Iterable<NodeWrapper> {
        val maxHeight = peekMax(list)
        val collection = list.filter { node -> node.height == maxHeight }
        list.removeIf { node -> node.height == maxHeight }
        return collection
    }

    /**
     * Pops all nodes with the highest height from the given list and opens them.
     */
    private fun popAndOpenEach(priorityList: LinkedList<NodeWrapper>) {
        pop(priorityList).forEach { node -> open(priorityList, node) }
        priorityList.sortByDescending { it.height }
    }

    /**
     * Returns the height of the highest node in the given list or -1 if the list was empty.
     */
    private fun peekMax(sortedList: LinkedList<NodeWrapper>): Int {
        var height = -1
        if (sortedList.isNotEmpty()) {
            height = sortedList[0].height
        }
        return height
    }

    /**
     * Returns the dice coefficient of the given nodes.
     * The dice coefficient is a measure of similarity between two trees.
     */
    private fun dice(source: NodeWrapper, destination: NodeWrapper): Double {
        val destinationNodes: Map<NodeWrapper, NodeWrapper> =
            destination.getDescendantsPreOrderWithoutRoot().associateBy { it }
        val numberOfCommonDescendents =
            source.getDescendantsPreOrderWithoutRoot().filter { destinationNodes.containsKey(it) }.count().toDouble()
        val numberOfSourceChildren = source.getDescendantsPreOrderWithoutRoot().count().toDouble()
        val numberOfDestinationChildren = destination.getDescendantsPreOrderWithoutRoot().count().toDouble()
        return ((2.0 * numberOfCommonDescendents) / (numberOfSourceChildren + numberOfDestinationChildren))
    }
}