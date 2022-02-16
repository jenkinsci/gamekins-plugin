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

package org.gamekins.challenge.quest

import hudson.FilePath
import hudson.model.TaskListener
import hudson.model.User
import org.gamekins.GameUserProperty
import org.gamekins.challenge.*
import org.gamekins.event.EventHandler
import org.gamekins.event.user.QuestGeneratedEvent
import org.gamekins.file.FileDetails
import org.gamekins.file.SourceFileDetails
import org.gamekins.file.TestFileDetails
import org.gamekins.util.*
import org.jsoup.nodes.Element
import kotlin.random.Random

/**
 * Factory for generating [Quest]s.
 *
 * @author Philipp Straubinger
 * @since 0.4
 */
object QuestFactory {

    /**
     * Generates new [Quest]s for a particular [user].
     */
    @JvmStatic
    fun generateNewQuests(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        files: ArrayList<FileDetails>, maxQuests: Int = Constants.DEFAULT_CURRENT_QUESTS
    ): Int {

        var generated = 0
        if (property.getCurrentQuests(parameters.projectName).size < maxQuests) {
            listener.logger.println("[Gamekins] Start generating quests for user ${user.fullName}")

            val userClasses = ArrayList(files)
            userClasses.removeIf { details: FileDetails ->
                !details.changedByUsers.contains(GitUtil.GameUser(user))
            }

            listener.logger.println("[Gamekins] Found ${userClasses.size} last changed files of user ${user.fullName}")

            for (i in property.getCurrentQuests(parameters.projectName).size until maxQuests) {
                if (userClasses.size == 0) {
                    property.newQuest(parameters.projectName,
                        Quest(Constants.NO_QUEST, arrayListOf())
                    )
                    EventHandler.addEvent(
                        QuestGeneratedEvent(parameters.projectName, parameters.branch, user,
                            Quest(Constants.NO_QUEST, arrayListOf())
                        )
                    )
                    break
                }

                listener.logger.println("[Gamekins] Started to generate quest")
                val quest = generateQuest(user, property, parameters, listener, userClasses)
                listener.logger.println("[Gamekins] Generated quest $quest")
                property.newQuest(parameters.projectName, quest)
                listener.logger.println("[Gamekins] Added quest $quest")
                EventHandler.addEvent(QuestGeneratedEvent(parameters.projectName, parameters.branch, user, quest))
                if (quest.name == Constants.NO_QUEST) break
                generated++
            }
        }

        return generated
    }

    /**
     * Through all: Line, Method, Class, Test, Mutation
     *
     * Generates a random new [Quest].
     */
    @JvmStatic
    fun generateQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>
    ): Quest {

        for (i in 0..2) {
            val quest = when (Random.nextInt(9)) {
                0 -> generateLinesQuest(user, property, parameters, listener, classes)
                1 -> generateMethodsQuest(user, property, parameters, listener, classes)
                2 -> generatePackageQuest(user, property, parameters, listener, classes)
                3 -> generateClassQuest(user, property, parameters, listener, classes)
                4 -> generateExpandingQuest(user, property, parameters, listener, classes)
                5 -> generateDecreasingQuest(user, property, parameters, listener, classes)
                6 -> generateTestQuest(user, property, parameters, listener, classes)
                7 -> generateMutationQuest(user, property, parameters, listener, classes)
                8 -> generateSmellQuest(user, property, parameters, listener, classes)
                else -> null
            }

            if (property.getRejectedQuests(parameters.projectName).map { it.first }.contains(quest)) continue

            if (quest != null) return quest
        }

        return Quest(Constants.NO_QUEST, arrayListOf())
    }

    /**
     * Generates a new ClassQuest with three [ClassCoverageChallenge]s for the same class.
     */
    @JvmStatic
    fun generateClassQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>
    ): Quest? {
        val suitableClasses = ArrayList(classes.filterIsInstance<SourceFileDetails>()
            .filter { it.coverage < 1.0 && it.filesExists() })
        val rejectedClasses = property.getRejectedChallenges(parameters.projectName)
            .map { it.first }
            .filterIsInstance<ClassCoverageChallenge>()
            .map { it.details }
        suitableClasses.removeIf { rejectedClasses.contains(it) }
        if (suitableClasses.isEmpty()) return null
        val selectedClass = suitableClasses.random()

        val steps = arrayListOf<QuestStep>()
        val data = Challenge.ChallengeGenerationData(parameters, user, selectedClass, listener)
        for (i in 0..2) {
            steps.add(QuestStep("", ClassCoverageChallenge(data)))
        }

        return Quest("Incremental - Solve three Class Coverage Challenges", steps)
    }

    /**
     * Generates a new DecreasingQuest in the order [ClassCoverageChallenge], [MethodCoverageChallenge] and
     * [LineCoverageChallenge] in the same class.
     */
    @JvmStatic
    fun generateDecreasingQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>
    ): Quest? {
        val suitableClasses = classes.filterIsInstance<SourceFileDetails>()
            .filter { it.coverage < 1.0  && it.filesExists() }
        if (suitableClasses.isEmpty()) return null
        val selectedClass = suitableClasses.random()

        val steps = arrayListOf<QuestStep>()
        val line = JacocoUtil.chooseRandomLine(selectedClass, parameters.workspace)
        val method = JacocoUtil.chooseRandomMethod(selectedClass, parameters.workspace)
        val data = Challenge.ChallengeGenerationData(parameters, user, selectedClass, listener, method, line)

        steps.add(QuestStep("", ClassCoverageChallenge(data)))
        steps.add(QuestStep("", MethodCoverageChallenge(data)))
        steps.add(QuestStep("", LineCoverageChallenge(data)))

        return Quest("Decreasing - Solve a Class, Method and Line Coverage Challenge", steps)
    }

    /**
     * Generates a new ExpandingQuest in the order [LineCoverageChallenge], [MethodCoverageChallenge] and
     * [ClassCoverageChallenge] in the same class.
     */
    @JvmStatic
    fun generateExpandingQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>
    ): Quest? {
        val suitableClasses = classes.filterIsInstance<SourceFileDetails>()
            .filter { it.coverage < 1.0  && it.filesExists() }
        if (suitableClasses.isEmpty()) return null
        val selectedClass = suitableClasses.random()

        val steps = arrayListOf<QuestStep>()
        val line = JacocoUtil.chooseRandomLine(selectedClass, parameters.workspace)
        val method = JacocoUtil.chooseRandomMethod(selectedClass, parameters.workspace)
        val data = Challenge.ChallengeGenerationData(parameters, user, selectedClass, listener, method, line)

        steps.add(QuestStep("", LineCoverageChallenge(data)))
        steps.add(QuestStep("", MethodCoverageChallenge(data)))
        steps.add(QuestStep("", ClassCoverageChallenge(data)))

        return Quest("Expanding - Solve a Line, Method and Class Coverage Challenge", steps)
    }

    /**
     * Generates a new LinesQuest with three different [LineCoverageChallenge]s in the same class.
     */
    @JvmStatic
    fun generateLinesQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>
    ): Quest? {
        val suitableClasses = classes.filterIsInstance<SourceFileDetails>()
            .filter { it.coverage < 1.0  && it.filesExists() }
        if (suitableClasses.isEmpty()) return null
        val selectedClass = suitableClasses.random()

        if (JacocoUtil.getLines(
                JacocoUtil.calculateCurrentFilePath(
                    parameters.workspace, selectedClass.jacocoSourceFile, selectedClass.parameters.remote
                )
            ).size < 3
        ) return null

        val steps = arrayListOf<QuestStep>()
        val lines = hashSetOf<Element>()
        while (lines.size < 3) JacocoUtil.chooseRandomLine(selectedClass, parameters.workspace)?.let { lines.add(it) }
        for (line in lines) {
            val data = Challenge.ChallengeGenerationData(parameters, user, selectedClass, listener, line = line)
            val challenge = LineCoverageChallenge(data)
            if (property.getRejectedChallenges(parameters.projectName).any { it.first == challenge }) {
                return null
            }
            steps.add(QuestStep("", challenge))
        }

        return Quest("Lines over lines - Solve three Line Coverage Challenges", steps)
    }

    /**
     * Generates a new MethodsQuest with three different [MethodCoverageChallenge]s in the same class.
     */
    @JvmStatic
    fun generateMethodsQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>
    ): Quest? {
        val suitableClasses = classes.filterIsInstance<SourceFileDetails>()
            .filter { it.coverage < 1.0  && it.filesExists() }
        if (suitableClasses.isEmpty()) return null
        val selectedClass = suitableClasses.random()

        val suitableMethods = JacocoUtil.getMethodEntries(
            FilePath(parameters.workspace, selectedClass.jacocoMethodFile.absolutePath)
        ).filter { it.missedLines > 0 }
        if (suitableMethods.size < 3) return null

        val methods = suitableMethods.shuffled().take(3)
        val steps = arrayListOf<QuestStep>()
        for (method in methods) {
            val data = Challenge.ChallengeGenerationData(parameters, user, selectedClass, listener, method = method)
            val challenge = MethodCoverageChallenge(data)
            if (property.getRejectedChallenges(parameters.projectName).any { it.first == challenge }) {
                return null
            }
            steps.add(QuestStep("", challenge))
        }

        return Quest("More than methods - Solve three Method Coverage Challenge", steps)
    }

    /**
     * Generates a new MutationQuest with three different [MutationTestChallenge]s in the same class.
     */
    @JvmStatic
    fun generateMutationQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>
    ): Quest? {
        val suitableClasses = classes.filterIsInstance<SourceFileDetails>().filter { it.filesExists() }
        if (suitableClasses.isEmpty()) return null
        val selectedClass = suitableClasses.random()

        val set = hashSetOf<MutationTestChallenge>()
        var count = 0
        while (set.size < 3 && count < 10) {
            val challenge = ChallengeFactory.generateMutationTestChallenge(
                selectedClass,
                parameters.branch,
                parameters.projectName,
                listener,
                parameters.workspace,
                user
            )
            if (challenge != null) set.add(challenge)
            count++
        }

        if (set.size < 3) return null
        val steps = arrayListOf<QuestStep>()
        for (i in 0..2) {
            steps.add(QuestStep("", ArrayList(set)[i]))
        }

        return Quest("Coverage is not everything - Solve three Mutation Test Challenges", steps)
    }

    /**
     * Generates a new PackageQuest with three different [Challenge]s in the same package.
     */
    @JvmStatic
    fun generatePackageQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        files: ArrayList<FileDetails>
    ): Quest? {
        val suitableClasses = files.filterIsInstance<SourceFileDetails>()
            .filter { it.coverage < 1.0  && it.filesExists() }
        if (suitableClasses.isEmpty()) return null
        var selectedClass = suitableClasses.random()

        val classesInPackage = arrayListOf<SourceFileDetails>()
        for (i in 0..2) {
            val filterClasses = suitableClasses.filter { it.packageName == selectedClass.packageName }
            if (filterClasses.size < 3) {
                selectedClass = suitableClasses.random()
            } else {
                classesInPackage.addAll(filterClasses)
                break
            }
        }

        if (classesInPackage.size < 3) return null
        val selectedClasses = classesInPackage.shuffled().take(3)
        val steps = arrayListOf<QuestStep>()
        for (cla in selectedClasses) {
            val challenge = ChallengeFactory.generateChallenge(user, parameters, listener,
                files, cla)
            steps.add(QuestStep("", challenge))
        }

        return Quest("Pack it together - Solve three Challenges in the same package", steps)
    }

    /**
     * Generates a new SmellQuest with [SmellChallenge]s.
     */
    @JvmStatic
    fun generateSmellQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>
    ): Quest? {
        val suitableFiles = classes.filter { it is SourceFileDetails || it is TestFileDetails }.shuffled()
        if (suitableFiles.isEmpty()) return null

        suitableFiles.forEach {file ->
            val smells = SmellUtil.getSmellsOfFile(file, listener)
            if (smells.size < 3) return@forEach

            val selectedSmells = smells.shuffled().take(3)
            val steps = arrayListOf<QuestStep>()
            selectedSmells.forEach {issue ->
                val challenge = SmellChallenge(file, issue)
                steps.add(QuestStep("", challenge))
            }

            return Quest("Smelly - Solve three Smell Challenges in the same class", steps)
        }

        return null
    }

    /**
     * Generates a new TestQuest with three [TestChallenge]s.
     */
    @JvmStatic
    fun generateTestQuest(
        user: User, property: GameUserProperty, parameters: Constants.Parameters, listener: TaskListener,
        classes: ArrayList<FileDetails>
    ): Quest {
        val steps = arrayListOf<QuestStep>()
        val data = Challenge.ChallengeGenerationData(
            parameters,
            user,
            classes.filterIsInstance<SourceFileDetails>().random(),
            listener,
            testCount = JUnitUtil.getTestCount(parameters.workspace),
            headCommitHash = parameters.workspace.act(GitUtil.HeadCommitCallable(parameters.remote)).name
        )
        (0..2).forEach { _ -> steps.add(QuestStep("", TestChallenge(data))) }

        return Quest("Just test - Solve three Test Challenges", steps)
    }
}