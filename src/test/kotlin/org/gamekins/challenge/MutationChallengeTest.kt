package org.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.gamekins.file.SourceFileDetails
import org.gamekins.test.TestUtils
import org.gamekins.util.Constants
import org.gamekins.util.JacocoUtil
import org.gamekins.util.MutationUtil
import org.gamekins.util.MutationUtil.MutationData
import org.gamekins.util.Pair
import org.jsoup.nodes.Document
import java.io.File

class MutationChallengeTest : AnnotationSpec() {

    private val className = "Challenge"
    private val path = FilePath(null, "/home/test/workspace")
    private val shortFilePath = "src/main/java/org/gamekins/challenge/$className.kt"
    private val shortJacocoPath = "**/target/site/jacoco/"
    private val shortJacocoCSVPath = "**/target/site/jacoco/csv"
    private lateinit var details: SourceFileDetails
    private lateinit var data : MutationData
    private lateinit var challenge: MutationChallenge
    private val line = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
            "<sourceFile>Complex.java</sourceFile><mutatedClass>com.example.Complex</mutatedClass>" +
            "<mutatedMethod>abs</mutatedMethod><methodDescription>()D</methodDescription>" +
            "<lineNumber>109</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>" +
            "<indexes><index>7</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
            "<description>Replaced double multiplication with division</description></mutation>"
    private val listener = TaskListener.NULL
    private val parameters = Constants.Parameters()
    private val branch = "master"
    private val coverage = 0.0
    private val run = mockkClass(Run::class)
    private lateinit var root : String
    private lateinit var testProjectPath : FilePath

    @BeforeEach
    fun init() {
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

    @BeforeAll
    fun initAll() {
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
        testProjectPath = FilePath(null, root)
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
        File(root).deleteRecursively()
    }

    @Test
    fun createCodeSnippet() {
        var codeChallenge = MutationChallenge(details, MutationData(
            line.replace("<lineNumber>109</lineNumber>", "<lineNumber>0</lineNumber>"))
        )
        codeChallenge.getSnippet() shouldBe ""

        codeChallenge = MutationChallenge(details, MutationData(
            line.replace("<lineNumber>109</lineNumber>", "<lineNumber>-109</lineNumber>"))
        )
        codeChallenge.getSnippet() shouldBe ""

        val file = mockkClass(File::class)
        every { file.exists() } returns false
        val codeDetails = mockkClass(SourceFileDetails::class)
        every { codeDetails.jacocoSourceFile } returns file
        codeChallenge = MutationChallenge(codeDetails, MutationData(line))
        codeChallenge.getSnippet() shouldBe ""

        every { file.exists() } returns true
        val filePath = mockkClass(FilePath::class)
        every { JacocoUtil.calculateCurrentFilePath(any(), any(), any()) } returns filePath
        var elements = Pair("", "")
        every { JacocoUtil.getLinesInRange(any(), any(), any()) } returns elements
        every { codeDetails.parameters } returns parameters
        codeChallenge = MutationChallenge(codeDetails, MutationData(line))
        codeChallenge.getSnippet() shouldBe ""

        elements = Pair("Test", "")
        every { JacocoUtil.getLinesInRange(any(), any(), any()) } returns elements
        every { MutationUtil.getMutatedCode(any(), any()) } returns ""
        codeChallenge = MutationChallenge(codeDetails, MutationData(line))
        codeChallenge.getSnippet() shouldBe "Write or update tests so that they fail on the mutant described below.\n" +
                "Original code snippet\n" +
                "<pre class='prettyprint linenums:108 mt-2'><code class='language-java'>Test</code></pre>Mutated line of code \n" +
                "<br><em>No mutated line available</em><br>The mutated line is built from information provided by PIT and could be syntactically invalid or wrong. Please use along with the description in that case:<br><a href=\"https://pitest.org/quickstart/mutators/#MATH\"target=\"_blank\">Replaced double multiplication with division</a> "

        every { MutationUtil.getMutatedCode(any(), any()) } returns "Mutant"
        codeChallenge = MutationChallenge(codeDetails, MutationData(line))
        codeChallenge.getSnippet() shouldBe "Write or update tests so that they fail on the mutant described below.\n" +
                "Original code snippet\n" +
                "<pre class='prettyprint linenums:108 mt-2'><code class='language-java'>Test</code></pre>Mutated line of code \n" +
                "<pre class='prettyprint linenums:109 mt-2'><code class='language-java'>Mutant</code></pre>The mutated line is built from information provided by PIT and could be syntactically invalid or wrong. Please use along with the description in that case:<br><a href=\"https://pitest.org/quickstart/mutators/#MATH\"target=\"_blank\">Replaced double multiplication with division</a> "

    }

    @Test
    fun equals() {
        challenge.equals(null) shouldBe false

        challenge.equals(listener) shouldBe false

        var details2 = SourceFileDetails(parameters, shortFilePath.replace("gamekins", "game"))
        var challenge2 = MutationChallenge(details2, data)
        (challenge == challenge2) shouldBe false

        details2 = SourceFileDetails(parameters, shortFilePath.replace("Challenge", "Property"))
        challenge2 = MutationChallenge(details2, data)
        (challenge == challenge2) shouldBe false

        val line2 = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>Rational.java</sourceFile><mutatedClass>com.example.Complex</mutatedClass>" +
                "<mutatedMethod>abs</mutatedMethod><methodDescription>()D</methodDescription>" +
                "<lineNumber>109</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>" +
                "<indexes><index>7</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
                "<description>Replaced double multiplication with division</description></mutation>"
        val data2 = MutationData(line2)
        challenge2 = MutationChallenge(details, data2)
        (challenge == challenge2) shouldBe false

        challenge2 = MutationChallenge(details, data)
        (challenge == challenge2) shouldBe true
    }

    @Test
    fun getKillingTest() {
        challenge.getKillingTest() shouldBe ""

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
        challenge2.getKillingTest() shouldBe "com.example.ComplexTest.testAdd()"
    }

    @Test
    fun getName() {
        challenge.getName() shouldBe "Mutation"
    }

    @Test
    fun getParameters() {
        challenge.getParameters() shouldBe parameters
    }

    @Test
    fun getScore() {
        challenge.getScore() shouldBe 4

        val newChallenge = MutationChallenge(details, MutationData(
            line.replace("status='NO_COVERAGE'", "status='SURVIVED'"))
        )
        newChallenge.getScore() shouldBe 5
    }

    @Test
    fun isSolvable() {
        val newParameters = Constants.Parameters(branch = "stale")
        challenge.isSolvable(newParameters, run, listener) shouldBe true

        newParameters.branch = branch
        every { MutationUtil.executePIT(any(), any(), any()) } returns false
        challenge.isSolvable(newParameters, run, listener) shouldBe false

        every { MutationUtil.executePIT(any(), any(), any()) } returns true
        challenge.isSolvable(newParameters, run, listener) shouldBe true

        newParameters.workspace = testProjectPath
        challenge.isSolvable(newParameters, run, listener) shouldBe true

        every { MutationUtil.getMutant(any(), any()) } returns null
        challenge.isSolvable(newParameters, run, listener) shouldBe false

        val mutant = MutationData(line.replace("detected='false'", "detected='true'"))
        every { MutationUtil.getMutant(any(), any()) } returns mutant
        challenge.isSolvable(newParameters, run, listener) shouldBe false
    }

    @Test
    fun isSolved() {
        every { MutationUtil.executePIT(any(), any(), any()) } returns false
        challenge.isSolved(parameters, run, listener) shouldBe false

        every { MutationUtil.executePIT(any(), any(), any()) } returns true
        every { MutationUtil.getMutant(any(), any()) } returns null
        challenge.isSolved(parameters, run, listener) shouldBe false

        var mutant = MutationData(line.replace("detected='false'", "detected='true'"))
        every { MutationUtil.getMutant(any(), any()) } returns mutant
        challenge.isSolved(parameters, run, listener) shouldBe false

        mutant = MutationData(line.replace("status='NO_COVERAGE'", "status='KILLED'"))
        every { MutationUtil.getMutant(any(), any()) } returns mutant
        challenge.isSolved(parameters, run, listener) shouldBe false

        mutant = MutationData(line
            .replace("detected='false'", "detected='true'")
            .replace("status='NO_COVERAGE'", "status='KILLED'"))
        every { MutationUtil.getMutant(any(), any()) } returns mutant
        challenge.isSolved(parameters, run, listener) shouldBe true
    }

    @Test
    fun printToXML() {
        var printOutput = "<MutationChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"Challenge\" " +
                "detected=\"false\" status=\"NO_COVERAGE\" numberOfTestsRun=\"0\" mutator=\"MATH\" " +
                "killingTest=\"\" description=\"Replaced double multiplication with division\"/>"
        challenge.printToXML("", "") shouldBe printOutput

        printOutput = "    <MutationChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"Challenge\" " +
                "detected=\"false\" status=\"NO_COVERAGE\" numberOfTestsRun=\"0\" mutator=\"MATH\" " +
                "killingTest=\"\" description=\"Replaced double multiplication with division\"/>"
        challenge.printToXML("", "    ") shouldBe printOutput

        printOutput = "    <MutationChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"Challenge\" " +
                "detected=\"false\" status=\"NO_COVERAGE\" numberOfTestsRun=\"0\" mutator=\"MATH\" " +
                "killingTest=\"\" description=\"Replaced double multiplication with division\" reason=\"test\"/>"
        challenge.printToXML("test", "    ") shouldBe printOutput
    }

    @Test
    fun testToString() {
        var stringOutput = "Write a test to kill the mutant at line <b>109</b> of method <b>abs()</b> in class " +
                "<b>Challenge</b> in package <b>org.gamekins.challenge</b> (created for branch master)"
        challenge.toString() shouldBe stringOutput

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
        challengeConstructor.toString() shouldBe stringOutput
    }
}
