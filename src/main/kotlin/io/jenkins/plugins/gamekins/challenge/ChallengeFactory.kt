package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Result
import hudson.model.TaskListener
import hudson.model.User
import io.jenkins.plugins.gamekins.GameUserProperty
import io.jenkins.plugins.gamekins.util.GitUtil
import io.jenkins.plugins.gamekins.util.GitUtil.HeadCommitCallable
import io.jenkins.plugins.gamekins.util.JacocoUtil
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.*
import kotlin.jvm.Throws
import kotlin.random.Random

/**
 * Factory for generating [Challenge]s.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
object ChallengeFactory {

    enum class Challenges {
        ClassCoverageChallenge, LineCoverageChallenge, MethodCoverageChallenge
    }

    /**
     * Generates a new [BuildChallenge] if the [result] was not [Result.SUCCESS] and returns true.
     */
    @JvmStatic
    fun generateBuildChallenge(result: Result?, user: User, workspace: FilePath, property: GameUserProperty,
                               constants: HashMap<String, String>, listener: TaskListener = TaskListener.NULL)
            : Boolean {
        try {
            if (result != null && result != Result.SUCCESS) {
                val challenge = BuildChallenge(constants)
                val mapUser: User? = GitUtil.mapUser(workspace.act(HeadCommitCallable(workspace.remote))
                        .authorIdent, User.getAll())

                if (mapUser == user
                        && !property.getCurrentChallenges(constants["projectName"]).contains(challenge)) {
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
    fun generateChallenge(user: User, constants: HashMap<String, String>, listener: TaskListener,
                          classes: ArrayList<ClassDetails>, workspace: FilePath): Challenge {
        if (Random.nextDouble() > 0.9) {
            listener.logger.println("[Gamekins] Generated new TestChallenge")
            return TestChallenge(workspace.act(HeadCommitCallable(workspace.remote)).name,
                    JacocoUtil.getTestCount(workspace), user, constants["branch"]!!, constants)
        }

        val workList = ArrayList(classes)
        val c = 1.5
        val rankValues = DoubleArray(workList.size)
        for (i in workList.indices) {
            rankValues[i] = (2 - c + 2 * (c - 1) * (i / (workList.size - 1).toDouble())) / workList.size.toDouble()
            if (i != 0) rankValues[i] += rankValues[i - 1]
        }

        var challenge: Challenge?
        var count = 0
        do {
            if (count == 5 || workList.isEmpty()) {
                listener.logger.println("[Gamekins] No CoverageChallenge could be built")
                return DummyChallenge(constants)
            }

            val probability = Random.nextDouble()
            var selectedClass = workList[workList.size - 1]
            for (i in workList.indices) {
                if (rankValues[i] > probability) {
                    selectedClass = workList[i]
                    break
                }
            }

            workList.remove(selectedClass)
            count++

            val rejectedChallenges = user.getProperty(GameUserProperty::class.java)
                    .getRejectedChallenges(constants["projectName"])

            //Remove classes where a ClassCoverageChallenge has been rejected previously
            if (!rejectedChallenges.filter {
                        it.first is ClassCoverageChallenge
                            && (it.first as ClassCoverageChallenge).classDetails.className == selectedClass.className
                            && (it.first as ClassCoverageChallenge).classDetails.packageName == selectedClass.packageName}
                            .isNullOrEmpty()) {
                listener.logger.println("[Gamekins] Class ${selectedClass.className} in package " +
                        "${selectedClass.packageName} was rejected previously")
                challenge = null
                continue
            }

            val challengeClass = when (Random.nextInt(4)) {
                0 -> {
                    Challenges.ClassCoverageChallenge
                }
                1 -> {
                    Challenges.MethodCoverageChallenge
                }
                else -> {
                    Challenges.LineCoverageChallenge
                }
            }

            listener.logger.println("[Gamekins] Try class " + selectedClass.className + " and type " + challengeClass)
            challenge = generateCoverageChallenge(selectedClass, challengeClass, constants["branch"],
                    listener, workspace)

            //TODO: Overwrite all equals() methods
            if (rejectedChallenges.contains(challenge)) {
                listener.logger.println("[Gamekins] Challenge $challenge was already rejected previously")
                challenge = null
            }
        } while (challenge == null)

        return challenge
    }

    /**
     * Generates a new [CoverageChallenge] of type [challengeClass] for the current class with details [classDetails]
     * and the current [branch]. The [workspace] is the folder with the code and execution rights, and the [listener]
     * reports the events to the console output of Jenkins.
     */
    @Throws(IOException::class, InterruptedException::class)
    private fun generateCoverageChallenge(classDetails: ClassDetails, challengeClass: Challenges, branch: String?,
                                          listener: TaskListener, workspace: FilePath): CoverageChallenge? {
        val document: Document
        document = try {
            JacocoUtil.generateDocument(JacocoUtil.calculateCurrentFilePath(workspace,
                    classDetails.jacocoSourceFile, classDetails.workspace))
        } catch (e: Exception) {
            listener.logger.println("[Gamekins] Exception with JaCoCoSourceFile "
                    + classDetails.jacocoSourceFile.absolutePath)
            e.printStackTrace(listener.logger)
            throw e
        }

        return if (JacocoUtil.calculateCoveredLines(document, "pc") > 0
                || JacocoUtil.calculateCoveredLines(document, "nc") > 0) {
            when (challengeClass) {
                Challenges.ClassCoverageChallenge -> {
                    ClassCoverageChallenge(classDetails, branch!!, workspace)
                }
                Challenges.MethodCoverageChallenge -> {
                    val method = JacocoUtil.chooseRandomMethod(classDetails, workspace)
                    if (method == null) null else MethodCoverageChallenge(classDetails, branch!!, workspace, method)
                }
                else -> {
                    val line = JacocoUtil.chooseRandomLine(classDetails, workspace)
                    if (line == null) null else LineCoverageChallenge(classDetails, branch!!, workspace, line)
                }
            }
        } else null
    }

    /**
     * Generates new Challenges for a [user] if he has less than [maxChallenges] Challenges after checking the solved
     * and solvable state of his Challenges. Returns the number of generated Challenges for debug output.
     */
    @JvmStatic
    fun generateNewChallenges(user: User, property: GameUserProperty, constants: HashMap<String, String>,
                              classes: ArrayList<ClassDetails>, workspace: FilePath,
                              listener: TaskListener = TaskListener.NULL, maxChallenges: Int = 3): Int {

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
                        challenge = generateChallenge(user, constants, listener,
                                userClasses, workspace)

                        listener.logger.println("[Gamekins] Generated challenge $challenge")
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
                    listener.logger.println("[Gamekins] Added challenge $challenge")
                    generated++
                } catch (e: Exception) {
                    e.printStackTrace(listener.logger)
                }
            }
        }

        return generated
    }
}
