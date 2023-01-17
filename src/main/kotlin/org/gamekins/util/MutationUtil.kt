package org.gamekins.util

import hudson.FilePath
import hudson.model.TaskListener
import org.gamekins.file.SourceFileDetails
import org.gamekins.util.Constants.Mutation.RETURN_FALSE
import org.gamekins.util.Constants.Mutation.RETURN_REGEX
import org.gamekins.util.Constants.Mutation.RETURN_TRUE
import org.gamekins.util.Constants.Mutation.RETURN_ZERO
import org.gamekins.util.Constants.Mutation.SHIFT_LEFT
import org.gamekins.util.Constants.Mutation.SHIFT_RIGHT
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.MutationUtil.Mutator.*
import java.io.File

/**
 * Util object for mutation with PIT.
 *
 * @author Philipp Straubinger
 * @since 0.6
 */
object MutationUtil {
    
    const val MUTATOR_PACKAGE = "org.pitest.mutationtest.engine.gregor.mutators"

    /**
     * Adds the configuration taken from [parameters] for PIT to the pom.xml file of the project. Executes PIT on the
     * command line with Maven based on the configuration and the specified class. Returns true if the process
     * finished without errors, false otherwise.
     */
    @JvmStatic
    fun executePIT(fileDetails: SourceFileDetails, parameters: Parameters, listener: TaskListener = TaskListener.NULL)
    : Boolean {
        val pom = FilePath(parameters.workspace.channel, "${parameters.workspace.remote}/pom.xml")
        if (!pom.exists()) return false
        val oldPomContent = pom.readToString()
        val regexPIT = ("<plugin>\\s*<groupId>org.pitest</groupId>\\s*<artifactId>pitest-maven</artifactId>" +
                "[\\s\\S]*?(?=</plugin>)").toRegex()
        val regexPluginManagement = "</plugins>\\s*(?=</pluginManagement>)".toRegex()
        val regexNoPluginManagement = "</plugins>\\s*(?!</pluginManagement>)".toRegex()
        var newPomContent = if (oldPomContent.contains(regexPIT)) {
            oldPomContent.replace(regexPIT, parameters.pitConfiguration.replace("</plugin>", ""))
        } else if (oldPomContent.contains(regexPluginManagement)) {
            oldPomContent.replace(regexNoPluginManagement, "${parameters.pitConfiguration}</plugins>")
        } else {
            oldPomContent.replace("</plugins>", "${parameters.pitConfiguration}</plugins>")
        }
        newPomContent = newPomContent
            .replace("{package}", fileDetails.packageName)
            .replace("{class}", fileDetails.fileName)
        pom.write(newPomContent, null)
        //TODO: Does not work on remote runners
        val process = ProcessBuilder(listOf("mvn", "-B", "pitest:mutationCoverage"))
            .directory(File(parameters.workspace.remote))
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        if (output.contains("BUILD FAILURE")) return false
        if (parameters.showPitOutput) listener.logger.println(output)
        pom.write(oldPomContent, null)
        return true
    }

    /**
     * Returns a mutant based on previous [data] to check whether it is killed. Assumes that the PIT mutation report
     * is stored in <project-root>/target/pit-reports/mutations.xml.
     */
    @JvmStatic
    fun getMutant(data: MutationData, parameters: Parameters): MutationData? {
        val mutationReport = FilePath(parameters.workspace.channel,
            parameters.workspace.remote + "/target/pit-reports/mutations.xml")
        if (!mutationReport.exists()) return null
        val mutants = mutationReport.readToString().split("\n").filter { it.startsWith("<mutation ") }
        if (mutants.isEmpty()) return null
        return mutants.map { MutationData(it) }.find { it == data }
    }

    /**
     * Returns the mutated line of code based on the mutation operator.
     */
    //TODO: Optimize for lambda expressions
    @JvmStatic
    fun getMutatedCode(codeSnippet: String, data: MutationData): String {
        val snippet = codeSnippet.trimEnd()

        return when (data.mutator) {
            CONDITIONALS_BOUNDARY -> {
                when {
                    "&lt;[^=]".toRegex().containsMatchIn(snippet) -> snippet.replace("&lt;", "&lt;=")
                    "&lt;=".toRegex().containsMatchIn(snippet) -> snippet.replace("&lt;=", "&lt;")
                    "&gt;[^=]".toRegex().containsMatchIn(snippet) -> snippet.replace("&gt;", "&gt;=")
                    "&gt;=".toRegex().containsMatchIn(snippet) -> snippet.replace("&gt;=", "&gt;")
                    else -> ""
                }
            }
            INCREMENTS -> {
                when {
                    snippet.contains("++") -> snippet.replace("++", "--")
                    snippet.contains("--") -> snippet.replace("--", "++")
                    snippet.contains("+=") -> snippet.replace("+=", "-=")
                    snippet.contains("-=") -> snippet.replace("-=", "+=")
                    else -> ""
                }

            }
            INVERT_NEGS -> {
                snippet.replace("-", "")
            }
            MATH -> {
                when {
                    snippet.contains("+") -> snippet.replace("+", "-")
                    snippet.contains("-") -> snippet.replace("-", "+")
                    snippet.contains("*") -> snippet.replace("*", "/")
                    snippet.contains("/") -> snippet.replace("/", "*")
                    snippet.contains("%") -> snippet.replace("%", "*")
                    snippet.contains("&amp;") -> snippet.replace("&amp;", "|")
                    snippet.contains("|") -> snippet.replace("|", "&amp;")
                    snippet.contains("^") -> snippet.replace("^", "&amp;")
                    snippet.contains(SHIFT_LEFT) -> snippet.replace(SHIFT_LEFT, SHIFT_RIGHT)
                    snippet.contains("&gt;&gt;&gt;") -> snippet.replace("&gt;&gt;&gt;", SHIFT_LEFT)
                    snippet.contains(SHIFT_RIGHT) -> snippet.replace(SHIFT_RIGHT, SHIFT_LEFT)
                    else -> ""
                }
            }
            NEGATE_CONDITIONALS -> {
                when {
                    "&lt;[^=]".toRegex().containsMatchIn(snippet) -> snippet.replace("&lt;", "&gt;=")
                    "&lt;=".toRegex().containsMatchIn(snippet) -> snippet.replace("&lt;=", "&gt;")
                    "&gt;[^=]".toRegex().containsMatchIn(snippet) -> snippet.replace("&gt;", "&lt;=")
                    "&gt;=".toRegex().containsMatchIn(snippet) -> snippet.replace("&gt;=", "&lt;")
                    "==".toRegex().containsMatchIn(snippet) -> snippet.replace("==", "!=")
                    "!=".toRegex().containsMatchIn(snippet) -> snippet.replace("!=", "==")
                    else -> ""
                }
            }
            //TODO: Complete (Not used per default)
            RETURN_VALS -> {
                when {
                    snippet.contains(RETURN_TRUE) -> snippet.replace(RETURN_TRUE, RETURN_FALSE)
                    snippet.contains(RETURN_FALSE) -> snippet.replace(RETURN_FALSE, RETURN_TRUE)
                    snippet.contains(RETURN_ZERO) -> snippet.replace(RETURN_ZERO, "return 1")
                    else -> ""
                }
            }
            VOID_METHOD_CALLS -> "Line removed"
            EMPTY_RETURNS -> {
                when {
                    "(Collections\\.\\S*)".toRegex().containsMatchIn(data.description) ->
                        snippet.replace(RETURN_REGEX, "return " + "(Collections\\.\\S*)"
                            .toRegex().find(data.description)!!.groupValues[1] + "()")
                    "(Stream\\.\\S*)".toRegex().containsMatchIn(data.description) ->
                        snippet.replace(RETURN_REGEX, "return " + "(Stream\\.\\S*)"
                            .toRegex().find(data.description)!!.groupValues[1] + "()")
                    data.description.contains("&quot;&quot;") ->
                        snippet.replace(RETURN_REGEX, "return &quot;&quot;")
                    else -> snippet.replace(RETURN_REGEX, RETURN_ZERO)
                }
            }
            FALSE_RETURNS -> snippet.replace(RETURN_REGEX, RETURN_FALSE)
            TRUE_RETURNS -> snippet.replace(RETURN_REGEX, RETURN_TRUE)
            NULL_RETURNS -> {
                if (snippet.contains(RETURN_REGEX)) {
                    snippet.replace(RETURN_REGEX, "return null")
                } else {
                    val argument = "\\((.*)\\)\\s*->\\s*.*[^;]".toRegex().find(snippet)!!.groupValues[1]
                    snippet.replace("\\(.*\\)\\s*->\\s*.*[^;]".toRegex(), "($argument) -> null")
                }
            }
            PRIMITIVE_RETURNS -> snippet.replace(RETURN_REGEX, RETURN_ZERO)
            UNKNOWN -> ""
        }
    }

    /**
     * Enum class for the status of a PIT mutant.
     *
     * @author Philipp Straubinger
     * @since 0.6
     */
    enum class MutationStatus {
        NO_COVERAGE, SURVIVED, KILLED
    }

    /**
     * Enum class for the different PIT mutation operators.
     *
     * @author Philipp Straubinger
     * @since 0.6
     */
    enum class Mutator {
        CONDITIONALS_BOUNDARY, INCREMENTS, INVERT_NEGS, MATH, NEGATE_CONDITIONALS, RETURN_VALS, VOID_METHOD_CALLS,
        EMPTY_RETURNS, FALSE_RETURNS, TRUE_RETURNS, NULL_RETURNS, PRIMITIVE_RETURNS, UNKNOWN
    }

    /**
     * Representation of a PIT mutant. Created from one line of the PIT report.
     *
     * @author Philipp Straubinger
     * @since 0.6
     */
    class MutationData(line: String) {

        val detected: Boolean
        val status: MutationStatus
        val numberOfTestsRun: Int
        val sourceFile: String
        val mutatedClass: String
        val mutatedMethod: String
        val methodDescription: String
        val lineNumber: Int
        val mutator: Mutator
        val killingTest: String
        val description: String

        init {
            detected = """detected='([a-z]*)'""".toRegex().find(line)!!.groupValues[1].toBoolean()
            status = when ("""status='([A-Z_]*)'""".toRegex().find(line)!!.groupValues[1]) {
                "NO_COVERAGE" -> MutationStatus.NO_COVERAGE
                "SURVIVED" -> MutationStatus.SURVIVED
                "KILLED" -> MutationStatus.KILLED
                else -> MutationStatus.KILLED
            }
            numberOfTestsRun = """numberOfTestsRun='(\d*)'""".toRegex().find(line)!!.groupValues[1].toInt()
            sourceFile = """<sourceFile>(.*)</sourceFile>""".toRegex().find(line)!!.groupValues[1]
            mutatedClass = """<mutatedClass>(.*)</mutatedClass>""".toRegex().find(line)!!.groupValues[1]
            mutatedMethod = """<mutatedMethod>(.*)</mutatedMethod>""".toRegex().find(line)!!.groupValues[1]
            methodDescription = """<methodDescription>(.*)</methodDescription>""".toRegex().find(line)!!.groupValues[1]
            lineNumber = """<lineNumber>(.*)</lineNumber>""".toRegex().find(line)!!.groupValues[1].toInt()
            mutator = when ("""<mutator>(.*)</mutator>""".toRegex().find(line)!!.groupValues[1]) {
                "$MUTATOR_PACKAGE.ConditionalsBoundaryMutator" -> CONDITIONALS_BOUNDARY
                "$MUTATOR_PACKAGE.IncrementsMutator" -> INCREMENTS
                "$MUTATOR_PACKAGE.InvertNegsMutator" -> INVERT_NEGS
                "$MUTATOR_PACKAGE.MathMutator" -> MATH
                "$MUTATOR_PACKAGE.NegateConditionalsMutator" -> NEGATE_CONDITIONALS
                "$MUTATOR_PACKAGE.ReturnValsMutator" -> RETURN_VALS
                "$MUTATOR_PACKAGE.VoidMethodCallMutator" -> VOID_METHOD_CALLS
                "$MUTATOR_PACKAGE.returns.EmptyObjectReturnValsMutator" -> EMPTY_RETURNS
                "$MUTATOR_PACKAGE.returns.BooleanFalseReturnValsMutator" -> FALSE_RETURNS
                "$MUTATOR_PACKAGE.returns.BooleanTrueReturnValsMutator" -> TRUE_RETURNS
                "$MUTATOR_PACKAGE.returns.NullReturnValsMutator" -> NULL_RETURNS
                "$MUTATOR_PACKAGE.returns.PrimitiveReturnsMutator" -> PRIMITIVE_RETURNS
                else -> UNKNOWN
            }
            killingTest = (if (line.contains("<killingTest/>")) "" else
                """<killingTest>(.*)</killingTest>""".toRegex().find(line)!!.groupValues[1])
            description = """<description>(.*)</description>""".toRegex().find(line)!!.groupValues[1]
        }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other !is MutationData) return false
            return other.sourceFile == this.sourceFile
                    && other.mutatedClass == this.mutatedClass
                    && other.mutatedMethod == this.mutatedMethod
                    && other.methodDescription == this.methodDescription
                    && other.lineNumber == this.lineNumber
                    && other.mutator == this.mutator
                    && other.description == this .description
        }

        override fun hashCode(): Int {
            var result = detected.hashCode()
            result = 31 * result + status.hashCode()
            result = 31 * result + numberOfTestsRun
            result = 31 * result + sourceFile.hashCode()
            result = 31 * result + mutatedClass.hashCode()
            result = 31 * result + mutatedMethod.hashCode()
            result = 31 * result + methodDescription.hashCode()
            result = 31 * result + lineNumber
            result = 31 * result + mutator.hashCode()
            result = 31 * result + killingTest.hashCode()
            result = 31 * result + description.hashCode()
            return result
        }
    }
}