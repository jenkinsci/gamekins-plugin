package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.TaskListener
import hudson.model.User
import io.jenkins.plugins.gamekins.GameUserProperty
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
                    JacocoUtil.getTestCount(workspace), user, constants["branch"]!!)
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
                return DummyChallenge()
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
                        it is ClassCoverageChallenge
                            && it.classDetails.className == selectedClass.className
                            && it.classDetails.packageName == selectedClass.packageName}
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
}
