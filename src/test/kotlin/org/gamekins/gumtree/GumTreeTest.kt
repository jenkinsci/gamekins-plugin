package org.gamekins.gumtree

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import hudson.FilePath
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.mockk.*
import org.gamekins.gumTree.GumTree
import org.gamekins.gumTree.JavaParser
import org.gamekins.test.TestUtils
import org.gamekins.util.Constants
import org.gamekins.util.MutationUtil.MutationData
import java.io.File

class GumTreeTest: FeatureSpec ({
    val line1 = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
            "<sourceFile>TestClass.java</sourceFile><mutatedClass>org.example.TestClass</mutatedClass>" +
            "<mutatedMethod>doSomething</mutatedMethod>" +
            "<methodDescription>(Lorg/example/anotherPackage/CSVParser;)V</methodDescription>" +
            "<lineNumber>24</lineNumber>" +
            "<mutator>org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator</mutator>" +
            "<indexes><index>4</index></indexes><blocks><block>0</block></blocks><killingTest/>" +
            "<description>removed call to java/lang/Object::notify</description></mutation>"
    val line2 = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
            "<sourceFile>TestClass.java</sourceFile><mutatedClass>org.example.TestClass</mutatedClass>" +
            "<mutatedMethod>doSomethingBig</mutatedMethod>" +
            "<methodDescription>()Ljava/math/BigInteger;</methodDescription><lineNumber>33</lineNumber>" +
            "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
            "<indexes><index>24</index></indexes><blocks><block>5</block></blocks><killingTest/>" +
            "<description>replaced return value with null for org/example/TestClass::doSomethingBig</description>" +
            "</mutation>"
    val line3 = "<mutation detected='true' status='KILLED' numberOfTestsRun='1'>" +
            "<sourceFile>TestClass.java</sourceFile><mutatedClass>org.example.TestClass</mutatedClass>" +
            "<mutatedMethod>getList</mutatedMethod>" +
            "<methodDescription>(Ljava/lang/String;[I)[Ljava/lang/String;</methodDescription>" +
            "<lineNumber>19</lineNumber>" +
            "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
            "<indexes><index>47</index></indexes><blocks><block>7</block></blocks>" +
            "<killingTest>org.example.TestClassTest.[engine:junit-jupiter]/[class:org.example.TestClassTest]/" +
            "[method:testTestMethod()]</killingTest>" +
            "<description>replaced return value with null for org/example/TestClass::getList</description></mutation>"
    val line4 = "<mutation detected='false' status='NO_COVERAGE' numberOfTestsRun='0'>" +
            "<sourceFile>TestClass.java</sourceFile><mutatedClass>org.example.TestClass</mutatedClass>" +
            "<mutatedMethod>doSomethingElse</mutatedMethod>" +
            "<methodDescription>()Lorg/example/anotherAnotherPackage/Additive;</methodDescription>" +
            "<lineNumber>43</lineNumber>" +
            "<mutator>org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator</mutator>" +
            "<indexes><index>25</index></indexes><blocks><block>3</block></blocks><killingTest/>" +
            "<description>replaced return value with null for org/example/TestClass::doSomethingElse</description>" +
            "</mutation>"

    lateinit var sourceCompilationUnit: CompilationUnit
    lateinit var destinationCompilationUnit: CompilationUnit

    val path = FilePath(null, "src/test/resources")
    val parameters = Constants.Parameters()
    val branch = "master"
    lateinit var root : String
    lateinit var testProjectPath : FilePath

    beforeContainer {
        StaticJavaParser.setConfiguration(ParserConfiguration().setAttributeComments(false))
        val combinedSolver = CombinedTypeSolver()
        //ReflectionTypeSolver is used to get the fully qualified name of standard java classes e.g. String
        combinedSolver.add(ReflectionTypeSolver())
        val symbolSolver = JavaSymbolSolver(combinedSolver)
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver)

        sourceCompilationUnit = StaticJavaParser.parse(File("src/test/resources/TestClass.java"))
        destinationCompilationUnit = StaticJavaParser.parse(File("src/test/resources/TestClass2.java"))

        parameters.branch = branch
        parameters.workspace = path
        mockkObject(JavaParser.Companion)
        every { JavaParser.parse(any(), any(), any()) } returns destinationCompilationUnit
    }


    /*
    beforeSpec {
        val rootDirectory = javaClass.classLoader.getResource("gumtreeTestResources.zip")
        rootDirectory shouldNotBe null
        root = rootDirectory.file.removeSuffix(".zip")
        root shouldEndWith "gumtreeTestResources"
        TestUtils.unzip("$root.zip", root)
        //testProjectPath = FilePath(null, root)
    }

     */

    afterSpec {
        unmockkAll()
        //File(root).deleteRecursively()
    }

    feature("testGumTree") {
        var mutationData = MutationData(line1)
        mutationData.compilationUnit = sourceCompilationUnit
        scenario("MoveMethod")
        {
            val updatedMutationData = GumTree().findMapping(mutationData, parameters)
            updatedMutationData shouldNotBe null
            assertSoftly {
                updatedMutationData!!.lineNumber shouldBe 23
                updatedMutationData.methodDescription shouldBe "(Lorg/example/anotherPackage/CSVParser;)V"
                updatedMutationData.mutatedClass shouldBe "org.example.TestClass"
            }
        }

        mutationData = MutationData(line2)
        mutationData.compilationUnit = sourceCompilationUnit
        scenario("MoveMethodToInternalClassAndAddParameter")
        {
            val updatedMutationData = GumTree().findMapping(mutationData, parameters)
            updatedMutationData shouldNotBe null
            assertSoftly {
                updatedMutationData!!.lineNumber shouldBe 45
                updatedMutationData.methodDescription shouldBe "(D)Ljava/math/BigInteger;"
                updatedMutationData.mutatedClass shouldBe "org.example.TestClass\$InternalClass"
            }
        }

        mutationData = MutationData(line3)
        mutationData.compilationUnit = sourceCompilationUnit
        scenario("RenameMethod")
        {
            val updatedMutationData = GumTree().findMapping(mutationData, parameters)
            updatedMutationData shouldNotBe null
            assertSoftly {
                updatedMutationData!!.lineNumber shouldBe 19
                updatedMutationData.methodDescription shouldBe "(Ljava/lang/String;[I)[Ljava/lang/String;"
                updatedMutationData.mutatedClass shouldBe "org.example.TestClass"
                updatedMutationData.mutatedMethod shouldBe "getName"
            }
        }

        mutationData = MutationData(line4)
        mutationData.compilationUnit = sourceCompilationUnit
        scenario("RetrieveMutationDataThrewPITReport")
        {
            val updatedMutationData = GumTree().findMapping(mutationData, parameters)
            updatedMutationData shouldNotBe null
            assertSoftly {
                updatedMutationData!!.lineNumber shouldBe 34
                updatedMutationData.methodDescription shouldBe "()Lorg/example/anotherAnotherPackage/Additive;"
                updatedMutationData.mutatedClass shouldBe "org.example.TestClass"
                updatedMutationData.mutatedMethod shouldBe "doSomethingElse"
            }
        }
    }
})