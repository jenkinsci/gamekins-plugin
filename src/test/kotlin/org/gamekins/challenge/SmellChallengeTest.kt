package org.gamekins.challenge

import hudson.model.TaskListener
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import org.gamekins.file.OtherFileDetails
import org.gamekins.file.SourceFileDetails
import org.gamekins.file.TestFileDetails
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue
import org.gamekins.util.Constants
import org.gamekins.util.SmellUtil
import org.sonarsource.sonarlint.core.analysis.api.TextRange

class SmellChallengeTest : AnnotationSpec() {

    private val file = mockkClass(SourceFileDetails::class)
    private val issue = mockkClass(Issue::class)
    lateinit var challenge: SmellChallenge

    @BeforeEach
    fun init() {
        mockkStatic(SmellUtil::class)
        every { issue.startLine } returns 1
        every { issue.endLine } returns 2
        every { issue.message } returns "Message"
        every { issue.ruleKey } returns "Key"
        every { issue.severity } returns "MAJOR"
        every { issue.type } returns "Type"
        every { file.contents() } returns "Content"
        every { file.packageName } returns "org.example"
        every { file.fileName } returns "File"
        every { file.parameters } returns Constants.Parameters(branch = "master")
        every { file.update(any()) } returns file

        challenge = SmellChallenge(file, issue)
    }

    @Test
    fun equals() {
        challenge.equals(null) shouldBe false

        challenge.equals(file) shouldBe false

        val secondChallenge = mockkClass(SmellChallenge::class)
        val secondFile = mockkClass(SourceFileDetails::class)
        every { secondFile.packageName } returns "org.test"
        every { secondChallenge.details } returns secondFile
        (challenge == secondChallenge) shouldBe  false

        every { secondFile.packageName } returns "org.example"
        every { secondFile.fileName } returns "Name"
        every { secondChallenge.details } returns secondFile
        (challenge == secondChallenge) shouldBe  false

        every { secondFile.fileName } returns "File"
        every { secondChallenge.details } returns secondFile
        val secondIssue = mockkClass(Issue::class)
        every { secondIssue.severity } returns "MINOR"
        every { secondChallenge.issue } returns secondIssue
        (challenge == secondChallenge) shouldBe  false

        every { secondIssue.severity } returns "MAJOR"
        every { secondIssue.type } returns "Other"
        every { secondChallenge.issue } returns secondIssue
        (challenge == secondChallenge) shouldBe  false

        every { secondIssue.type } returns "Type"
        every { secondIssue.ruleKey } returns "Rule"
        every { secondChallenge.issue } returns secondIssue
        (challenge == secondChallenge) shouldBe  false

        every { secondIssue.ruleKey } returns "Key"
        every { secondChallenge.issue } returns secondIssue
        (challenge == secondChallenge) shouldBe  false

        val identicalChallenge = SmellChallenge(file, issue)
        (challenge == identicalChallenge) shouldBe true
    }

    @Test
    fun getHighlightedFileContent() {
        val content = "<pre class='prettyprint mt-2 linenums:1'><code class='language-java'>Content</code></pre>"
        challenge.getHighlightedFileContent() shouldBe content
    }

    @Test
    fun getName() {
        challenge.getName() shouldBe "Code Smell"

        val testFile = mockkClass(TestFileDetails::class)
        every { testFile.contents() } returns ""
        val testSmellChallenge = SmellChallenge(testFile, issue)
        testSmellChallenge.getName() shouldBe "Test Smell"

        val otherFile = mockkClass(OtherFileDetails::class)
        every { otherFile.contents() } returns ""
        val otherSmellChallenge = SmellChallenge(otherFile, issue)
        otherSmellChallenge.getName() shouldBe "Smell"
    }

    @Test
    fun getParameters() {
        challenge.getParameters() shouldBe file.parameters
    }

    @Test
    fun getScore() {
        challenge.getScore() shouldBe 2

        every { issue.severity } returns "BLOCKER"
        challenge.getScore() shouldBe 4

        every { issue.severity } returns "CRITICAL"
        challenge.getScore() shouldBe 3

        every { issue.severity } returns "MINOR"
        challenge.getScore() shouldBe 1
    }

    @Test
    fun getSnippet() {
        val snippet = "<pre class='prettyprint mt-2 linenums:-1'><code class='language-java'>Content</code></pre><br><em>Message <a href=\"https://rules.sonarsource.com/java/RSPEC-Key\" target=\"_blank\">More Information</a> </em>"
        challenge.getSnippet() shouldBe snippet
    }

    @Test
    fun testHashCode() {
        challenge.hashCode() shouldBe challenge.hashCode()
    }

    @Test
    fun isSolvable() {
        val run = mockkClass(hudson.model.Run::class)
        val parameters = Constants.Parameters(branch = "test")

        challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe true

        parameters.branch = "master"
        every { issue.textRange } returns null
        challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe true

        every { issue.textRange } returns TextRange(1)
        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(issue)
        challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe true

        val secondIssue = mockkClass(Issue::class)
        every { secondIssue.ruleKey } returns issue.ruleKey
        every { secondIssue.type } returns issue.type
        every { secondIssue.severity } returns issue.severity
        every { secondIssue.textRange } returns issue.textRange
        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(secondIssue)
        challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe true

        every { secondIssue.textRange } returns TextRange(2)
        every { secondIssue.startLine } returns 2
        every { secondIssue.endLine } returns 2
        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(secondIssue)
        every { SmellUtil.getLineContent(file, any(), any()) } returns "Content"
        challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe true

        every { SmellUtil.getLineContent(file, any(), any()) } returns "Nothing"
        challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe false

        every { secondIssue.ruleKey } returns "Nothing"
        challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe false

        every { secondIssue.ruleKey } returns issue.ruleKey
        every { secondIssue.type } returns "Nothing"
        challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe false

        every { secondIssue.type } returns issue.type
        every { secondIssue.severity } returns "Nothing"
        challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe false
    }

    @Test
    fun isSolved() {
        val run = mockkClass(hudson.model.Run::class)
        val parameters = Constants.Parameters()
        every { issue.textRange } returns TextRange(1)

        every { SmellUtil.getSmellsOfFile(any(), any()) } returns arrayListOf(issue)
        challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe false

        val secondIssue = mockkClass(Issue::class)
        every { secondIssue.ruleKey } returns issue.ruleKey
        every { secondIssue.type } returns issue.type
        every { secondIssue.severity } returns issue.severity
        every { secondIssue.textRange } returns issue.textRange
        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(secondIssue)
        challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe false

        every { secondIssue.textRange } returns TextRange(2)
        every { secondIssue.startLine } returns 2
        every { secondIssue.endLine } returns 2
        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(secondIssue)
        every { SmellUtil.getLineContent(file, any(), any()) } returns "Content"
        challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe false

        every { SmellUtil.getLineContent(file, any(), any()) } returns "Nothing"
        challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe true
        challenge.getSolved() shouldBeGreaterThan 0

        every { secondIssue.ruleKey } returns "Nothing"
        challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe true

        every { secondIssue.ruleKey } returns issue.ruleKey
        every { secondIssue.type } returns "Nothing"
        challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe true

        every { secondIssue.type } returns issue.type
        every { secondIssue.severity } returns "Nothing"
        challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe true
    }

    @Test
    fun printToXML() {
        var text = "<SmellChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"File\" type=\"Type\" severity=\"MAJOR\" line=\"1\" rule=\"Key\"/>"
        challenge.printToXML("", "") shouldBe text

        text = "    <SmellChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"File\" type=\"Type\" severity=\"MAJOR\" line=\"1\" rule=\"Key\"/>"
        challenge.printToXML("", "    ") shouldBe text

        text = "<SmellChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"File\" type=\"Type\" severity=\"MAJOR\" line=\"1\" rule=\"Key\" reason=\"reason\"/>"
        challenge.printToXML("reason", "") shouldBe text
    }

    @Test
    fun testToString() {
        challenge.toString() shouldBe "Improve your code by removing the smell starting from line <b>1</b> in class <b>File</b> in package <b>org.example</b> (created for branch master)"

        every { issue.startLine } returns null
        val secondChallenge = SmellChallenge(file, issue)
        secondChallenge.toString() shouldBe "Improve your code by removing the smell concerning the whole class <b>File</b> in package <b>org.example</b> (created for branch master)"
    }
}