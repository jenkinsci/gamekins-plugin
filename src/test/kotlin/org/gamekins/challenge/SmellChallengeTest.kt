package org.gamekins.challenge

import hudson.model.TaskListener
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkStatic
import org.gamekins.file.OtherFileDetails
import org.gamekins.file.SourceFileDetails
import org.gamekins.file.TestFileDetails
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue
import org.gamekins.util.Constants
import org.gamekins.util.SmellUtil
import org.sonarsource.sonarlint.core.commons.IssueSeverity
import org.sonarsource.sonarlint.core.commons.RuleType
import org.sonarsource.sonarlint.core.commons.TextRange

class SmellChallengeTest : FeatureSpec({

    val file = mockkClass(SourceFileDetails::class)
    val issue = mockkClass(Issue::class)
    lateinit var challenge: SmellChallenge

    beforeContainer {
        mockkStatic(SmellUtil::class)
        every { issue.startLine } returns 1
        every { issue.endLine } returns 2
        every { issue.message } returns "Message"
        every { issue.ruleKey } returns "Key-155"
        every { issue.severity } returns IssueSeverity.MAJOR
        every { issue.type } returns RuleType.BUG
        every { file.contents() } returns "Content"
        every { file.packageName } returns "org.example"
        every { file.fileName } returns "File"
        every { file.parameters } returns Constants.Parameters(branch = "master")
        every { file.update(any()) } returns file

        challenge = SmellChallenge(file, issue)
    }

    feature("equals") {
        scenario("Null")
        {
            challenge.equals(null) shouldBe false
        }

        scenario("Not Challenge")
        {
            challenge.equals(file) shouldBe false
        }

        scenario("Self")
        {
            (challenge == challenge) shouldBe true
        }

        val secondChallenge = mockkClass(SmellChallenge::class)
        val secondFile = mockkClass(SourceFileDetails::class)
        every { secondFile.packageName } returns "org.test"
        every { secondChallenge.details } returns secondFile
        scenario("Different PackageName")
        {
            (challenge == secondChallenge) shouldBe  false
        }

        every { secondFile.packageName } returns "org.example"
        every { secondFile.fileName } returns "Name"
        every { secondChallenge.details } returns secondFile
        scenario("Different FileName")
        {
            (challenge == secondChallenge) shouldBe  false
        }

        every { secondFile.fileName } returns "File"
        every { secondChallenge.details } returns secondFile
        val secondIssue = mockkClass(Issue::class)
        every { secondIssue.severity } returns IssueSeverity.MINOR
        every { secondChallenge.issue } returns secondIssue
        scenario("Different Severity")
        {
            (challenge == secondChallenge) shouldBe  false
        }

        every { secondIssue.severity } returns IssueSeverity.MAJOR
        every { secondIssue.type } returns RuleType.CODE_SMELL
        every { secondChallenge.issue } returns secondIssue
        scenario("Different Type")
        {
            (challenge == secondChallenge) shouldBe  false
        }

        every { secondIssue.type } returns RuleType.BUG
        every { secondIssue.ruleKey } returns "Rule"
        every { secondChallenge.issue } returns secondIssue
        scenario("Different RuleKey")
        {
            (challenge == secondChallenge) shouldBe  false
        }

        every { secondIssue.ruleKey } returns "Key"
        every { secondChallenge.issue } returns secondIssue
        scenario("Different LineContent")
        {
            (challenge == secondChallenge) shouldBe  false
        }

        val identicalChallenge = SmellChallenge(file, issue)
        scenario("Identical Challenge")
        {
            (challenge == identicalChallenge) shouldBe true
        }
    }

    feature("getHighlightedFileContent") {
        val content = "<pre class='prettyprint mt-2 linenums:1'><code class='language-java'>Content</code></pre>"
        challenge.getHighlightedFileContent() shouldBe content
    }

    feature("getName") {
        scenario("Challenge references SourceFile")
        {
            challenge.getName() shouldBe "Code Smell"
        }

        val testFile = mockkClass(TestFileDetails::class)
        every { testFile.contents() } returns ""
        val testSmellChallenge = SmellChallenge(testFile, issue)
        scenario("Challenge references TestFile")
        {
            testSmellChallenge.getName() shouldBe "Test Smell"
        }

        val otherFile = mockkClass(OtherFileDetails::class)
        every { otherFile.contents() } returns ""
        val otherSmellChallenge = SmellChallenge(otherFile, issue)
        scenario("Unspecified Smell")
        {
            otherSmellChallenge.getName() shouldBe "Smell"
        }
    }

    feature("getParameters") {
        challenge.getParameters() shouldBe file.parameters
    }

    feature("getScore") {
        scenario("IssueSeverity MAJOR")
        {
            challenge.getScore() shouldBe 2
        }

        every { issue.severity } returns IssueSeverity.BLOCKER
        scenario("IssueSeverity BLOCKER")
        {
            challenge.getScore() shouldBe 4
        }

        every { issue.severity } returns IssueSeverity.CRITICAL
        scenario("IssueSeverity CRITICAL")
        {
            challenge.getScore() shouldBe 3
        }

        every { issue.severity } returns IssueSeverity.MINOR
        scenario("Other Issue Severity (here MINOR)")
        {
            challenge.getScore() shouldBe 1
        }
    }

    feature("getSnippet") {
        val snippet = "<pre class='prettyprint mt-2 linenums:-1'><code class='language-java'>Content</code></pre><br><em>Message <a href=\"https://rules.sonarsource.com/java/RSPEC-155\" target=\"_blank\">More Information</a> </em>"
        challenge.getSnippet() shouldBe snippet
    }

    feature("testHashCode") {
        challenge.hashCode() shouldBe challenge.hashCode()
    }

    feature("isSolvable") {
        val run = mockkClass(hudson.model.Run::class)
        val parameters = Constants.Parameters(branch = "test")
        scenario("Different Branch")
        {
            challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe true
        }

        parameters.branch = "master"
        every { issue.textRange } returns null
        scenario("TextRange is null")
        {
            challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe true
        }

        every { issue.textRange } returns TextRange(1, 0, 1, 0)
        every { file.filesExists() } returns false
        scenario("DetailsFile does not exist")
        {
            challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe false
        }

        every { file.filesExists() } returns true
        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(issue)
        scenario("Issue is still open")
        {
            challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe true
        }

        val secondIssue = mockkClass(Issue::class)
        every { secondIssue.ruleKey } returns issue.ruleKey
        every { secondIssue.type } returns issue.type
        every { secondIssue.severity } returns issue.severity
        every { secondIssue.textRange } returns issue.textRange
        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(secondIssue)
        scenario("Equivalent open Issue exists with same textRange")
        {
            challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe true
        }

        every { secondIssue.textRange } returns TextRange(2, 0, 2, 0)
        every { secondIssue.startLine } returns 2
        every { secondIssue.endLine } returns 2
        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(secondIssue)
        every { SmellUtil.getLineContent(file, any(), any()) } returns "Content"
        scenario("Equivalent open Issue exists with same lineContent")
        {
            challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe true
        }

        every { SmellUtil.getLineContent(file, any(), any()) } returns "Nothing"
        scenario("Different to open Issues by LineContent")
        {
            challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe false
        }

        every { secondIssue.ruleKey } returns "Nothing"
        scenario("Different to open Issues by RuleKey")
        {
            challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe false
        }

        every { secondIssue.ruleKey } returns issue.ruleKey
        every { secondIssue.type } returns RuleType.BUG
        scenario("Different to open Issues by type")
        {
            challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe false
        }

        every { secondIssue.type } returns issue.type
        every { secondIssue.severity } returns IssueSeverity.CRITICAL
        scenario("Different to open Issues by severity")
        {
            challenge.isSolvable(parameters, run, TaskListener.NULL) shouldBe false
        }
    }

    feature("isSolved") {
        val run = mockkClass(hudson.model.Run::class)
        val parameters = Constants.Parameters()
        every { issue.textRange } returns TextRange(1, 0, 1, 0)

        every { file.filesExists() } returns false
        scenario("DetailsFile does not exist")
        {
            challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe false
        }

        every { SmellUtil.getSmellsOfFile(any(), any()) } returns arrayListOf(issue)
        every { file.filesExists() } returns true
        scenario("Issue is still open")
        {
            challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe false
        }

        val secondIssue = mockkClass(Issue::class)
        every { secondIssue.ruleKey } returns issue.ruleKey
        every { secondIssue.type } returns issue.type
        every { secondIssue.severity } returns issue.severity
        every { secondIssue.textRange } returns issue.textRange
        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(secondIssue)
        scenario("No otherwise equivalent Issue has same textRange")
        {
            challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe false
        }

        every { secondIssue.textRange } returns TextRange(2, 0, 2, 0)
        every { secondIssue.startLine } returns 2
        every { secondIssue.endLine } returns 2
        every { SmellUtil.getSmellsOfFile(file, any()) } returns arrayListOf(secondIssue)
        every { SmellUtil.getLineContent(file, any(), any()) } returns "Content"
        scenario("No otherwise equivalent Issue has same lineContent")
        {
            challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe false
        }

        every { SmellUtil.getLineContent(file, any(), any()) } returns "Nothing"
        scenario("Smell solved (No Issue with same lineContent)")
        {
            challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe true
            challenge.getSolved() shouldBeGreaterThan 0
        }

        every { secondIssue.ruleKey } returns "Nothing"
        scenario("Smell solved (No Issue with same ruleKey)")
        {
            challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe true
        }

        every { secondIssue.ruleKey } returns issue.ruleKey
        every { secondIssue.type } returns RuleType.BUG
        scenario("Smell solved (No Issue with same RuleType)")
        {
            challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe true
        }

        every { secondIssue.type } returns issue.type
        every { secondIssue.severity } returns IssueSeverity.CRITICAL
        scenario("Smell solved (No Issue with same Severity)")
        {
            challenge.isSolved(parameters, run, TaskListener.NULL) shouldBe true
        }
    }

    feature("printToXML") {
        var text = "<SmellChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"File\" type=\"BUG\" severity=\"MAJOR\" line=\"1\" rule=\"Key-155\"/>"
        scenario("No Reason, no Indentation")
        {
            challenge.printToXML("", "") shouldBe text
        }

        text = "    <SmellChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"File\" type=\"BUG\" severity=\"MAJOR\" line=\"1\" rule=\"Key-155\"/>"
        scenario("No Reason, with Indentation")
        {
            challenge.printToXML("", "    ") shouldBe text
        }

        text = "<SmellChallenge created=\"${challenge.getCreated()}\" solved=\"0\" class=\"File\" type=\"BUG\" severity=\"MAJOR\" line=\"1\" rule=\"Key-155\" reason=\"reason\"/>"
        scenario("With Reason, no Indentation")
        {
            challenge.printToXML("reason", "") shouldBe text
        }
    }

    feature("testToString") {
        scenario("Smell in specific lines")
        {
            challenge.toString() shouldBe "Improve your code by removing the smell starting from line <b>1</b> in class <b>File</b> in package <b>org.example</b> (created for branch master)"
        }

        every { issue.startLine } returns null
        val secondChallenge = SmellChallenge(file, issue)
        scenario("Smell in whole class")
        {
            secondChallenge.toString() shouldBe "Improve your code by removing the smell concerning the whole class <b>File</b> in package <b>org.example</b> (created for branch master)"
        }
    }
})