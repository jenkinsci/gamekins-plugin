package org.gamekins.util

import hudson.FilePath
import hudson.model.TaskListener
import org.gamekins.file.SourceFileDetails
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
    fun executePIT(fileDetails: SourceFileDetails, parameters: Parameters, listener: TaskListener): Boolean {
        val pom = FilePath(parameters.workspace.channel, "${parameters.workspace.remote}/pom.xml")
        if (!pom.exists()) return false
        val oldPomContent = pom.readToString()
        val newPomContent = oldPomContent
            .replace("</plugins>", "${parameters.pitConfiguration}</plugins>")
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
        listener.logger.println(output)
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
                if ("&lt;[^=]".toRegex().containsMatchIn(snippet)) {
                    snippet.replace("&lt;", "&lt;=")
                } else if ("&lt;=".toRegex().containsMatchIn(snippet)) {
                    snippet.replace("&lt;=", "&lt;")
                } else if ("&gt;[^=]".toRegex().containsMatchIn(snippet)) {
                    snippet.replace("&gt;", "&gt;=")
                } else if ("&gt;=".toRegex().containsMatchIn(snippet)) {
                    snippet.replace("&gt;=", "&gt;")
                } else {
                    ""
                }
            }
            INCREMENTS -> {
                snippet
                    .replace("++", "--")
                    .replace("--", "++")
            }
            INVERT_NEGS -> {
                snippet.replace("-", "")
            }
            MATH -> {
                snippet
                    .replace("+", "-")
                    .replace("-", "+")
                    .replace("*", "/")
                    .replace("/", "*")
                    .replace("%", "*")
                    .replace("&amp;", "|")
                    .replace("|", "&amp;")
                    .replace("^", "&amp;")
                    .replace("&lt;&lt;", "&gt;&gt;")
                    .replace("&gt;&gt;", "&lt;&lt;")
                    .replace("&gt;&gt;&gt;", "&lt;&lt;")
            }
            NEGATE_CONDITIONALS -> {
                if ("&lt;[^=]".toRegex().containsMatchIn(snippet)) {
                    snippet.replace("&lt;", "&gt;=")
                } else if ("&lt;=".toRegex().containsMatchIn(snippet)) {
                    snippet.replace("&lt;=", "&gt;")
                } else if ("&gt;[^=]".toRegex().containsMatchIn(snippet)) {
                    snippet.replace("&gt;", "&lt;=")
                } else if ("&gt;=".toRegex().containsMatchIn(snippet)) {
                    snippet.replace("&gt;=", "&lt;")
                } else if ("==".toRegex().containsMatchIn(snippet)) {
                    snippet.replace("==", "!=")
                } else if ("!=".toRegex().containsMatchIn(snippet)) {
                    snippet.replace("!=", "==")
                } else {
                    ""
                }
            }
            //TODO: Complete (Not used per default)
            RETURN_VALS -> {
                if (snippet.contains("return true")) {
                    snippet.replace("return true", "return false")
                } else if (snippet.contains("return false")) {
                    snippet.replace("return false", "return true")
                } else if (snippet.contains("return 0")) {
                    snippet.replace("return 0", "return 1")
                } else {
                    ""
                }
            }
            VOID_METHOD_CALLS -> ""
            EMPTY_RETURNS -> {
                if ("(Collections\\.\\S*)".toRegex().containsMatchIn(data.description)) {
                    snippet.replace("return .*[^;]".toRegex(),
                        "return " + "(Collections\\.\\S*)".toRegex().find(data.description)!!.groupValues[1] + "()")
                } else if (data.description.contains("&quot;&quot;")) {
                    snippet.replace("return .*[^;]".toRegex(), "return &quot;&quot;")
                } else {
                    snippet.replace("return .*[^;]".toRegex(), "return 0")
                }
            }
            FALSE_RETURNS -> snippet.replace("return .*[^;]".toRegex(), "return false")
            TRUE_RETURNS -> snippet.replace("return .*[^;]".toRegex(), "return true")
            NULL_RETURNS -> {
                if (snippet.contains("return .*[^;]".toRegex())) {
                    snippet.replace("return .*[^;]".toRegex(), "return null")
                } else {
                    val argument = "\\((.*)\\)\\s*->\\s*.*[^;]".toRegex().find(snippet)!!.groupValues[1]
                    snippet.replace("\\(.*\\)\\s*->\\s*.*[^;]".toRegex(), "($argument) -> null")
                }
            }
            PRIMITIVE_RETURNS -> snippet.replace("return .*[^;]".toRegex(), "return 0")
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