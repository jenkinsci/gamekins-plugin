/*
 * Copyright 2021 Gamekins contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gamekins.mutation

import hudson.FilePath
import hudson.model.User
import org.gamekins.GameUserProperty
import org.gamekins.challenge.MutationTestChallenge
import org.gamekins.util.JacocoUtil


/**
 * Contains all utility functions that ChallengeFactory needs to create a MutationTestChallenge
 *
 * @author Tran Phan
 * @since 0.3
 */

object MutationUtils {
    /**
     * Return list of mutation info contains mutation that:
     *
     * - Survived
     * - Unique ID not in current mutation challenges list (avoid generating duplicated)
     * - Unique ID not in rejected mutation challenge list
     * - Not on the same line of code with a rejected challenge that has same commit ID
     *
     */
    fun getSurvivedMutationList(
        initialList: Set<MutationInfo>?, commitID: String,
        currentChallenges: List<MutationTestChallenge>,
        user: User, projectName: String?
    ): List<MutationInfo?>? {
        val rejectedMutations =
            user.getProperty(GameUserProperty::class.java).getRejectedChallenges(projectName).filter {
                it.first is MutationTestChallenge
            }
        return initialList?.filter {
            it.result == "survived" &&
                    !(rejectedMutations.any { it1 ->
                        shouldIgnoreMutation(it1.first as MutationTestChallenge, it, commitID)
                    }) &&
                    !(currentChallenges.any { it2 -> it2.uniqueID == it.uniqueID })
        }
    }

    /**
     * Put entries into currentLinesOperatorMap
     * an entry in currentLinesOperatorMap is mapping from line of code to mutation operators of current challenges
     */
    fun getCurrentLinesOperatorMapping(
        currentChallenges: List<MutationTestChallenge>, fullClassName: String,
        currentLinesOperatorMap: MutableMap<Int, MutableSet<String>>
    ) {
        currentChallenges.map {
            if ("${it.classDetails.packageName}.${it.classDetails.className}" == fullClassName) {
                if (!currentLinesOperatorMap.containsKey(it.lineOfCode)) {
                    currentLinesOperatorMap[it.lineOfCode] =
                        mutableSetOf(it.mutationInfo.mutationDetails.mutationOperatorName)
                } else {
                    currentLinesOperatorMap[it.lineOfCode]?.add(it.mutationInfo.mutationDetails.mutationOperatorName)
                }
            }
        }
    }

    /**
     * Ignore a mutation that's in rejected challenges list OR on the same line with a previous rejected mutation
     * test challenge and both have contains the same commit ID field
     */
    fun shouldIgnoreMutation(
        rejectedChallenge: MutationTestChallenge,
        target: MutationInfo,
        commitID: String
    ): Boolean {
        // Mutation on the same line with same commit ID shouldn't be used for generating new mutation challenge again
        // Even if they have different mutation operators
        val mutationDetails = target.mutationDetails
        return (rejectedChallenge.uniqueID == target.uniqueID) ||
                (rejectedChallenge.lineOfCode == mutationDetails.loc &&
                        rejectedChallenge.commitID == commitID)
    }

    /**
     * We prioritize mutation in new methods
     * If no mutation with completed code snippet info can be found, then we proceed with mutation in
     * existing methods (methods already in current challenges)
     */
    fun findMutationHasCodeSnippets(
        survivedList: List<MutationInfo?>,
        classDetails: JacocoUtil.ClassDetails, workspace: FilePath,
        currentLinesOperatorMap: Map<Int, Set<String>>, currentChallengeMethods: Set<String>
    ): Pair<MutationInfo?, Map<String, String>> {
        val mutationOnNewMethods = survivedList.filter {
            !currentChallengeMethods.contains(it?.mutationDetails?.methodInfo?.get("methodName"))
        }
        // Prefer mutations in methods that not in current challenges
        var res = handleMutationInNewMethods(mutationOnNewMethods, classDetails, workspace, currentLinesOperatorMap)
        if (res.first != null) return res
        // Then comes the rest
        val mutationOnOldMethods = survivedList.minus(mutationOnNewMethods)
        res = handleMutationInOldMethods(mutationOnOldMethods, classDetails, workspace, currentLinesOperatorMap)
        return res
    }


    /**
     * We prioritize mutation with operators which are different from those in current challenges
     */
    fun handleMutationInNewMethods(
        muList: List<MutationInfo?>,
        classDetails: JacocoUtil.ClassDetails, workspace: FilePath,
        currentLinesOperatorMap: Map<Int, Set<String>>,
    ): Pair<MutationInfo?, Map<String, String>> {
        // Prioritize mutations with operators that not in current challenges
        val mutationOnDiffOperator = muList.filter {
            val loc = it?.mutationDetails?.loc
            currentLinesOperatorMap[loc]?.contains(it?.mutationDetails?.mutationOperatorName) != true
        }
        val res = processMutationSnippet(mutationOnDiffOperator, classDetails, workspace)
        if (res.first != null) return res
        val mutationOnSameOperator = muList.minus(mutationOnDiffOperator)
        return processMutationSnippet(mutationOnSameOperator, classDetails, workspace)
    }


    /**
     * We prioritize mutation with operators which are different from those in current challenges
     * and mutation on different lines of code
     */
    fun handleMutationInOldMethods(
        muList: List<MutationInfo?>,
        classDetails: JacocoUtil.ClassDetails, workspace: FilePath,
        currentLinesOperatorMap: Map<Int, Set<String>>
    ): Pair<MutationInfo?, Map<String, String>> {
        val mutationOnNewLines = muList.filter { !currentLinesOperatorMap.keys.contains(it?.mutationDetails?.loc) }
        val onNewLinesDiffOperator = mutationOnNewLines.filter {
            val loc = it?.mutationDetails?.loc
            currentLinesOperatorMap[loc]?.contains(it?.mutationDetails?.mutationOperatorName) != true
        }
        // Highest priority is mutation on a new line diff operator (not in current challenges)
        var res = processMutationSnippet(onNewLinesDiffOperator, classDetails, workspace)
        if (res.first != null) return res

        val onNewLinesOldOperator = mutationOnNewLines.minus(onNewLinesDiffOperator)
        processMutationSnippet(onNewLinesOldOperator, classDetails, workspace)
        if (res.first != null) return res

        // Lower priority is mutation on a same line
        val mutationOnOldLines = muList.minus(mutationOnNewLines)
        val onOldLinesDiffOperator = mutationOnOldLines.filter {
            val loc = it?.mutationDetails?.loc
            currentLinesOperatorMap[loc]?.contains(it?.mutationDetails?.mutationOperatorName) != true
        }
        res = processMutationSnippet(onOldLinesDiffOperator, classDetails, workspace)
        if (res.first != null) return res

        val theRest = mutationOnOldLines.minus(onOldLinesDiffOperator)
        res = processMutationSnippet(theRest, classDetails, workspace)
        return res
    }

    /**
     * Since it is not always possible to convert mutation info to mutated code snippet
     * We prioritize mutation with completed code snippet info
     * A mutation is chosen only if both of its [codeSnippet] and [mutatedLine] can be created.
     */
    fun processMutationSnippet(
        muList: List<MutationInfo?>,
        classDetails: JacocoUtil.ClassDetails, workspace: FilePath
    ): Pair<MutationInfo?, Map<String, String>> {
        // Prioritize mutation with available code snippet and mutated code snippet
        var chosenMutation: MutationInfo? = null
        var codeSnippet = ""
        var mutatedLine = ""

        //  val temp = muList.groupBy { it?.mutationDetails?.mutationOperatorName }
        //  val sorted = temp.toList().sortedBy { (_, value) -> value.size }.toMap().values.flatten()

        // Change strategy, shuffle instead of prioritizing minority mutation types
        val temp = muList.shuffled()
        for (mutation in temp) {
            val relatedSource = MutationTestChallenge.createCodeSnippet(
                classDetails, mutation?.mutationDetails!!.loc, workspace
            )
            codeSnippet = relatedSource.first
            if (codeSnippet.isNotEmpty()) {
                mutatedLine =
                    MutationPresentation.createMutatedLine(relatedSource.second, mutation, mutation.mutationDetails.loc)
            }
            if (mutatedLine.isNotEmpty()) {
                chosenMutation = mutation
                // Return as soon as a mutation with completed mutated source code can be created
                break
            }
        }
        if (chosenMutation == null) {
            codeSnippet = ""
            mutatedLine = ""
        }
        return Pair(chosenMutation, mapOf("codeSnippet" to codeSnippet, "mutatedSnippet" to mutatedLine))
    }

    // Add ignored mutation to this list during mutation test challenge creation
    val mutationBlackList: MutableSet<MutationInfo> = mutableSetOf()
}