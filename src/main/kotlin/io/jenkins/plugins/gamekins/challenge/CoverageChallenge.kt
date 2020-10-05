package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import io.jenkins.plugins.gamekins.util.JacocoUtil.ClassDetails
import io.jenkins.plugins.gamekins.util.JacocoUtil.calculateCoveredLines
import io.jenkins.plugins.gamekins.util.JacocoUtil.calculateCurrentFilePath
import io.jenkins.plugins.gamekins.util.JacocoUtil.generateDocument

abstract class CoverageChallenge(val classDetails: ClassDetails, val branch: String, workspace: FilePath?)
    : Challenge {

    val fullyCoveredLines: Int
    val partiallyCoveredLines: Int
    val notCoveredLines: Int
    val coverage: Double
    var solvedCoverage = 0.0
    //TODO: Fix
    @get:JvmName("getCreated_") val created = System.currentTimeMillis()
    @get:JvmName("getSolved_") var solved: Long = 0

    override fun getCreated(): Long {
        return created
    }

    override fun getSolved(): Long {
        return solved
    }

    abstract fun getName(): String

    override fun printToXML(reason: String, indentation: String): String {
        var print = (indentation + "<" + getName() + " created=\"" + created + "\" solved=\"" + solved
                + "\" class=\"" + classDetails.className + "\" coverage=\"" + coverage
                + "\" coverageAtSolved=\"" + solvedCoverage)
        if (reason.isNotEmpty()) {
            print += "\" reason=\"$reason"
        }
        print += "\"/>"
        return print
    }

    init {
        val document = generateDocument(calculateCurrentFilePath(workspace!!,
                classDetails.jacocoSourceFile, classDetails.workspace))
        fullyCoveredLines = calculateCoveredLines(document, "fc")
        partiallyCoveredLines = calculateCoveredLines(document, "pc")
        notCoveredLines = calculateCoveredLines(document, "nc")
        coverage = classDetails.coverage
    }
}
