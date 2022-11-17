package org.gamekins.util

import hudson.FilePath
import hudson.model.TaskListener
import jenkins.model.Jenkins
import org.gamekins.challenge.MutationChallenge
import org.gamekins.file.SourceFileDetails
import org.pitest.mutationtest.config.PluginServices
import org.pitest.mutationtest.config.ReportOptions
import org.pitest.mutationtest.tooling.EntryPoint
import org.pitest.testapi.TestGroupConfig
import java.io.File
import java.util.concurrent.TimeUnit

object MutationUtil {
    
    const val MUTATOR_PACKAGE = "org.pitest.mutationtest.engine.gregor.mutators"

    fun executePIT(fileDetails: SourceFileDetails, parameters: Constants.Parameters, listener: TaskListener) {
        val pom = FilePath(parameters.workspace.channel, "${parameters.workspace.remote}/pom.xml")
        if (!pom.exists()) return
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
        listener.logger.println(output)
        pom.write(oldPomContent, null)
    }

    fun getMutantsOfClass(details: SourceFileDetails): List<String> {
        val entry = EntryPoint()
        val plugins = PluginServices.makeForContextLoader()
        val data = ReportOptions()
        val workspace = File(details.parameters.workspace.toURI().path)
        data.targetClasses = listOf("${details.packageName}.${details.fileName}*")
        data.reportDir = "${workspace.absolutePath}/target/pit-reports"
        data.setSourceDirs(listOf(
            File("${workspace.absolutePath}/src/main/java").toPath(),
            File("${workspace.absolutePath}/src/test/java").toPath()))
        data.groupConfig = TestGroupConfig()
        data.addOutputFormats(listOf("XML"))
        val root = Jenkins.get().root.absolutePath
        data.classPathElements = listOf(
            "${workspace.absolutePath}/target/test-classes",
            "${workspace.absolutePath}/target/classes",
            "$root/.m2/repository/org/pitest/pitest-junit5-plugin/1.1.0/pitest-junit5-plugin-1.1.0.jar",
            "$root/.m2/repository/org/pitest/pitest/1.9.10/pitest-1.9.10.jar",
            "$root/.m2/repository/org/junit/jupiter/junit-jupiter-engine/5.7.0/junit-jupiter-engine-5.7.0.jar",
            "$root/.m2/repository/org/junit/jupiter/junit-jupiter-api/5.7.0/junit-jupiter-api-5.7.0.jar",
            "$root/.m2/repository/org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar",
            "$root/.m2/repository/com/google/guava/guava/29.0-jre/guava-29.0-jre.jar")
        entry.execute(workspace, data, plugins, hashMapOf())

        val dir = File(data.reportDir).listFiles()?.maxOf { it.name.toLong() }
        val mutationReport = File("${data.reportDir}/$dir/mutations.xml")
        return mutationReport.readLines().filter { it.startsWith("<mutation ") }
    }

    enum class MutationStatus {
        NO_COVERAGE, SURVIVED, KILLED
    }

    enum class Mutator {
        CONDITIONALS_BOUNDARY, INCREMENTS, INVERT_NEGS, MATH, NEGATE_CONDITIONALS, RETURN_VALS, VOID_METHOD_CALLS,
        EMPTY_RETURNS, FALSE_RETURNS, TRUE_RETURNS, NULL_RETURNS, PRIMITIVE_RETURNS, UNKNOWN
    }

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
                "$MUTATOR_PACKAGE.ConditionalsBoundaryMutator" -> Mutator.CONDITIONALS_BOUNDARY
                "$MUTATOR_PACKAGE.IncrementsMutator" -> Mutator.INCREMENTS
                "$MUTATOR_PACKAGE.InvertNegsMutator" -> Mutator.INVERT_NEGS
                "$MUTATOR_PACKAGE.MathMutator" -> Mutator.MATH
                "$MUTATOR_PACKAGE.NegateConditionalsMutator" -> Mutator.NEGATE_CONDITIONALS
                "$MUTATOR_PACKAGE.ReturnValsMutator" -> Mutator.RETURN_VALS
                "$MUTATOR_PACKAGE.VoidMethodCallMutator" -> Mutator.VOID_METHOD_CALLS
                "$MUTATOR_PACKAGE.returns.EmptyObjectReturnValsMutator" -> Mutator.EMPTY_RETURNS
                "$MUTATOR_PACKAGE.returns.BooleanFalseReturnValsMutator" -> Mutator.FALSE_RETURNS
                "$MUTATOR_PACKAGE.returns.BooleanTrueReturnValsMutator" -> Mutator.TRUE_RETURNS
                "$MUTATOR_PACKAGE.returns.NullReturnValsMutator" -> Mutator.NULL_RETURNS
                "$MUTATOR_PACKAGE.returns.PrimitiveReturnsMutator" -> Mutator.PRIMITIVE_RETURNS
                else -> Mutator.UNKNOWN
            }
            killingTest = (if (line.contains("<killingTest/>")) "" else
                """<killingTest>(.*)</killingTest>""".toRegex().find(line)!!.groupValues[1])
            description = """<description>(.*)</description>""".toRegex().find(line)!!.groupValues[1]
        }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other !is MutationData) return false
            return other.sourceFile ==this.sourceFile
                    && other.mutatedClass == this.mutatedClass
                    && other.mutatedMethod == this.mutatedMethod
                    && other.methodDescription == this.methodDescription
                    && other.lineNumber == this.lineNumber
                    && other.mutator == this.mutator
                    && other.description == this .description
        }
    }
}