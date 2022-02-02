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

package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Result
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.GamePublisherDescriptor
import org.gamekins.GameUserProperty
import org.gamekins.challenge.Challenge.ChallengeGenerationData
import org.gamekins.event.EventHandler
import org.gamekins.event.user.ChallengeGeneratedEvent
import org.gamekins.file.SourceFileDetails
import org.gamekins.mutation.MutationInfo
import org.gamekins.mutation.MutationResults
import org.gamekins.mutation.MutationUtils
import org.gamekins.mutation.MutationUtils.findMutationHasCodeSnippets
import org.gamekins.mutation.MutationUtils.getCurrentLinesOperatorMapping
import org.gamekins.mutation.MutationUtils.getSurvivedMutationList
import org.gamekins.util.*
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.GitUtil.HeadCommitCallable
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
    private fun chooseChallengeType(mocoJSONPath: String?): Class<out Challenge> {
        val weightList = arrayListOf<Class<out Challenge>>()
        var challengeTypes = GamePublisherDescriptor.challenges
        if (mocoJSONPath.isNullOrEmpty()) {
            challengeTypes = challengeTypes.filter { it.key != MutationTestChallenge::class.java }
                    as HashMap<Class<out Challenge>, Int>
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
        result: Result?, user: User, property: GameUserProperty,
        parameters: Parameters, listener: TaskListener = TaskListener.NULL
    )
            : Boolean {
        try {
            if (result != null && result != Result.SUCCESS) {
                val challenge = BuildChallenge(parameters)
                val mapUser: User? = GitUtil.mapUser(
                    parameters.workspace.act(HeadCommitCallable(parameters.remote))
                        .authorIdent, User.getAll()
                )

                if (mapUser == user
                    && !property.getCurrentChallenges(parameters.projectName).contains(challenge)
                ) {
                    property.newChallenge(parameters.projectName, challenge)
                    EventHandler.addEvent(ChallengeGeneratedEvent(parameters.projectName, parameters.branch,
                        property.getUser(), challenge))
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
     * the list of [classes] is empty, a new [DummyChallenge] is generated. The workspace is the folder with the
     * code and execution rights, and the [listener] reports the events to the console output of Jenkins.
     */
    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun generateChallenge(
        user: User, parameters: Parameters, listener: TaskListener, classes: ArrayList<SourceFileDetails>,
        cla: SourceFileDetails? = null
    ): Challenge {

        val workList = ArrayList(classes)

        var challenge: Challenge?
        var count = 0
        do {
            if (count == 5 || workList.isEmpty()) {
                listener.logger.println("[Gamekins] No CoverageChallenge could be built")
                return DummyChallenge(parameters, Constants.ERROR_GENERATION)
            }
            count++

            val challengeClass = chooseChallengeType(parameters.mocoJSONPath)
            val selectedClass = cla
                ?: if (challengeClass.superclass == CoverageChallenge::class.java) {
                    val tempList = ArrayList(workList)
                    tempList.removeIf { details: SourceFileDetails -> details.coverage == 1.0 }
                    tempList.removeIf { details: SourceFileDetails -> !details.filesExists() }
                    if (tempList.isEmpty()) {
                        challenge = null
                        continue
                    }
                    selectClass(tempList, initializeRankSelection(tempList))
                } else {
                    selectClass(workList, initializeRankSelection(workList))
                }

            workList.remove(selectedClass)

            val rejectedChallenges = user.getProperty(GameUserProperty::class.java)
                .getRejectedChallenges(parameters.projectName)

            //Remove classes where a ClassCoverageChallenge has been rejected previously
            if (!rejectedChallenges.filter {
                    it.first is ClassCoverageChallenge
                            && (it.first as ClassCoverageChallenge).details.fileName == selectedClass.fileName
                            && (it.first as ClassCoverageChallenge)
                        .details.packageName == selectedClass.packageName
                }
                    .isNullOrEmpty()) {
                listener.logger.println(
                    "[Gamekins] Class ${selectedClass.fileName} in package " +
                            "${selectedClass.packageName} was rejected previously"
                )
                challenge = null
                continue
            }

            val data = ChallengeGenerationData(parameters, user, selectedClass, listener)

            when {
                challengeClass == TestChallenge::class.java -> {
                    data.testCount = JUnitUtil.getTestCount(parameters.workspace)
                    data.headCommitHash = parameters.workspace.act(HeadCommitCallable(parameters.remote)).name
                    listener.logger.println("[Gamekins] Generated new TestChallenge")
                    challenge = challengeClass
                        .getConstructor(ChallengeGenerationData::class.java)
                        .newInstance(data)
                }
                challengeClass.superclass == CoverageChallenge::class.java -> {
                    listener.logger.println(
                        "[Gamekins] Try class " + selectedClass.fileName + " and type "
                                + challengeClass
                    )
                    challenge = generateCoverageChallenge(data, challengeClass)
                }
                challengeClass == MutationTestChallenge::class.java -> {
                    listener.logger.println(
                        "[Gamekins] Try class " + selectedClass.fileName + " and type "
                                + challengeClass
                    )
                    challenge = generateMutationTestChallenge(selectedClass, parameters.branch, parameters.projectName,
                        listener, parameters.workspace, user)
                }
                challengeClass == SmellChallenge::class.java -> {
                    listener.logger.println(
                        "[Gamekins] Try class " + selectedClass.fileName + " and type "
                                + challengeClass
                    )
                    //TODO: Include test files
                    challenge = generateSmellChallenge(data, listener)
                }
                else -> {
                    challenge = generateThirdPartyChallenge(data, challengeClass)
                }
            }

            if (rejectedChallenges.any { it.first == challenge }) {
                listener.logger.println("[Gamekins] Challenge ${challenge?.toEscapedString()} was already " +
                        "rejected previously")
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
                    data.parameters.workspace,
                    data.selectedClass.jacocoSourceFile, data.selectedClass.parameters.remote
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
                    data.method = JacocoUtil.chooseRandomMethod(data.selectedClass, data.parameters.workspace)
                    if (data.method == null) null else challengeClass
                        .getConstructor(ChallengeGenerationData::class.java)
                        .newInstance(data)
                }
                else -> {
                    data.line = JacocoUtil.chooseRandomLine(data.selectedClass, data.parameters.workspace)
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
        user: User, property: GameUserProperty, parameters: Parameters, classes: ArrayList<SourceFileDetails>,
        listener: TaskListener = TaskListener.NULL, maxChallenges: Int = Constants.DEFAULT_CURRENT_CHALLENGES
    ): Int {

        var generated = 0
        if (property.getCurrentChallenges(parameters.projectName).size < maxChallenges) {
            listener.logger.println("[Gamekins] Start generating challenges for user ${user.fullName}")

            val userClasses = ArrayList(classes)
            userClasses.removeIf { details: SourceFileDetails ->
                !details.changedByUsers.contains(GitUtil.GameUser(user))
            }

            listener.logger.println("[Gamekins] Found ${userClasses.size} last changed files of user ${user.fullName}")

            for (i in property.getCurrentChallenges(parameters.projectName).size until maxChallenges) {
                if (userClasses.size == 0) {
                    property.newChallenge(parameters.projectName,
                        DummyChallenge(parameters, Constants.NOTHING_DEVELOPED))
                    EventHandler.addEvent(ChallengeGeneratedEvent(parameters.projectName, parameters.branch,
                        property.getUser(), DummyChallenge(parameters, Constants.ERROR_GENERATION)))
                    break
                }

                generated += generateUniqueChallenge(user, property, parameters, userClasses, listener)
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

        data.testCount = JUnitUtil.getTestCount(data.parameters.workspace)
        data.headCommitHash = data.parameters.workspace.act(HeadCommitCallable(data.parameters.remote)).name
        data.method = JacocoUtil.chooseRandomMethod(data.selectedClass, data.parameters.workspace)
        data.line = JacocoUtil.chooseRandomLine(data.selectedClass, data.parameters.workspace)

        return challengeClass.getConstructor(ChallengeGenerationData::class.java).newInstance(data)
    }

    /**
     * Tries to generate a new unique [Challenge].
     */
    private fun generateUniqueChallenge(
        user: User, property: GameUserProperty, parameters: Parameters, userClasses: ArrayList<SourceFileDetails>,
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
                    challenge = DummyChallenge(parameters, Constants.ERROR_GENERATION)
                    break
                }
                isChallengeUnique = true

                listener.logger.println("[Gamekins] Started to generate challenge")
                challenge = generateChallenge(user, parameters, listener, userClasses)

                listener.logger.println("[Gamekins] Generated challenge ${challenge.toEscapedString()}")
                if (challenge is DummyChallenge) break

                for (currentChallenge in property.getCurrentChallenges(parameters.projectName)) {
                    if (currentChallenge.toString() == challenge.toString()) {
                        isChallengeUnique = false
                        listener.logger.println("[Gamekins] Challenge is not unique")
                        break
                    }
                }
                count++
            } while (!isChallengeUnique)

            property.newChallenge(parameters.projectName, challenge)
            listener.logger.println("[Gamekins] Added challenge ${challenge.toEscapedString()}")
            EventHandler.addEvent(ChallengeGeneratedEvent(parameters.projectName, parameters.branch,
                property.getUser(), challenge))
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
    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun generateMutationTestChallenge(
        classDetails: SourceFileDetails, branch: String?, projectName: String?,
        listener: TaskListener, workspace: FilePath, user: User
    ): MutationTestChallenge? {
        MutationUtils.mutationBlackList.clear()
        val jsonFilePath = JacocoUtil.calculateCurrentFilePath(
            workspace, classDetails.mocoJSONFile!!, classDetails.parameters.remote
        )
        val commitID = workspace.act(HeadCommitCallable(workspace.remote)).name
        val fullClassName = "${classDetails.packageName}.${classDetails.fileName}"
        val relevantMutationResultsByClass: Map<String, Set<MutationInfo>>? =
            MutationResults.retrievedMutationsFromJson(jsonFilePath, listener)?.entries?.filter {
                it.key == fullClassName
                        && it.value.any { it1 -> it1.result == "survived" }
            }
        if (relevantMutationResultsByClass.isNullOrEmpty() ||
            relevantMutationResultsByClass[fullClassName].isNullOrEmpty()) {
            listener.logger.println("[Gamekins] Mutation test - no mutation information for class $fullClassName")
            return null
        }

        val currentChallenges = user.getProperty(GameUserProperty::class.java).getCurrentChallenges(projectName!!).
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
            chosenMutation, classDetails, branch, commitID, codeSnippet, mutatedLine
        )
    }

    /**
     * Generates a new [SmellChallenge] according the current [data]. Gets all smells of a file and chooses one of
     * them randomly for generation.
     */
    fun generateSmellChallenge(data: ChallengeGenerationData, listener: TaskListener): SmellChallenge? {
        val issues = SmellUtil.getSmellsOfFile(data.selectedClass, listener)

        if (issues.isEmpty()) return null

        return SmellChallenge(data.selectedClass, issues[Random.nextInt(issues.size)])
    }

    /**
     * Initializes the array with the values for the Rank Selection algorithm.
     */
    private fun initializeRankSelection(workList: List<SourceFileDetails>): DoubleArray {
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
    private fun selectClass(workList: List<SourceFileDetails>, rankValues: DoubleArray): SourceFileDetails {
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
