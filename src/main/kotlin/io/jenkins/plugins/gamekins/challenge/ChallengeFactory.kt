package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.TaskListener
import hudson.model.User
import io.jenkins.plugins.gamekins.util.GitUtil.HeadCommitCallable
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import io.jenkins.plugins.gamekins.util.JacocoUtil.calculateCoveredLines
import io.jenkins.plugins.gamekins.util.JacocoUtil.calculateCurrentFilePath
import io.jenkins.plugins.gamekins.util.JacocoUtil.generateDocument
import io.jenkins.plugins.gamekins.util.JacocoUtil.getTestCount
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.*

object ChallengeFactory {

    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun generateChallenge(user: User, constants: HashMap<String, String>, listener: TaskListener,
                          classes: ArrayList<ClassDetails>, workspace: FilePath): Challenge {
        if (Math.random() > 0.9) {
            listener.logger.println("[Gamekins] Generated new TestChallenge")
            return TestChallenge(workspace.act(HeadCommitCallable(workspace.remote)).name,
                    getTestCount(workspace), user, constants["branch"]!!)
        }
        val worklist = ArrayList(classes)
        val c = 1.5
        val rankValues = DoubleArray(worklist.size)
        for (i in worklist.indices) {
            rankValues[i] = (2 - c + 2 * (c - 1) * (i / (worklist.size - 1).toDouble())) / worklist.size.toDouble()
            if (i != 0) rankValues[i] += rankValues[i - 1]
        }
        var challenge: Challenge?
        val random = Random()
        var count = 0
        do {
            if (count == 5 || worklist.isEmpty()) {
                listener.logger.println("[Gamekins] No CoverageChallenge could be built")
                return DummyChallenge()
            }
            val probability = Math.random()
            var selectedClass = worklist[worklist.size - 1]
            for (i in worklist.indices) {
                if (rankValues[i] > probability) {
                    selectedClass = worklist[i]
                    break
                }
            }
            //TODO: Make more beautiful
            val challengeType = random.nextInt(4)
            var challengeClass: Class<*>
            challengeClass = when (challengeType) {
                0 -> {
                    ClassCoverageChallenge::class.java
                }
                1 -> {
                    MethodCoverageChallenge::class.java
                }
                else -> {
                    LineCoverageChallenge::class.java
                }
            }
            listener.logger.println("[Gamekins] Try class " + selectedClass.className + " and type "
                    + challengeClass.simpleName)
            challenge = generateCoverageChallenge(selectedClass, challengeClass, constants["branch"],
                    listener, workspace)
            if ((challenge is MethodCoverageChallenge
                            && challenge.methodName == null)
                    || (challenge is LineCoverageChallenge
                            && challenge.lineContent == null)) {
                challenge = null
            }
            worklist.remove(selectedClass)
            count++
        } while (challenge == null)
        return challenge
    }

    //TODO: Create Enum
    @Throws(IOException::class, InterruptedException::class)
    private fun generateCoverageChallenge(classDetails: ClassDetails,
                                          challengeClass: Class<*>, branch: String?,
                                          listener: TaskListener, workspace: FilePath): CoverageChallenge? {
        val document: Document
        document = try {
            generateDocument(calculateCurrentFilePath(workspace,
                    classDetails.jacocoSourceFile, classDetails.workspace))
        } catch (e: Exception) {
            listener.logger.println("[Gamekins] Exception with JaCoCoSourceFile "
                    + classDetails.jacocoSourceFile.absolutePath)
            e.printStackTrace(listener.logger)
            throw e
        }
        return if (calculateCoveredLines(document, "pc") > 0
                || calculateCoveredLines(document, "nc") > 0) {
            when (challengeClass) {
                ClassCoverageChallenge::class.java -> {
                    ClassCoverageChallenge(classDetails, branch!!, workspace)
                }
                MethodCoverageChallenge::class.java -> {
                    MethodCoverageChallenge(classDetails, branch!!, workspace)
                }
                else -> {
                    LineCoverageChallenge(classDetails, branch!!, workspace)
                }
            }
        } else null
    }
}
