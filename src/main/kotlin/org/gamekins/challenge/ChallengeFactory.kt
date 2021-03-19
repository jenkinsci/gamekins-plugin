/*
 * Copyright 2020 Gamekins contributors
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

package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Result
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.GamePublisherDescriptor
import org.gamekins.GameUserProperty
import org.gamekins.challenge.Challenge.ChallengeGenerationData
import org.gamekins.mutation.MutationInfo
import org.gamekins.mutation.MutationResults
import org.gamekins.mutation.MutationUtils
import org.gamekins.mutation.MutationUtils.findMutationHasCodeSnippets
import org.gamekins.mutation.MutationUtils.getCurrentLinesOperatorMapping
import org.gamekins.mutation.MutationUtils.getSurvivedMutationList
import org.gamekins.util.GitUtil
import org.gamekins.util.GitUtil.HeadCommitCallable
import org.gamekins.util.JUnitUtil
import org.gamekins.util.JacocoUtil
import org.gamekins.util.JacocoUtil.ClassDetails
import org.jsoup.nodes.Document
import java.io.IOException
import kotlin.collections.ArrayList
import kotlin.jvm.Throws
import kotlin.random.Random

/**
 * Factory for generating [Challenge]s.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
object ChallengeFactory {

    /**
     * Chooses the type of [Challenge] to be generated.
     */
    private fun chooseChallengeType(): Class<out Challenge> {
        val weightList = arrayListOf<Class<out Challenge>>()
        var challengeTypes = GamePublisherDescriptor.challenges
        if (!MutationResults.mocoJSONAvailable) {
            challengeTypes = challengeTypes.filter { it.key != MutationTestChallenge::class.java } as HashMap<Class<out Challenge>, Int>
        }
        challengeTypes.forEach { (clazz, weight) ->
            (0 until weight).forEach { _ ->
                weightList.add(clazz)
            }
        }
        return weightList[Random.nextInt(weightList.size)]
    }

    /**
     * Generates a new [BuildChallenge] if the [result] was not [Result.SUCCESS] and returns true.
     */
    @JvmStatic
    fun generateBuildChallenge(
        result: Result?, user: User, workspace: FilePath, property: GameUserProperty,
        constants: HashMap<String, String>, listener: TaskListener = TaskListener.NULL
    )
            : Boolean {
        try {
            if (result != null && result != Result.SUCCESS) {
                val challenge = BuildChallenge(constants)
                val mapUser: User? = GitUtil.mapUser(
                    workspace.act(HeadCommitCallable(workspace.remote))
                        .authorIdent, User.getAll()
                )

                if (mapUser == user
                    && !property.getCurrentChallenges(constants["projectName"]).contains(challenge)
                ) {
                    property.newChallenge(constants["projectName"]!!, challenge)
                    listener.logger.println("[Gamekins] Generated new BuildChallenge")
                    user.save()
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
        }

        return false
    }

    /**
     * Generates a new [Challenge] for the current [user].
     *
     * With a probability of 10% a new [TestChallenge] is generated to keep the user motivated. Otherwise a class
     * is selected by the Rank Selection algorithm from the pool of [classes], where the [user] has changed something
     * in his last commits. It is being attempted five times to generate a [CoverageChallenge]. If this fails or if
     * the list of [classes] is empty, a new [DummyChallenge] is generated. The [workspace] is the folder with the
     * code and execution rights, and the [listener] reports the events to the console output of Jenkins.
     */
    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun generateChallenge(
        user: User, constants: HashMap<String, String>, listener: TaskListener,
        classes: ArrayList<ClassDetails>, workspace: FilePath
    ): Challenge {

        val workList = ArrayList(classes)
        val rankValues = initializeRankSelection(workList)

        var challenge: Challenge?
        var count = 0
        do {
            if (count == 5 || workList.isEmpty()) {
                listener.logger.println("[Gamekins] No CoverageChallenge could be built")
                return DummyChallenge(constants)
            }

            val selectedClass = selectClass(workList, rankValues)
            workList.remove(selectedClass)
            count++

            val rejectedChallenges = user.getProperty(GameUserProperty::class.java)
                .getRejectedChallenges(constants["projectName"])

            //Remove classes where a ClassCoverageChallenge has been rejected previously
            if (!rejectedChallenges.filter {
                    it.first is ClassCoverageChallenge
                            && (it.first as ClassCoverageChallenge).classDetails.className == selectedClass.className
                            && (it.first as ClassCoverageChallenge)
                        .classDetails.packageName == selectedClass.packageName
                }
                    .isNullOrEmpty()) {
                listener.logger.println(
                    "[Gamekins] Class ${selectedClass.className} in package " +
                            "${selectedClass.packageName} was rejected previously"
                )
                challenge = null
                continue
            }

            val challengeClass = chooseChallengeType()
            val data = ChallengeGenerationData(constants, user, selectedClass, workspace, listener)

            when {
                challengeClass == TestChallenge::class.java -> {
                    data.testCount = JUnitUtil.getTestCount(workspace)
                    data.headCommitHash = workspace.act(HeadCommitCallable(workspace.remote)).name
                    listener.logger.println("[Gamekins] Generated new TestChallenge")
                    challenge = challengeClass
                        .getConstructor(ChallengeGenerationData::class.java)
                        .newInstance(data)
                }
                challengeClass.superclass == CoverageChallenge::class.java -> {
                    listener.logger.println(
                        "[Gamekins] Try class " + selectedClass.className + " and type "
                                + challengeClass
                    )
                    challenge = generateCoverageChallenge(data, challengeClass)
                }
                challengeClass == MutationTestChallenge::class.java -> {
                    listener.logger.println(
                        "[Gamekins] Try class " + selectedClass.className + " and type "
                                + challengeClass
                    )
                    challenge = generateMutationTestChallenge(
                        selectedClass, constants["branch"],
                        constants["projectName"], listener, workspace, user
                    )
                }
                else -> {
                    challenge = generateThirdPartyChallenge(data, challengeClass)
                }
            }

            if (rejectedChallenges.any { it.first == challenge }) {
                listener.logger.println("[Gamekins] Challenge ${challenge?.toEscapedString()} was already rejected previously")
                challenge = null
            }

            if (challenge != null && !challenge.builtCorrectly) {
                listener.logger.println("[Gamekins] Challenge ${challenge.toEscapedString()} was not built correctly")
                challenge = null
            }
        } while (challenge == null)

        return challenge
    }

    /**
     * Generates a new [CoverageChallenge] of type [challengeClass] for the current class with details classDetails
     * and the current branch. The workspace is the folder with the code and execution rights, and the listener
     * reports the events to the console output of Jenkins.
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun generateCoverageChallenge(data: ChallengeGenerationData, challengeClass: Class<out Challenge>)
            : Challenge? {

        val document: Document = try {
            JacocoUtil.generateDocument(
                JacocoUtil.calculateCurrentFilePath(
                    data.workspace,
                    data.selectedClass.jacocoSourceFile, data.selectedClass.workspace
                )
            )
        } catch (e: Exception) {
            data.listener.logger.println(
                "[Gamekins] Exception with JaCoCoSourceFile "
                        + data.selectedClass.jacocoSourceFile.absolutePath
            )
            e.printStackTrace(data.listener.logger)
            throw e
        }

        return if (JacocoUtil.calculateCoveredLines(document, "pc") > 0
            || JacocoUtil.calculateCoveredLines(document, "nc") > 0
        ) {
            when (challengeClass) {
                ClassCoverageChallenge::class.java -> {
                    challengeClass
                        .getConstructor(ChallengeGenerationData::class.java)
                        .newInstance(data)
                }
                MethodCoverageChallenge::class.java -> {
                    data.method = JacocoUtil.chooseRandomMethod(data.selectedClass, data.workspace)
                    if (data.method == null) null else challengeClass
                        .getConstructor(ChallengeGenerationData::class.java)
                        .newInstance(data)
                }
                else -> {
                    data.line = JacocoUtil.chooseRandomLine(data.selectedClass, data.workspace)
                    if (data.line == null) null else challengeClass
                        .getConstructor(ChallengeGenerationData::class.java)
                        .newInstance(data)
                }
            }
        } else null
    }

    /**
     * Generates new Challenges for a [user] if he has less than [maxChallenges] Challenges after checking the solved
     * and solvable state of his Challenges. Returns the number of generated Challenges for debug output.
     */
    @JvmStatic
    fun generateNewChallenges(
        user: User, property: GameUserProperty, constants: HashMap<String, String>,
        classes: ArrayList<ClassDetails>, workspace: FilePath,
        listener: TaskListener = TaskListener.NULL, maxChallenges: Int = 3
    ): Int {

        var generated = 0
        if (property.getCurrentChallenges(constants["projectName"]).size < maxChallenges) {
            listener.logger.println("[Gamekins] Start generating challenges for user ${user.fullName}")

            val userClasses = ArrayList(classes)
            userClasses.removeIf { classDetails: ClassDetails ->
                !classDetails.changedByUsers.contains(GitUtil.GameUser(user))
            }

            listener.logger.println("[Gamekins] Found ${userClasses.size} last changed files of user ${user.fullName}")

            for (i in property.getCurrentChallenges(constants["projectName"]).size..2) {
                if (userClasses.size == 0) {
                    property.newChallenge(constants["projectName"]!!, DummyChallenge(constants))
                    break
                }

                generated += generateUniqueChallenge(user, property, constants, userClasses, workspace, listener)
            }
        }

        return generated
    }

    /**
     * Generates a new third party [Challenge]. The values listed below in the method may be null and have to be checked
     * in the initialisation of the [Challenge].
     */
    private fun generateThirdPartyChallenge(data: ChallengeGenerationData, challengeClass: Class<out Challenge>)
            : Challenge? {

        data.testCount = JUnitUtil.getTestCount(data.workspace)
        data.headCommitHash = data.workspace.act(HeadCommitCallable(data.workspace.remote)).name
        data.method = JacocoUtil.chooseRandomMethod(data.selectedClass, data.workspace)
        data.line = JacocoUtil.chooseRandomLine(data.selectedClass, data.workspace)

        return challengeClass.getConstructor(ChallengeGenerationData::class.java).newInstance(data)
    }

    /**
     * Tries to generate a new unique [Challenge].
     */
    private fun generateUniqueChallenge(
        user: User, property: GameUserProperty, constants: HashMap<String, String>,
        userClasses: ArrayList<ClassDetails>, workspace: FilePath,
        listener: TaskListener
    ): Int {
        var generated = 0
        try {
            //Try to generate a new unique Challenge three times. because it can fail
            var challenge: Challenge
            var isChallengeUnique: Boolean
            var count = 0
            do {
                if (count == 3) {
                    challenge = DummyChallenge(constants)
                    break
                }
                isChallengeUnique = true

                listener.logger.println("[Gamekins] Started to generate challenge")
                challenge = generateChallenge(user, constants, listener, userClasses, workspace)

                listener.logger.println("[Gamekins] Generated challenge ${challenge.toEscapedString()}")
                if (challenge is DummyChallenge) break

                for (currentChallenge in property.getCurrentChallenges(constants["projectName"])) {
                    if (currentChallenge.toString() == challenge.toString()) {
                        isChallengeUnique = false
                        listener.logger.println("[Gamekins] Challenge is not unique")
                        break
                    }
                }
                count++
            } while (!isChallengeUnique)

            property.newChallenge(constants["projectName"]!!, challenge)
            listener.logger.println("[Gamekins] Added challenge ${challenge.toEscapedString()}")
            generated++
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
        }

        return generated
    }

    /**
     * Generates a new [MutationTestChallenge] of type [Challenge] for the current class with details [classDetails]
     * and the current [branch]. The [workspace] is the folder with the code and execution rights, and the [listener]
     * reports the events to the console output of Jenkins.
     *
     * This function parses JSON file of mutation test results and filter to take only SURVIVED
     * mutations which belong to the given class
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun generateMutationTestChallenge(
        classDetails: ClassDetails, branch: String?, projectName: String?,
        listener: TaskListener, workspace: FilePath, user: User
    ): MutationTestChallenge? {
        MutationUtils.mutationBlackList.clear()
        val jsonFilePath = JacocoUtil.calculateCurrentFilePath(
            workspace, classDetails.mocoJSONFile!!, classDetails.workspace
        )
        val commitID = workspace.act(HeadCommitCallable(workspace.remote)).name
        val fullClassName = "${classDetails.packageName}.${classDetails.className}"
        val relevantMutationResultsByClass: Map<String, List<MutationInfo>>? =
            MutationResults.retrievedMutationsFromJson(jsonFilePath, listener)?.entries?.filter {
                it.key == fullClassName
                        && it.value.any { it1 -> it1.result == "survived" }
            }
        if (relevantMutationResultsByClass.isNullOrEmpty() ||
            relevantMutationResultsByClass[fullClassName].isNullOrEmpty()) {
            listener.logger.println("[Gamekins] Mutation test - no mutation information for class $fullClassName")
            return null
        }

        val currentChallenges = user.getProperty(GameUserProperty::class.java).getCurrentChallenges(projectName).
                                        filterIsInstance<MutationTestChallenge>()

        val survivedList = getSurvivedMutationList(relevantMutationResultsByClass[fullClassName],
                commitID, currentChallenges, user, projectName)

        if (survivedList.isNullOrEmpty()) {
            listener.logger.println("[Gamekins] Mutation test - no survived mutation for class $fullClassName")
            return null
        }

        // Mapping from line of code to operator names of current challenges
        val currentLinesOperatorMap: MutableMap<Int, MutableSet<String>> = mutableMapOf()
        getCurrentLinesOperatorMapping(currentChallenges, fullClassName, currentLinesOperatorMap)
        val currentChallengeMethods: MutableSet<String> = mutableSetOf()
        currentChallenges.map { it.methodName?.let { it1 -> currentChallengeMethods.add(it1) } }

        // Prioritize mutation with available code snippet and mutated code snippet
        val foundMutationInfoPair =
            findMutationHasCodeSnippets(survivedList, classDetails, workspace,
                                        currentLinesOperatorMap, currentChallengeMethods)
        var chosenMutation: MutationInfo? = foundMutationInfoPair.first
        var codeSnippet: String = foundMutationInfoPair.second["codeSnippet"]!!
        val mutatedLine: String = foundMutationInfoPair.second["mutatedSnippet"]!!

        if (chosenMutation == null) {
            // Choose a survived mutation randomly if no mutation with completed code snippet info can be found
            val temp = survivedList.minus(MutationUtils.mutationBlackList)
            if (temp.isNullOrEmpty()) return null
            val randomMutation = temp.random() ?: return null
            codeSnippet = MutationTestChallenge.createCodeSnippet(
                classDetails, randomMutation.mutationDetails.loc, workspace
            ).first
            chosenMutation = randomMutation
        }
        // Commit id is persisted with mutation info to later determine the solvability of a mutation challenge
        return MutationTestChallenge(
            chosenMutation, classDetails, branch, workspace, commitID, codeSnippet, mutatedLine
        )
    }

    /**
     * Initializes the array with the values for the Rank Selection algorithm.
     */
    private fun initializeRankSelection(workList: List<ClassDetails>): DoubleArray {
        val c = 1.5
        val rankValues = DoubleArray(workList.size)
        for (i in workList.indices) {
            rankValues[i] = (2 - c + 2 * (c - 1) * (i / (workList.size - 1).toDouble())) / workList.size.toDouble()
            if (i != 0) rankValues[i] += rankValues[i - 1]
        }
        return rankValues
    }

    /**
     * Select a class of the [workList] with the Rank Selection algorithm ([rankValues]).
     */
    private fun selectClass(workList: List<ClassDetails>, rankValues: DoubleArray): ClassDetails {
        val probability = Random.nextDouble()
        var selectedClass = workList[workList.size - 1]
        for (i in workList.indices) {
            if (rankValues[i] > probability) {
                selectedClass = workList[i]
                break
            }
        }

        return selectedClass
    }
}
