package org.gamekins.challenge.quest

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter
import hudson.FilePath
import hudson.model.TaskListener
import hudson.model.User
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.gamekins.GameUserProperty
import org.gamekins.challenge.*
import org.gamekins.file.FileDetails
import org.gamekins.file.SourceFileDetails
import org.gamekins.gumTree.JavaParser
import org.gamekins.util.Constants
import org.gamekins.util.GitUtil
import org.gamekins.util.JacocoUtil
import org.gamekins.util.Pair
import org.gamekins.util.SmellUtil
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.random.Random

class QuestFactoryTest : FeatureSpec({

    val user = mockkClass(User::class)
    val property = mockkClass(GameUserProperty::class)
    val parameters = Constants.Parameters()
    val listener: TaskListener = TaskListener.NULL
    lateinit var classes: ArrayList<FileDetails>
    val numberOfQuests = 9

    beforeContainer {
        mockkStatic(QuestFactory::class)
        mockkObject(Random)
        mockkStatic(JacocoUtil::class)
        mockkStatic(ChallengeFactory::class)
        classes = arrayListOf()

        parameters.projectName = "project"
        every { property.getCurrentQuests(any()) } returns CopyOnWriteArrayList()
        every { property.getGitNames() } returns CopyOnWriteArraySet()
        every { user.fullName } returns "User"
        every { user.id } returns "user"
        every { user.getProperty(hudson.tasks.Mailer.UserProperty::class.java) } returns null
        every { user.getProperty(GameUserProperty::class.java) } returns property

        val compilationUnit = mockkClass(CompilationUnit::class)
        mockkStatic(JavaParser::class)
        mockkStatic(LexicalPreservingPrinter::class)
        every { JavaParser.parse(any(), any(), any()) } returns compilationUnit
        every { LexicalPreservingPrinter.setup(any()) } returns null
        every { LexicalPreservingPrinter.print(any()) } returns ""
    }

    feature("generateNewQuests") {
        scenario("Quest limit reached (0)")
        {
            QuestFactory.generateNewQuests(user, property, parameters, listener, classes, 0) shouldBe 0
        }

        every { property.newQuest(any(), any()) } returns Unit
        scenario("User has not modified any classes")
        {
            QuestFactory.generateNewQuests(user, property, parameters, listener, classes, 1) shouldBe 0
        }

        val detail = mockkClass(FileDetails::class)
        val gameUser = GitUtil.GameUser(user)
        every { detail.changedByUsers } returns hashSetOf(gameUser)
        classes.add(detail)
        every { QuestFactory.generateQuest(any(), any(), any(), any(), any()) } returns Quest(Constants.NO_QUEST, arrayListOf())
        scenario("Could not generate new Quests")
        {
            QuestFactory.generateNewQuests(user, property, parameters, listener, classes, 1) shouldBe 0
        }

        every { QuestFactory.generateQuest(any(), any(), any(), any(), any()) } returns Quest("Some quest", arrayListOf())
        scenario("New Quest successfully generated")
        {
            QuestFactory.generateNewQuests(user, property, parameters, listener, classes, 1) shouldBe 1
        }
    }

    feature("generateQuest") {
        every { QuestFactory.generateLinesQuest(any(), any(), any(), any(), any()) } returns null
        every { Random.nextInt(numberOfQuests) } returns 0
        every { property.getRejectedQuests(any()) } returns CopyOnWriteArrayList()
        scenario("No Quest was generated")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe Quest(Constants.NO_QUEST, arrayListOf())
        }

        val quest = mockkClass(Quest::class)
        every { QuestFactory.generateLinesQuest(any(), any(), any(), any(), any()) } returns quest
        scenario("LinesQuest selected")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest
        }

        val pair = Pair(quest, "")
        every { property.getRejectedQuests(any()) } returns CopyOnWriteArrayList(arrayListOf(pair))
        scenario("Generated Quest was already rejected")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe Quest(Constants.NO_QUEST, arrayListOf())
        }

        every { property.getRejectedQuests(any()) } returns CopyOnWriteArrayList()
        every { QuestFactory.generateMethodsQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 1
        scenario("MethodsQuest selected")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest
        }

        every { QuestFactory.generatePackageQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 2
        scenario("PackageQuest selected")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest
        }

        every { QuestFactory.generateClassQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 3
        scenario("ClassQuest selected")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest
        }

        every { QuestFactory.generateExpandingQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 4
        scenario("ExpandingQuest selected")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest
        }

        every { QuestFactory.generateDecreasingQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 5
        scenario("DecreasingQuest selected")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest
        }

        every { QuestFactory.generateTestQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 6
        scenario("TestQuest selected")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest
        }

        every { QuestFactory.generateMutationQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 7
        scenario("MutationQuest selected")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest
        }

        every { QuestFactory.generateSmellQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 8
        scenario("SmellQuest selected")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest
        }

        every { Random.nextInt(numberOfQuests) } returns 9
        scenario("NoQuest selected")
        {
            QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe Quest(Constants.NO_QUEST, arrayListOf())
        }
    }

    feature("generateClassQuest") {
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList()
        scenario("No Files exist")
        {
            QuestFactory.generateClassQuest(user, property, parameters, listener, classes) shouldBe null
        }

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        scenario("Existing file is sufficiently covered already")
        {
            QuestFactory.generateClassQuest(user, property, parameters, listener, classes) shouldBe null
        }

        every { sourceDetail.coverage } returns 0.8
        val challenge = mockkClass(ClassCoverageChallenge::class)
        val classDetails = mockkClass(SourceFileDetails::class)
        every { sourceDetail == any() } returns true
        every { challenge.details } returns classDetails
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList(arrayListOf(Pair(challenge, "")))
        scenario("Existing file has previously rejected ClassCoverageChallenge")
        {
            QuestFactory.generateClassQuest(user, property, parameters, listener, classes) shouldBe null
        }

        every { sourceDetail == any() } returns false
        every { sourceDetail.jacocoSourceFile } returns File("")
        every { sourceDetail.parameters } returns Constants.Parameters()
        every { sourceDetail.fileName } returns "file"
        every { JacocoUtil.generateDocument(any()) } returns mockkClass(Document::class)
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 1
        scenario("Successful Generation")
        {
            QuestFactory.generateClassQuest(user, property, parameters, listener, classes)!!.name shouldBe "Incremental - Solve three Class Coverage Challenges"
        }
    }

    feature("generateDecreasingQuest") {
        scenario("No Files exist")
        {
            QuestFactory.generateDecreasingQuest(user, property, parameters, listener, classes) shouldBe null
        }

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        scenario("Existing file is sufficiently covered already")
        {
            QuestFactory.generateDecreasingQuest(user, property, parameters, listener, classes) shouldBe null
        }

        every { sourceDetail.coverage } returns 0.8
        every { sourceDetail.jacocoSourceFile } returns File("")
        every { sourceDetail.parameters } returns Constants.Parameters()
        every { sourceDetail.fileName } returns "file"
        every { sourceDetail.packageName } returns "org.example"
        every { sourceDetail.fileExtension } returns "java"
        every { JacocoUtil.generateDocument(any()) } returns mockkClass(Document::class)
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 1
        val line = mockkClass(Element::class)
        every { line.attr("class") } returns "fc"
        every { line.attr("id") } returns "L7"
        every { line.attr("title") } returns ""
        every { line.text() } returns "content"
        every { JacocoUtil.chooseRandomLine(any(), any()) } returns line
        val method = JacocoUtil.CoverageMethod("method", 10, 5, "7")
        every { JacocoUtil.chooseRandomMethod(any(), any()) } returns method
        scenario("Successful Generation")
        {
            QuestFactory.generateDecreasingQuest(user, property, parameters, listener, classes)!!.name shouldBe "Decreasing - Solve a Class, Method and Line Coverage Challenge"
        }
    }

    feature("generateExpandingQuest") {
        scenario("No Files exist")
        {
            QuestFactory.generateExpandingQuest(user, property, parameters, listener, classes) shouldBe null
        }

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        scenario("Existing file is sufficiently covered already")
        {
            QuestFactory.generateExpandingQuest(user, property, parameters, listener, classes) shouldBe null
        }

        every { sourceDetail.coverage } returns 0.8
        every { sourceDetail.jacocoSourceFile } returns File("")
        every { sourceDetail.parameters } returns Constants.Parameters()
        every { sourceDetail.fileName } returns "file"
        every { sourceDetail.packageName } returns "org.example"
        every { sourceDetail.fileExtension } returns "java"
        every { JacocoUtil.generateDocument(any()) } returns mockkClass(Document::class)
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 1
        val line = mockkClass(Element::class)
        every { line.attr("class") } returns "fc"
        every { line.attr("id") } returns "L7"
        every { line.attr("title") } returns ""
        every { line.text() } returns "content"
        every { JacocoUtil.chooseRandomLine(any(), any()) } returns line
        val method = JacocoUtil.CoverageMethod("method", 10, 5, "7")
        every { JacocoUtil.chooseRandomMethod(any(), any()) } returns method
        scenario("Successful Generation")
        {
            QuestFactory.generateExpandingQuest(user, property, parameters, listener, classes)!!.name shouldBe "Expanding - Solve a Line, Method and Class Coverage Challenge"
        }
    }

    feature("generateLinesQuest") {
        scenario("No Files exist")
        {
            QuestFactory.generateLinesQuest(user, property, parameters, listener, classes) shouldBe null
        }

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        scenario("Existing file is sufficiently covered already")
        {
            QuestFactory.generateLinesQuest(user, property, parameters, listener, classes) shouldBe null
        }

        every { sourceDetail.coverage } returns 0.8
        every { JacocoUtil.calculateCurrentFilePath(any(), any(), any()) } returns mockkClass(FilePath::class)
        val elements = mockkClass(Elements::class)
        every { elements.size } returns 1
        every { JacocoUtil.getLines(any()) } returns elements
        every { sourceDetail.jacocoSourceFile } returns File("")
        every { sourceDetail.parameters } returns Constants.Parameters()
        scenario("Not enough lines in File")
        {
            QuestFactory.generateLinesQuest(user, property, parameters, listener, classes) shouldBe null
        }

        every { sourceDetail.packageName } returns "org.example"
        every { sourceDetail.fileName } returns "TestClass"
        every { sourceDetail.fileExtension } returns "java"
        every { elements.size } returns 3
        val line1 = mockkClass(Element::class)
        val line2 = mockkClass(Element::class)
        val line3 = mockkClass(Element::class)
        every { line1.attr("class") } returns "fc"
        every { line1.attr("id") } returns "L7"
        every { line1.attr("title") } returns ""
        every { line1.text() } returns "content"
        every { line2.attr("class") } returns "fc"
        every { line2.attr("id") } returns "L8"
        every { line2.attr("title") } returns ""
        every { line2.text() } returns "content"
        every { line3.attr("class") } returns "fc"
        every { line3.attr("id") } returns "L9"
        every { line3.attr("title") } returns ""
        every { line3.text() } returns "content"
        every { JacocoUtil.generateDocument(any()) } returns mockkClass(Document::class)
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 1
        every { JacocoUtil.chooseRandomLine(any(), any()) } returns line1 andThen null andThen line2 andThen line3
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList()
        scenario("Successful Generation, nothing previously rejected")
        {
            QuestFactory.generateLinesQuest(user, property, parameters, listener, classes)!!.name shouldBe "Lines over lines - Solve three Line Coverage Challenges"
        }

        val challenge = mockkClass(LineCoverageChallenge::class)
        every { challenge == any() } returns true
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList(arrayListOf(Pair(challenge, "")))
        every { JacocoUtil.chooseRandomLine(any(), any()) } returns line1 andThen line2 andThen line3
        scenario("Challenge previously rejected")
        {
            QuestFactory.generateLinesQuest(user, property, parameters, listener, classes) shouldBe null
        }

        every { challenge == any() } returns false
        every { JacocoUtil.chooseRandomLine(any(), any()) } returns line1 andThen line2 andThen line3
        scenario("Successful Generation, other challenge previously rejected")
        {
            QuestFactory.generateLinesQuest(user, property, parameters, listener, classes)!!.name shouldBe "Lines over lines - Solve three Line Coverage Challenges"
        }
    }

    //TODO: Fix
    xfeature("generateMutationQuest") {
        QuestFactory.generateMutationQuest(user, property, parameters, listener, classes) shouldBe null

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)

        QuestFactory.generateMutationQuest(user, property, parameters, listener, classes) shouldBe null

        QuestFactory.generateMutationQuest(user, property, parameters, listener, classes)!!.name shouldBe "Coverage is not everything - Solve three Mutation Test Challenges"

    }

    feature("generateMethodsQuest") {
        scenario("No Files exist")
        {
            QuestFactory.generateMethodsQuest(user, property, parameters, listener, classes) shouldBe null
        }

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        scenario("Existing file is sufficiently covered already")
        {
            QuestFactory.generateMethodsQuest(user, property, parameters, listener, classes) shouldBe null
        }

        every { sourceDetail.coverage } returns 0.8
        every { sourceDetail.jacocoMethodFile } returns File("")
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf()
        scenario("No Methods exist in file")
        {
            QuestFactory.generateMethodsQuest(user, property, parameters, listener, classes) shouldBe null
        }

        val method1 = JacocoUtil.CoverageMethod("method1", 10, 5, "7")
        val method2 = JacocoUtil.CoverageMethod("method2", 10, 5, "7")
        val method3 = JacocoUtil.CoverageMethod("method3", 10, 10, "7")
        val method4 = JacocoUtil.CoverageMethod("method4", 10, 5, "7")
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf(method1, method2, method3, method4)
        every { JacocoUtil.generateDocument(any()) } returns mockkClass(Document::class)
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 1
        every { sourceDetail.jacocoSourceFile } returns File("")
        every { sourceDetail.parameters } returns parameters
        every { sourceDetail.packageName } returns "org.example"
        every { sourceDetail.fileName } returns "TestClass"
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList()
        scenario("Successful Generation")
        {
            QuestFactory.generateMethodsQuest(user, property, parameters, listener, classes)!!.name shouldBe "More than methods - Solve three Method Coverage Challenge"
        }

        val challenge = mockkClass(LineCoverageChallenge::class)
        every { challenge == any() } returns true
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList(arrayListOf(Pair(challenge, "")))
        scenario("Challenge previously rejected")
        {
            QuestFactory.generateMethodsQuest(user, property, parameters, listener, classes) shouldBe null
        }
    }

    feature("generatePackageQuest") {
        scenario("No Files exist")
        {
            QuestFactory.generatePackageQuest(user, property, parameters, listener, classes) shouldBe null
        }

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        scenario("Existing file is sufficiently covered already")
        {
            QuestFactory.generatePackageQuest(user, property, parameters, listener, classes) shouldBe null
        }

        every { sourceDetail.coverage } returns 0.8
        every { sourceDetail.packageName } returns "package"
        scenario("Not enough Classes in package")
        {
            QuestFactory.generatePackageQuest(user, property, parameters, listener, classes) shouldBe null
        }

        val details1 = mockkClass(SourceFileDetails::class)
        val details2 = mockkClass(SourceFileDetails::class)
        val details3 = mockkClass(SourceFileDetails::class)
        every { details1.coverage } returns 0.8
        every { details2.coverage } returns 0.8
        every { details3.coverage } returns 0.8
        every { details1.packageName } returns "package"
        every { details2.packageName } returns "package"
        every { details3.packageName } returns "other"
        every { details1.filesExists() } returns true
        every { details2.filesExists() } returns true
        every { details3.filesExists() } returns true
        classes.add(details1)
        classes.add(details2)
        classes.add(details3)
        every { ChallengeFactory.generateChallenge(any(), any(), any(), any(), any()) } returns mockkClass(LineCoverageChallenge::class)
        scenario("Identical Steps")
        {
            QuestFactory.generatePackageQuest(user, property, parameters, listener, classes)?.name shouldBe null
        }

        every { ChallengeFactory.generateChallenge(any(), any(), any(), any(), any()) } returns mockkClass(LineCoverageChallenge::class) andThen mockkClass(MethodCoverageChallenge::class) andThen mockkClass(ClassCoverageChallenge::class)
        scenario("Successful Generation")
        {
            QuestFactory.generatePackageQuest(user, property, parameters, listener, classes)?.name shouldBe "Pack it together - Solve three Challenges in the same package"
        }
    }

    feature("generateSmellQuest") {
        val file = mockkClass(SourceFileDetails::class)
        every { file.contents() } returns ""
        val issue1 = mockkClass(Issue::class)
        val issue2 = mockkClass(Issue::class)
        val issue3 = mockkClass(Issue::class)

        every { issue1.startLine } returns 1
        every { issue2.startLine } returns 1
        every { issue3.startLine } returns 1
        every { issue1.endLine } returns 2
        every { issue2.endLine } returns 2
        every { issue3.endLine } returns 2
        every { issue1.message } returns ""
        every { issue2.message } returns ""
        every { issue3.message } returns ""
        every { issue1.ruleKey } returns ""
        every { issue2.ruleKey } returns ""
        every { issue3.ruleKey } returns ""

        val issueList = arrayListOf(issue1, issue2, issue3)
        mockkStatic(SmellUtil::class)

        scenario("No Files exist")
        {
            QuestFactory.generateSmellQuest(user, property, parameters, listener, arrayListOf()) shouldBe null
        }

        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf()
        scenario("No Smells in File")
        {
            QuestFactory.generateSmellQuest(user, property, parameters, listener, arrayListOf(file)) shouldBe null
        }

        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(issue1, issue2)
        scenario("Not enough smells in file")
        {
            QuestFactory.generateSmellQuest(user, property, parameters, listener, arrayListOf(file)) shouldBe null
        }

        every { SmellUtil.getSmellsOfFile(file, any()) } returns issueList
        scenario("Successful Generation")
        {
            QuestFactory.generateSmellQuest(user, property, parameters, listener, arrayListOf(file))!!.name shouldBe "Smelly - Solve three Smell Challenges in the same class"
        }
    }

    feature("generateTestQuest") {
        val sourceDetail = mockkClass(SourceFileDetails::class)
        classes.add(sourceDetail)
        QuestFactory.generateTestQuest(user, property, parameters, listener, classes).name shouldBe "Just test - Solve three Test Challenges"
    }
})
