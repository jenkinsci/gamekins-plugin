package org.gamekins.util

import hudson.FilePath
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.unmockkAll
import org.gamekins.file.SourceFileDetails
import org.gamekins.test.TestUtils
import org.gamekins.util.MutationUtil.MutationData
import java.io.File

class MutationUtilTest : AnnotationSpec() {

    private val parameters = Constants.Parameters()
    private val line = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
            "<sourceFile>Complex.java</sourceFile><mutatedClass>com.example.Complex</mutatedClass>" +
            "<mutatedMethod>abs</mutatedMethod><methodDescription>()D</methodDescription>" +
            "<lineNumber>109</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator>" +
            "<indexes><index>7</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
            "<description>Replaced double multiplication with division</description></mutation>"
    private lateinit var data : MutationData
    private lateinit var root : String
    private lateinit var testProjectPath : FilePath
    private val className = "Complex"
    private val packageName = "com.example"
    private val sourceFile = mockkClass(SourceFileDetails::class)

    @BeforeEach
    fun init() {
        data = MutationData(line)
        every { sourceFile.fileName } returns className
        every { sourceFile.packageName } returns packageName
    }

    @BeforeAll
    fun initAll() {
        val rootDirectory = javaClass.classLoader.getResource("test-project.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory.file.removeSuffix(".zip")
        root shouldEndWith "test-project"
        TestUtils.unzip("$root.zip", root)
        testProjectPath = FilePath(null, root)
        parameters.workspace = testProjectPath
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
        File(root).deleteRecursively()
    }

    @Test
    @Ignore
    fun executePIT() {
        //PIT does not work, no idea why
        MutationUtil.executePIT(sourceFile, parameters) shouldBe true
    }

    @Test
    fun getMutant() {
        parameters.workspace = FilePath(null, "")
        MutationUtil.getMutant(data, parameters) shouldBe null

        parameters.workspace = testProjectPath
        MutationUtil.getMutant(data, parameters) shouldBe data
    }

    /**
     * Use an existing mutant from a project (for the mutator) and some other line from the same project
     * (for the different cases)(as far as available).
     */
    @Test
    fun getMutatedCode() {
        //CONDITIONALS_BOUNDARY
        var mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>Complex.java</sourceFile><mutatedClass>com.example.Complex</mutatedClass>" +
                "<mutatedMethod>toString</mutatedMethod><methodDescription>(I)Ljava/lang/String;</methodDescription>" +
                "<lineNumber>222</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator</mutator>" +
                "<indexes><index>22</index></indexes><blocks><block>3</block></blocks><killingTest/>" +
                "<description>changed conditional boundary</description></mutation>"
        MutationUtil.getMutatedCode("    if (imag &lt; 0.0) {", MutationData(mutationLine)) shouldBe
                "    if (imag &lt;= 0.0) {"
        MutationUtil.getMutatedCode("    if (decimal.scale() &lt;= 0) {", MutationData(mutationLine)) shouldBe
                "    if (decimal.scale() &lt; 0) {"
        MutationUtil.getMutatedCode("    while (input &gt; 0) {", MutationData(mutationLine)) shouldBe
                "    while (input &gt;= 0) {"
        MutationUtil.getMutatedCode("    while (input &gt;= 0) {", MutationData(mutationLine)) shouldBe
                "    while (input &gt; 0) {"
        MutationUtil.getMutatedCode("    while (input == 0) {", MutationData(mutationLine)) shouldBe ""

        //INCREMENTS
        mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>TestClass.java</sourceFile><mutatedClass>com.example.TestClass</mutatedClass>" +
                "<mutatedMethod>countFoos</mutatedMethod><methodDescription>(I)I</methodDescription>" +
                "<lineNumber>21</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.IncrementsMutator</mutator><indexes>" +
                "<index>11</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
                "<description>Changed increment from 10 to -10</description></mutation>"
        MutationUtil.getMutatedCode("    for (int i = 0; i &lt; 42; i++) {", MutationData(mutationLine)) shouldBe
                "    for (int i = 0; i &lt; 42; i--) {"
        MutationUtil.getMutatedCode("    for (int i = 0; i &lt; 42; i--) {", MutationData(mutationLine)) shouldBe
                "    for (int i = 0; i &lt; 42; i++) {"
        MutationUtil.getMutatedCode("    arbitraryInt += 10;", MutationData(mutationLine)) shouldBe
                "    arbitraryInt -= 10;"
        MutationUtil.getMutatedCode("    arbitraryInt -= 10;", MutationData(mutationLine)) shouldBe
                "    arbitraryInt += 10;"
        MutationUtil.getMutatedCode("    arbitraryInt = 10;", MutationData(mutationLine)) shouldBe ""

        //INVERT_NEGS
        mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>Complex.java</sourceFile><mutatedClass>com.example.Complex</mutatedClass>" +
                "<mutatedMethod>toString</mutatedMethod><methodDescription>(I)Ljava/lang/String;</methodDescription>" +
                "<lineNumber>224</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.InvertNegsMutator</mutator><indexes>" +
                "<index>34</index></indexes><blocks><block>5</block></blocks><killingTest/>" +
                "<description>removed negation</description></mutation>"
        MutationUtil.getMutatedCode("      temp.append(trim(-imag, doubleLength));", MutationData(mutationLine)) shouldBe
                "      temp.append(trim(imag, doubleLength));"

        //MATH
        mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>Complex.java</sourceFile><mutatedClass>com.example.Complex</mutatedClass>" +
                "<mutatedMethod>trim</mutatedMethod><methodDescription>(DI)D</methodDescription>" +
                "<lineNumber>243</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.MathMutator</mutator><indexes>" +
                "<index>12</index></indexes><blocks><block>1</block></blocks><killingTest/>" +
                "<description>Replaced double multiplication with division</description></mutation>"
        MutationUtil.getMutatedCode("    return Math.sqrt(real * real + imag * imag);", MutationData(mutationLine)) shouldBe
                "    return Math.sqrt(real * real - imag * imag);"
        MutationUtil.getMutatedCode("    int d = b - c;", MutationData(mutationLine)) shouldBe
                "    int d = b + c;"
        MutationUtil.getMutatedCode("    return c * b / d;", MutationData(mutationLine)) shouldBe
                "    return c / b / d;"
        MutationUtil.getMutatedCode("    double dArg = d / 2;", MutationData(mutationLine)) shouldBe
                "    double dArg = d * 2;"
        MutationUtil.getMutatedCode("      if (input % 3 == 0) {", MutationData(mutationLine)) shouldBe
                "      if (input * 3 == 0) {"
        MutationUtil.getMutatedCode("    return (num.equals(b.num) &amp;&amp; den.equals(b.den));", MutationData(mutationLine)) shouldBe
                "    return (num.equals(b.num) || den.equals(b.den));"
        MutationUtil.getMutatedCode("    if (a == ZERO || b == ZERO) {", MutationData(mutationLine)) shouldBe
                "    if (a == ZERO &amp;&amp; b == ZERO) {"
        MutationUtil.getMutatedCode("        return currentSize &lt;= 4 ? 8 : (currentSize &lt;&lt; 1);", MutationData(mutationLine)) shouldBe
                "        return currentSize &lt;= 4 ? 8 : (currentSize &gt;&gt; 1);"
        MutationUtil.getMutatedCode("            int mid = lo &gt;&gt;&gt; 1;", MutationData(mutationLine)) shouldBe
                "            int mid = lo &lt;&lt; 1;"
        MutationUtil.getMutatedCode("        return currentSize &lt;= 4 ? 8 : (currentSize &gt;&gt; 1);", MutationData(mutationLine)) shouldBe
                "        return currentSize &lt;= 4 ? 8 : (currentSize &lt;&lt; 1);"
        MutationUtil.getMutatedCode("        return currentSize &lt;= 4", MutationData(mutationLine)) shouldBe ""

        //NEGATE_CONDITIONALS
        mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>Rational.java</sourceFile><mutatedClass>com.example.Rational</mutatedClass>" +
                "<mutatedMethod>abs</mutatedMethod><methodDescription>()Lcom/example/Rational;</methodDescription>" +
                "<lineNumber>242</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator</mutator>" +
                "<indexes><index>6</index></indexes><blocks><block>1</block></blocks><killingTest/>" +
                "<description>negated conditional</description></mutation>"
        MutationUtil.getMutatedCode("    if (imag &lt; 0.0) {", MutationData(mutationLine)) shouldBe
                "    if (imag &gt;= 0.0) {"
        MutationUtil.getMutatedCode("    if (decimal.scale() &lt;= 0) {", MutationData(mutationLine)) shouldBe
                "    if (decimal.scale() &gt; 0) {"
        MutationUtil.getMutatedCode("    while (input &gt; 0) {", MutationData(mutationLine)) shouldBe
                "    while (input &lt;= 0) {"
        MutationUtil.getMutatedCode("    while (input &gt;= 0) {", MutationData(mutationLine)) shouldBe
                "    while (input &lt; 0) {"
        MutationUtil.getMutatedCode("    if (a == ONE) {", MutationData(mutationLine)) shouldBe
                "    if (a != ONE) {"
        MutationUtil.getMutatedCode("    if (y.getClass() != this.getClass()) {", MutationData(mutationLine)) shouldBe
                "    if (y.getClass() == this.getClass()) {"
        MutationUtil.getMutatedCode("    if (y.getClass()) {", MutationData(mutationLine)) shouldBe ""

        //RETURN_VALS
        //Skipped because not part of default mutators

        //VOID_METHOD_CALLS
        mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>App.java</sourceFile><mutatedClass>com.example.App</mutatedClass>" +
                "<mutatedMethod>main</mutatedMethod><methodDescription>([Ljava/lang/String;)V</methodDescription>" +
                "<lineNumber>11</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
                "<description>removed call to java/io/PrintStream::println</description></mutation>"
        MutationUtil.getMutatedCode("y.getClass()", MutationData(mutationLine)) shouldBe "Line removed"

        //EMPTY_RETURNS
        mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>Complex.java</sourceFile><mutatedClass>com.example.Complex</mutatedClass>" +
                "<mutatedMethod>toString</mutatedMethod><methodDescription>()Ljava/lang/String;</methodDescription>" +
                "<lineNumber>211</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator</mutator>" +
                "<indexes><index>6</index></indexes><blocks><block>1</block></blocks><killingTest/>" +
                "<description>replaced return value with &quot;&quot; for com/example/Complex::toString</description></mutation>"
        MutationUtil.getMutatedCode("    return toString(8);", MutationData(mutationLine)) shouldBe
                "    return &quot;&quot;;"
        mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>Main.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.Main</mutatedClass>" +
                "<mutatedMethod>performSearch</mutatedMethod><methodDescription>()Ljava/util/Map;</methodDescription>" +
                "<lineNumber>402</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator</mutator>" +
                "<indexes><index>63</index></indexes><blocks><block>12</block></blocks><killingTest/>" +
                "<description>replaced return value with Collections.emptyMap for de/uni_passau/fim/se2/Main::performSearch</description></mutation>"
        MutationUtil.getMutatedCode("        return results;", MutationData(mutationLine)) shouldBe
                "        return Collections.emptyMap();"
        mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>RandomWalk.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.metaheuristics.algorithms.RandomWalk</mutatedClass>" +
                "<mutatedMethod>randomWalk</mutatedMethod><methodDescription>()Ljava/util/stream/Stream;</methodDescription>" +
                "<lineNumber>134</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator</mutator>" +
                "<indexes><index>22</index></indexes><blocks><block>3</block></blocks><killingTest/>" +
                "<description>replaced return value with Stream.empty for de/uni_passau/fim/se2/metaheuristics/algorithms/RandomWalk::randomWalk</description></mutation>"
        MutationUtil.getMutatedCode("        return Stream.iterate(start, searchCanContinue, this::pickRandomNeighbor);", MutationData(mutationLine)) shouldBe
                "        return Stream.empty();"
        mutationLine = "<mutation detected='false' status='SURVIVED' numberOfTestsRun='1'>" +
                "<sourceFile>SlidingPair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.test_prioritization.util.SlidingPair</mutatedClass>" +
                "<mutatedMethod>characteristics</mutatedMethod><methodDescription>()I</methodDescription>" +
                "<lineNumber>83</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator</mutator>" +
                "<indexes><index>4</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
                "<description>replaced int return with 0 for de/uni_passau/fim/se2/test_prioritization/util/SlidingPair::characteristics</description></mutation>"
        MutationUtil.getMutatedCode("        return characteristics;", MutationData(mutationLine)) shouldBe
                "        return 0;"

        //FALSE_RETURNS
        mutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>SlidingPair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.test_prioritization.util.SlidingPair</mutatedClass>" +
                "<mutatedMethod>tryAdvance</mutatedMethod><methodDescription>(Ljava/util/function/Consumer;)Z</methodDescription>" +
                "<lineNumber>68</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanFalseReturnValsMutator</mutator>" +
                "<indexes><index>67</index></indexes><blocks><block>13</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.test_prioritization.util.SlidingPairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.test_prioritization.util.SlidingPairTest]/[method:testAdvanceTwoElements()]</killingTest>" +
                "<description>replaced boolean return with false for de/uni_passau/fim/se2/test_prioritization/util/SlidingPair::tryAdvance</description></mutation>"
        MutationUtil.getMutatedCode("        return true;", MutationData(mutationLine)) shouldBe
                "        return false;"

        //TRUE_RETURNS
        mutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>SlidingPair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.test_prioritization.util.SlidingPair</mutatedClass>" +
                "<mutatedMethod>tryAdvance</mutatedMethod><methodDescription>(Ljava/util/function/Consumer;)Z</methodDescription>" +
                "<lineNumber>54</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanTrueReturnValsMutator</mutator>" +
                "<indexes><index>29</index></indexes><blocks><block>5</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.test_prioritization.util.SlidingPairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.test_prioritization.util.SlidingPairTest]/[method:testAdvanceEmpty()]</killingTest>" +
                "<description>replaced boolean return with true for de/uni_passau/fim/se2/test_prioritization/util/SlidingPair::tryAdvance</description></mutation>"
        MutationUtil.getMutatedCode("                return false;", MutationData(mutationLine)) shouldBe
                "                return true;"

        //NULL_RETURNS
        mutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        MutationUtil.getMutatedCode("        return fst;", MutationData(mutationLine)) shouldBe
                "        return null;"
        MutationUtil.getMutatedCode("(argument) -> Object();", MutationData(mutationLine)) shouldBe
                "(argument) -> null;"

        //PRIMITIVE_RETURNS
        mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>APLC.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.test_prioritization.fitness_functions.APLC\$1</mutatedClass>" +
                "<mutatedMethod>applyAsDouble</mutatedMethod><methodDescription>(Lde/uni_passau/fim/se2/test_prioritization/configurations/Ordering;)D</methodDescription>" +
                "<lineNumber>222</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.PrimitiveReturnsMutator</mutator>" +
                "<indexes><index>9</index></indexes><blocks><block>1</block></blocks><killingTest/>" +
                "<description>replaced double return with 0.0d for de/uni_passau/fim/se2/test_prioritization/fitness_functions/APLC\$1::applyAsDouble</description></mutation>"
        MutationUtil.getMutatedCode("                return 1 - APLC.this.applyAsDouble(ordering);", MutationData(mutationLine)) shouldBe
                "                return 0;"

        //UNKNOWN
        mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
                "<sourceFile>APLC.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.test_prioritization.fitness_functions.APLC\$1</mutatedClass>" +
                "<mutatedMethod>applyAsDouble</mutatedMethod><methodDescription>(Lde/uni_passau/fim/se2/test_prioritization/configurations/Ordering;)D</methodDescription>" +
                "<lineNumber>222</lineNumber><mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.UnknownMutator</mutator>" +
                "<indexes><index>9</index></indexes><blocks><block>1</block></blocks><killingTest/>" +
                "<description>replaced double return with 0.0d for de/uni_passau/fim/se2/test_prioritization/fitness_functions/APLC\$1::applyAsDouble</description></mutation>"
        MutationUtil.getMutatedCode("                return 1 - APLC.this.applyAsDouble(ordering);", MutationData(mutationLine)) shouldBe
                ""
    }

    @Test
    fun testMutationData() {
        var mutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        var data = MutationData(mutationLine)
        data.detected shouldBe true
        data.status shouldBe MutationUtil.MutationStatus.KILLED
        data.numberOfTestsRun shouldBe 1
        data.sourceFile shouldBe "Pair.java"
        data.mutatedClass shouldBe "de.uni_passau.fim.se2.util.Pair"
        data.mutatedMethod shouldBe "getFst"
        data.methodDescription shouldBe "()Ljava/lang/Object;"
        data.lineNumber shouldBe 42
        data.mutator shouldBe MutationUtil.Mutator.NULL_RETURNS
        data.killingTest shouldBe "de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]"
        data.description shouldBe "replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst"

        mutationLine = "<mutation detected='false' status='SURVIVED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        data = MutationData(mutationLine)
        data.detected shouldBe false
        data.status shouldBe MutationUtil.MutationStatus.SURVIVED
        data.killingTest shouldBe ""

        mutationLine = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        data = MutationData(mutationLine)
        data.status shouldBe MutationUtil.MutationStatus.NO_COVERAGE

        mutationLine = "<mutation detected='false' status='STATUS' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        data = MutationData(mutationLine)
        data.status shouldBe MutationUtil.MutationStatus.KILLED
    }

    @Test
    fun testMutationDataEquals() {
        val mutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        val data = MutationData(mutationLine)
        data.equals(null) shouldBe false

        data.equals(mutationLine) shouldBe false

        var otherData = MutationData(mutationLine)
        (data == otherData) shouldBe true

        //sourceFile
        var otherMutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pairs.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        otherData = MutationData(otherMutationLine)
        (data == otherData) shouldBe false

        //mutatedClass
        otherMutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pairs</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        otherData = MutationData(otherMutationLine)
        (data == otherData) shouldBe false

        //mutatedMethod
        otherMutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFirst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        otherData = MutationData(otherMutationLine)
        (data == otherData) shouldBe false

        //methodDescription
        otherMutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/math/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        otherData = MutationData(otherMutationLine)
        (data == otherData) shouldBe false

        //lineNumber
        otherMutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>21</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        otherData = MutationData(otherMutationLine)
        (data == otherData) shouldBe false

        //mutator
        otherMutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.PrimitiveReturnsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        otherData = MutationData(otherMutationLine)
        (data == otherData) shouldBe false

        //description
        otherMutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with 0 for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        otherData = MutationData(otherMutationLine)
        (data == otherData) shouldBe false

        //detected
        otherMutationLine = "<mutation detected='false' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        otherData = MutationData(otherMutationLine)
        (data == otherData) shouldBe true

        //status
        otherMutationLine = "<mutation detected='true' status='SURVIVED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        otherData = MutationData(otherMutationLine)
        (data == otherData) shouldBe true

        //numberOfTestsRun
        otherMutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='0'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        otherData = MutationData(otherMutationLine)
        (data == otherData) shouldBe true

        //killingTest
        otherMutationLine = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
                "<sourceFile>Pair.java</sourceFile><mutatedClass>de.uni_passau.fim.se2.util.Pair</mutatedClass>" +
                "<mutatedMethod>getFst</mutatedMethod><methodDescription>()Ljava/lang/Object;</methodDescription>" +
                "<lineNumber>42</lineNumber>" +
                "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
                "<indexes><index>5</index></indexes><blocks><block>0</block></blocks>" +
                "<killingTest>de.uni_passau.fim.se2.util.PairsTest.[engine:junit-jupiter]/[class:de.uni_passau.fim.se2.util.PairTest]/[method:testPairNull()]</killingTest>" +
                "<description>replaced return value with null for de/uni_passau/fim/se2/util/Pair::getFst</description></mutation>"
        otherData = MutationData(otherMutationLine)
        (data == otherData) shouldBe true
    }
}
