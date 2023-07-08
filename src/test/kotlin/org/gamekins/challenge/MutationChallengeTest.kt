package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.file.SourceFileDetails
import org.gamekins.test.TestUtils
import org.gamekins.util.Constants.Parameters
import org.gamekins.util.JacocoUtil
import org.gamekins.util.MutationUtil
import org.gamekins.util.MutationUtil.MutationData
import org.gamekins.util.Pair
import org.jsoup.nodes.Document
import java.io.File

class MutationChallengeTest : FeatureSpec({

    val className = "Challenge"
    val path = FilePath(null, "/home/test/workspace")
    val shortFilePath = "src/main/java/org/gamekins/challenge/$className.kt"
    val shortJacocoPath = "**/target/site/jacoco/"
    val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    lateinit var details: SourceFileDetails
    lateinit var data : MutationData
    lateinit var challenge: MutationChallenge
    val line = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
            "<sourceFile>Complex.java</sourceFile><mutatedClass>com.example.Complex</mutatedClass>" +
            "<mutatedMethod>abs</mutatedMethod><methodDescription>()D</methodDescription>" +
            "<lineNumber>109</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>" +
            "<indexes><index>7</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
            "<description>Replaced double multiplication with division</description></mutation>"
    val listener = TaskListener.NULL
    val parameters = Parameters()
    val branch = "master"
    val coverage = 0.0
    val run = mockkClass(Run::class)
    lateinit var root : String
    lateinit var testProjectPath : FilePath

    beforeContainer {
        parameters.branch = branch
        parameters.workspace = path
        parameters.jacocoResultsPath = shortJacocoPath
        parameters.jacocoCSVPath = shortJacocoCSVPath
        data = MutationData(line)
        mockkStatic(JacocoUtil::class)
        mockkStatic(MutationUtil::class)
        val document = mockkClass(Document::class)
        every { JacocoUtil.calculateCurrentFilePath(any(), any()) } returns path
        every { JacocoUtil.getCoverageInPercentageFromJacoco(any(), any()) } returns coverage
        every { JacocoUtil.generateDocument(any()) } returns document
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 0
        details = SourceFileDetails(parameters, shortFilePath, listener)
        challenge = MutationChallenge(details, data)
    }

    beforeSpec {
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
        testProjectPath = FilePath(null, root)
    }

    afterSpec {
        unmockkAll()
        File(root).deleteRecursively()
    }

    feature("createCodeSnippet") {
        var codeChallenge = MutationChallenge(details, MutationData(
            line.replace("<lineNumber>109</lineNumber>", "<lineNumber>0</lineNumber>"))
        )
        scenario("LineNumber is 0")
        {
            codeChallenge.getSnippet() shouldBe ""
        }

        codeChallenge = MutationChallenge(details, MutationData(
            line.replace("<lineNumber>109</lineNumber>", "<lineNumber>-109</lineNumber>"))
        )
        scenario("LineNumber negative")
        {
            codeChallenge.getSnippet() shouldBe ""
        }

        val file = mockkClass(File::class)
        every { file.exists() } returns false
        val codeDetails = mockkClass(SourceFileDetails::class)
        every { codeDetails.jacocoSourceFile } returns file
        codeChallenge = MutationChallenge(codeDetails, MutationData(line))
        scenario("jacocoSourceFile does not exist")
        {
            codeChallenge.getSnippet() shouldBe ""
        }

        every { file.exists() } returns true
        val filePath = mockkClass(FilePath::class)
        every { JacocoUtil.calculateCurrentFilePath(any(), any(), any()) } returns filePath
        var elements = Pair("", "")
        every { JacocoUtil.getLinesInRange(any(), any(), any()) } returns elements
        every { codeDetails.parameters } returns parameters
        codeChallenge = MutationChallenge(codeDetails, MutationData(line))
        scenario("Lines are empty")
        {
            codeChallenge.getSnippet() shouldBe ""
        }

        elements = Pair("Test", "")
        every { JacocoUtil.getLinesInRange(any(), any(), any()) } returns elements
        every { MutationUtil.getMutatedCode(any(), any()) } returns ""
        codeChallenge = MutationChallenge(codeDetails, MutationData(line))
        scenario("Without mutated line")
        {
            codeChallenge.getSnippet() shouldBe "Write or update tests so that they fail on the mutant described below.\n" +
                    "Original code snippet\n" +
                    "<pre class='prettyprint linenums:108 mt-2'><code class='language-java'>Test</code></pre>Mutated line of code \n" +
                    "<br><em>No mutated line available</em><br>The mutated line is built from information provided by PIT and could be syntactically invalid or wrong. Please use along with the description in that case:<br><a href=\"https://pitest.org/quickstart/mutators/#MATH\"target=\"_blank\">Replaced double multiplication with division</a> "

        }

        every { MutationUtil.getMutatedCode(any(), any()) } returns "Mutant"
        codeChallenge = MutationChallenge(codeDetails, MutationData(line))
        scenario("With mutated line")
        {
            codeChallenge.getSnippet() shouldBe "Write or update tests so that they fail on the mutant described below.\n" +
                    "Original code snippet\n" +
                    "<pre class='prettyprint linenums:108 mt-2'><code class='language-java'>Test</code></pre>Mutated line of code \n" +
                    "<pre class='prettyprint linenums:109 mt-2'><code class='language-java'>Mutant</code></pre>The mutated line is built from information provided by PIT and could be syntactically invalid or wrong. Please use along with the description in that case:<br><a href=\"https://pitest.org/quickstart/mutators/#MATH\"target=\"_blank\">Replaced double multiplication with division</a> "

        }
    }

    feature("equals") {
        scenario("Null")
        {
            challenge.equals(null) shouldBe false
        }

        scenario("Not Challenge")
        {
            challenge.equals(listener) shouldBe false
        }

        scenario("Self")
        {
            (challenge == challenge) shouldBe true
        }

        var details2 = SourceFileDetails(parameters, shortFilePath.replace("gamekins", "game"))
        var challenge2 = MutationChallenge(details2, data)
        scenario("Different PackageName")
        {
            (challenge == challenge2) shouldBe false
        }

        details2 = SourceFileDetails(parameters, shortFilePath.replace("Challenge", "Property"))
        challenge2 = MutationChallenge(details2, data)
        scenario("Different FileName")
        {
            (challenge == challenge2) shouldBe false
        }

        val line2 = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>Rational.java</sourceFile><mutatedClass>com.example.Complex</mutatedClass>" +
                "<mutatedMethod>abs</mutatedMethod><methodDescription>()D</methodDescription>" +
                "<lineNumber>109</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>" +
                "<indexes><index>7</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
                "<description>Replaced double multiplication with division</description></mutation>"
        val data2 = MutationData(line2)
        challenge2 = MutationChallenge(details, data2)
        scenario("Different Data")
        {
            (challenge == challenge2) shouldBe false
        }

        challenge2 = MutationChallenge(details, data)
        scenario("Equivalent Challenge")
        {
            (challenge == challenge2) shouldBe true
        }
    }

    feature("getKillingTest") {
        scenario("No KillingTest yet")
        {
            challenge.getKillingTest() shouldBe ""
        }

        val killingLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>Complex.java</sourceFile><mutatedClass>com.example.Complex</mutatedClass>" +
                "<mutatedMethod>abs</mutatedMethod><methodDescription>()D</methodDescription>" +
                "<lineNumber>109</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>" +
                "<indexes><index>7</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>com.example.ComplexTest.[engine:junit-jupiter]/[class:com.example.ComplexTest]/" +
                "[method:testAdd()]</killingTest>>" +
                "<description>Replaced double multiplication with division</description></mutation>"
        val killingData = MutationData(killingLine)
        val challenge2 = MutationChallenge(details, killingData)
        scenario("Has KillingTest")
        {
            challenge2.getKillingTest() shouldBe "com.example.ComplexTest.testAdd()"
        }
    }

    feature("getName") {
        challenge.getName() shouldBe "Mutation"
    }

    feature("getParameters") {
        challenge.getParameters() shouldBe parameters
    }

    feature("getScore") {
        scenario("Default Value")
        {
            challenge.getScore() shouldBe 4
        }

        val newChallenge = MutationChallenge(details, MutationData(
            line.replace("status='NO_COVERAGE'", "status='SURVIVED'"))
        )
        scenario("Mutant has survived")
        {
            newChallenge.getScore() shouldBe 5
        }
    }

    feature("isSolvable") {
        val solvableDetails = mockkClass(SourceFileDetails::class)
        every { solvableDetails.jacocoSourceFile } returns details.jacocoSourceFile
        every { solvableDetails.parameters } returns parameters
        every { solvableDetails.coverage } returns details.coverage
        every { solvableDetails.fileName } returns details.fileName
        every { solvableDetails.update(any()) } returns solvableDetails
        every { solvableDetails.filesExists() } returns false
        val solvableChallenge = MutationChallenge(solvableDetails, challenge.data)
        val newParameters = Parameters(branch = "stale")
        scenario("Different branch")
        {
            solvableChallenge.isSolvable(newParameters, run, listener) shouldBe true
        }

        scenario("SourceFileDetails do not exist")
        {
            solvableChallenge.isSolvable(parameters, run, listener) shouldBe false
        }

        every { solvableDetails.filesExists() } returns true
        every { MutationUtil.executePIT(any(), any(), any()) } returns false
        scenario("PIT could not execute")
        {
            solvableChallenge.isSolvable(parameters, run, listener) shouldBe false
        }

        every { MutationUtil.executePIT(any(), any(), any()) } returns true
        scenario("MutationReport does not exist")
        {
            solvableChallenge.isSolvable(parameters, run, listener) shouldBe true
        }

        /*
        parameters.workspace = testProjectPath
	every { JacocoUtil.getLineNumberAfterCodeChange(any(), any(), any(), any(), any(), any()) } returns 109
        scenario("Mutant not detected")
        {
            solvableChallenge.isSolvable(parameters, run, listener) shouldBe true
        }

         */

        every { MutationUtil.getMutant(any(), any()) } returns null
        scenario("Mutant was not generated by PIT this time")
        {
            solvableChallenge.isSolvable(parameters, run, listener) shouldBe true
        }

        /*
        val mutant = MutationData(line.replace("detected='false'", "detected='true'"))
        every { MutationUtil.getMutant(any(), any()) } returns mutant
        scenario("Mutant detected")
        {
            solvableChallenge.isSolvable(parameters, run, listener) shouldBe false //Error hier
        }

         */
    }

    feature("isSolved") {
        every { MutationUtil.executePIT(any(), any(), any()) } returns false
        scenario("PIT could not execute")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false
        }

        /*
        every { MutationUtil.executePIT(any(), any(), any()) } returns true
        every { MutationUtil.getMutant(any(), any()) } returns null
	every { JacocoUtil.getLineNumberAfterCodeChange(any(), any(), any(), any(), any(), any()) } returns 109
        scenario("Mutant is null")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false
        }

         */

        var mutant = MutationData(line.replace("detected='false'", "detected='true'"))
        every { MutationUtil.getMutant(any(), any()) } returns mutant
        scenario("Mutant is not covered")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false
        }

        mutant = MutationData(line.replace("status='NO_COVERAGE'", "status='KILLED'"))
        every { MutationUtil.getMutant(any(), any()) } returns mutant
        scenario("Mutant not detected")
        {
            challenge.isSolved(parameters, run, listener) shouldBe false
        }

        /*
        mutant = MutationData(line
            .replace("detected='false'", "detected='true'")
            .replace("status='NO_COVERAGE'", "status='KILLED'"))
        every { MutationUtil.getMutant(any(), any()) } returns mutant
        scenario("Solved")
        {
            challenge.isSolved(parameters, run, listener) shouldBe true //Error here
        }

         */
    }

    feature("printToXML") {
        var printOutput = "<MutationChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"Challenge\" " +
                "detected=\"false\" status=\"NO_COVERAGE\" numberOfTestsRun=\"0\" mutator=\"MATH\" " +
                "killingTest=\"\" description=\"Replaced double multiplication with division\"/>"
        scenario("No Reason, no Indentation")
        {
            challenge.printToXML("", "") shouldBe printOutput
        }

        printOutput = "    <MutationChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"Challenge\" " +
                "detected=\"false\" status=\"NO_COVERAGE\" numberOfTestsRun=\"0\" mutator=\"MATH\" " +
                "killingTest=\"\" description=\"Replaced double multiplication with division\"/>"
        scenario("No Reason, with Indentation")
        {
            challenge.printToXML("", "    ") shouldBe printOutput
        }

        printOutput = "    <MutationChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"Challenge\" " +
                "detected=\"false\" status=\"NO_COVERAGE\" numberOfTestsRun=\"0\" mutator=\"MATH\" " +
                "killingTest=\"\" description=\"Replaced double multiplication with division\" reason=\"test\"/>"
        scenario("With Reason, no Indentation")
        {
            challenge.printToXML("test", "    ") shouldBe printOutput
        }
    }

    feature("ToString") {
        var stringOutput = "Write a test to kill the mutant at line <b>109</b> of method <b>abs()</b> in class " +
                "<b>Challenge</b> in package <b>org.gamekins.challenge</b> (created for branch master)"
        scenario("Kill mutant in method abs")
        {
            challenge.toString() shouldBe stringOutput
        }

        val lineConstructor = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
            "<sourceFile>Complex.java</sourceFile><mutatedClass>com.example.Complex</mutatedClass>" +
            "<mutatedMethod>&lt;init&gt;</mutatedMethod><methodDescription>()D</methodDescription>" +
            "<lineNumber>109</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>" +
            "<indexes><index>7</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
            "<description>Replaced double multiplication with division</description></mutation>"
        val dataConstructor = MutationData(lineConstructor)
        val challengeConstructor = MutationChallenge(details, dataConstructor)
        stringOutput = "Write a test to kill the mutant at line <b>109</b> of method <b>Complex()</b> in class " +
                "<b>Challenge</b> in package <b>org.gamekins.challenge</b> (created for branch master)"
        scenario("Kill mutant in method Complex")
        {
            challengeConstructor.toString() shouldBe stringOutput
        }
    }
})
