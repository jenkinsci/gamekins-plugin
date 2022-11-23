package org.gamekins.challenge.quest

import hudson.FilePath
import hudson.model.TaskListener
import hudson.model.User
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.gamekins.GameUserProperty
import org.gamekins.challenge.*
import org.gamekins.file.FileDetails
import org.gamekins.file.SourceFileDetails
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

class QuestFactoryTest : AnnotationSpec() {

    private val user = mockkClass(User::class)
    private val property = mockkClass(GameUserProperty::class)
    private val parameters = Constants.Parameters()
    private val listener: TaskListener = TaskListener.NULL
    private lateinit var classes: ArrayList<FileDetails>
    private val numberOfQuests = 9

    @BeforeEach
    fun init() {
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
    }

    @Test
    fun generateNewQuests() {
        QuestFactory.generateNewQuests(user, property, parameters, listener, classes, 0) shouldBe 0

        every { property.newQuest(any(), any()) } returns Unit
        QuestFactory.generateNewQuests(user, property, parameters, listener, classes, 1) shouldBe 0

        val detail = mockkClass(FileDetails::class)
        val gameUser = GitUtil.GameUser(user)
        every { detail.changedByUsers } returns hashSetOf(gameUser)
        classes.add(detail)
        every { QuestFactory.generateQuest(any(), any(), any(), any(), any()) } returns Quest(Constants.NO_QUEST, arrayListOf())
        QuestFactory.generateNewQuests(user, property, parameters, listener, classes, 1) shouldBe 0

        every { QuestFactory.generateQuest(any(), any(), any(), any(), any()) } returns Quest("Some quest", arrayListOf())
        QuestFactory.generateNewQuests(user, property, parameters, listener, classes, 1) shouldBe 1
    }

    @Test
    fun generateTest() {
        every { QuestFactory.generateLinesQuest(any(), any(), any(), any(), any()) } returns null
        every { Random.nextInt(numberOfQuests) } returns 0
        every { property.getRejectedQuests(any()) } returns CopyOnWriteArrayList()
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe Quest(Constants.NO_QUEST, arrayListOf())

        val quest = mockkClass(Quest::class)
        every { QuestFactory.generateLinesQuest(any(), any(), any(), any(), any()) } returns quest
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest

        val pair = Pair(quest, "")
        every { property.getRejectedQuests(any()) } returns CopyOnWriteArrayList(arrayListOf(pair))
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe Quest(Constants.NO_QUEST, arrayListOf())

        every { property.getRejectedQuests(any()) } returns CopyOnWriteArrayList()
        every { QuestFactory.generateMethodsQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 1
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest

        every { QuestFactory.generatePackageQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 2
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest

        every { QuestFactory.generateClassQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 3
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest

        every { QuestFactory.generateExpandingQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 4
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest

        every { QuestFactory.generateDecreasingQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 5
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest

        every { QuestFactory.generateTestQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 6
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest

        every { QuestFactory.generateMutationQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 7
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest

        every { QuestFactory.generateSmellQuest(any(), any(), any(), any(), any()) } returns quest
        every { Random.nextInt(numberOfQuests) } returns 8
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe quest

        every { Random.nextInt(numberOfQuests) } returns 9
        QuestFactory.generateQuest(user, property, parameters, listener, classes) shouldBe Quest(Constants.NO_QUEST, arrayListOf())
    }

    @Test
    fun generateClassQuest() {
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList()
        QuestFactory.generateClassQuest(user, property, parameters, listener, classes) shouldBe null

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        QuestFactory.generateClassQuest(user, property, parameters, listener, classes) shouldBe null

        every { sourceDetail.coverage } returns 0.8
        val challenge = mockkClass(ClassCoverageChallenge::class)
        val classDetails = mockkClass(SourceFileDetails::class)
        every { sourceDetail == any() } returns true
        every { challenge.details } returns classDetails
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList(arrayListOf(Pair(challenge, "")))
        QuestFactory.generateClassQuest(user, property, parameters, listener, classes) shouldBe null

        every { sourceDetail == any() } returns false
        every { sourceDetail.jacocoSourceFile } returns File("")
        every { sourceDetail.parameters } returns Constants.Parameters()
        every { sourceDetail.fileName } returns "file"
        every { JacocoUtil.generateDocument(any()) } returns mockkClass(Document::class)
        every { JacocoUtil.calculateCoveredLines(any(), any()) } returns 1
        QuestFactory.generateClassQuest(user, property, parameters, listener, classes)!!.name shouldBe "Incremental - Solve three Class Coverage Challenges"
    }

    @Test
    fun generateDecreasingQuest() {
        QuestFactory.generateDecreasingQuest(user, property, parameters, listener, classes) shouldBe null

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        QuestFactory.generateDecreasingQuest(user, property, parameters, listener, classes) shouldBe null

        every { sourceDetail.coverage } returns 0.8
        every { sourceDetail.jacocoSourceFile } returns File("")
        every { sourceDetail.parameters } returns Constants.Parameters()
        every { sourceDetail.fileName } returns "file"
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
        QuestFactory.generateDecreasingQuest(user, property, parameters, listener, classes)!!.name shouldBe "Decreasing - Solve a Class, Method and Line Coverage Challenge"
    }

    @Test
    fun generateExpandingQuest() {
        QuestFactory.generateExpandingQuest(user, property, parameters, listener, classes) shouldBe null

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        QuestFactory.generateExpandingQuest(user, property, parameters, listener, classes) shouldBe null

        every { sourceDetail.coverage } returns 0.8
        every { sourceDetail.jacocoSourceFile } returns File("")
        every { sourceDetail.parameters } returns Constants.Parameters()
        every { sourceDetail.fileName } returns "file"
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
        QuestFactory.generateExpandingQuest(user, property, parameters, listener, classes)!!.name shouldBe "Expanding - Solve a Line, Method and Class Coverage Challenge"
    }

    @Test
    fun generateLinesQuest() {
        QuestFactory.generateLinesQuest(user, property, parameters, listener, classes) shouldBe null

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        QuestFactory.generateLinesQuest(user, property, parameters, listener, classes) shouldBe null

        every { sourceDetail.coverage } returns 0.8
        every { JacocoUtil.calculateCurrentFilePath(any(), any(), any()) } returns mockkClass(FilePath::class)
        val elements = mockkClass(Elements::class)
        every { elements.size } returns 1
        every { JacocoUtil.getLines(any()) } returns elements
        every { sourceDetail.jacocoSourceFile } returns File("")
        every { sourceDetail.parameters } returns Constants.Parameters()
        QuestFactory.generateLinesQuest(user, property, parameters, listener, classes) shouldBe null

        every { sourceDetail.packageName } returns "org.example"
        every { sourceDetail.fileName } returns "TestClass"
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
        QuestFactory.generateLinesQuest(user, property, parameters, listener, classes)!!.name shouldBe "Lines over lines - Solve three Line Coverage Challenges"

        val challenge = mockkClass(LineCoverageChallenge::class)
        every { challenge == any() } returns true
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList(arrayListOf(Pair(challenge, "")))
        every { JacocoUtil.chooseRandomLine(any(), any()) } returns line1 andThen line2 andThen line3
        QuestFactory.generateLinesQuest(user, property, parameters, listener, classes) shouldBe null

        every { challenge == any() } returns false
        every { JacocoUtil.chooseRandomLine(any(), any()) } returns line1 andThen line2 andThen line3
        QuestFactory.generateLinesQuest(user, property, parameters, listener, classes)!!.name shouldBe "Lines over lines - Solve three Line Coverage Challenges"
    }

    //TODO: Fix
    @Test
    @Ignore
    fun generateMutationQuest() {
        QuestFactory.generateMutationQuest(user, property, parameters, listener, classes) shouldBe null

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        every { ChallengeFactory.generateMutationTestChallenge(any(), any(), any(), any(), any(), any()) } returns null
        QuestFactory.generateMutationQuest(user, property, parameters, listener, classes) shouldBe null

        val mutationChallenge1 = mockkClass(MutationTestChallenge::class)
        val mutationChallenge2 = mockkClass(MutationTestChallenge::class)
        val mutationChallenge3 = mockkClass(MutationTestChallenge::class)
        every { ChallengeFactory.generateMutationTestChallenge(any(), any(), any(), any(), any(), any()) } returns mutationChallenge1 andThen mutationChallenge2 andThen mutationChallenge3
        QuestFactory.generateMutationQuest(user, property, parameters, listener, classes)!!.name shouldBe "Coverage is not everything - Solve three Mutation Test Challenges"
    }

    @Test
    fun generateMethodsQuest() {
        QuestFactory.generateMethodsQuest(user, property, parameters, listener, classes) shouldBe null

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        QuestFactory.generateMethodsQuest(user, property, parameters, listener, classes) shouldBe null

        every { sourceDetail.coverage } returns 0.8
        every { sourceDetail.jacocoMethodFile } returns File("")
        every { JacocoUtil.getMethodEntries(any()) } returns arrayListOf()
        QuestFactory.generateMethodsQuest(user, property, parameters, listener, classes) shouldBe null

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
        QuestFactory.generateMethodsQuest(user, property, parameters, listener, classes)!!.name shouldBe "More than methods - Solve three Method Coverage Challenge"

        val challenge = mockkClass(LineCoverageChallenge::class)
        every { challenge == any() } returns true
        every { property.getRejectedChallenges(any()) } returns CopyOnWriteArrayList(arrayListOf(Pair(challenge, "")))
        QuestFactory.generateMethodsQuest(user, property, parameters, listener, classes) shouldBe null
    }

    @Test
    fun generatePackageQuest() {
        QuestFactory.generatePackageQuest(user, property, parameters, listener, classes) shouldBe null

        val sourceDetail = mockkClass(SourceFileDetails::class)
        every { sourceDetail.coverage } returns 1.0
        every { sourceDetail.filesExists() } returns true
        classes.add(sourceDetail)
        QuestFactory.generatePackageQuest(user, property, parameters, listener, classes) shouldBe null

        every { sourceDetail.coverage } returns 0.8
        every { sourceDetail.packageName } returns "package"
        QuestFactory.generatePackageQuest(user, property, parameters, listener, classes) shouldBe null

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
        QuestFactory.generatePackageQuest(user, property, parameters, listener, classes)?.name shouldBe null

        every { ChallengeFactory.generateChallenge(any(), any(), any(), any(), any()) } returns mockkClass(LineCoverageChallenge::class) andThen mockkClass(MethodCoverageChallenge::class) andThen mockkClass(ClassCoverageChallenge::class)
        QuestFactory.generatePackageQuest(user, property, parameters, listener, classes)?.name shouldBe "Pack it together - Solve three Challenges in the same package"
    }

    @Test
    fun generateSmellQuest() {
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

        QuestFactory.generateSmellQuest(user, property, parameters, listener, arrayListOf()) shouldBe null

        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf()
        QuestFactory.generateSmellQuest(user, property, parameters, listener, arrayListOf(file)) shouldBe null

        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(issue1, issue2)
        QuestFactory.generateSmellQuest(user, property, parameters, listener, arrayListOf(file)) shouldBe null

        every { SmellUtil.getSmellsOfFile(file, any()) } returns issueList
        QuestFactory.generateSmellQuest(user, property, parameters, listener, arrayListOf(file))!!.name shouldBe "Smelly - Solve three Smell Challenges in the same class"
    }

    @Test
    fun generateTestQuest() {
        val sourceDetail = mockkClass(SourceFileDetails::class)
        classes.add(sourceDetail)
        QuestFactory.generateTestQuest(user, property, parameters, listener, classes).name shouldBe "Just test - Solve three Test Challenges"
    }
}
