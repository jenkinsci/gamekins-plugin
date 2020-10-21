package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Result
import hudson.model.Run
import hudson.model.TaskListener
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.unmockkAll

class BuildChallengeTest : AnnotationSpec() {

    private lateinit var challenge : BuildChallenge

    @BeforeEach
    fun init() {
        challenge = BuildChallenge()
    }

    @AfterAll
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun isSolved() {
        val run = mockkClass(Run::class)
        val map = HashMap<String, String>()
        val listener = TaskListener.NULL
        val path = FilePath(null, "")

        challenge.isSolvable(map, run, listener, path) shouldBe true
        every { run.getResult() } returns Result.FAILURE
        challenge.isSolved(map, run, listener, path) shouldBe false
        every { run.getResult() } returns Result.SUCCESS
        challenge.isSolved(map, run, listener, path) shouldBe true
        challenge.getSolved() shouldNotBe 0
        challenge.getScore() shouldBe 1
    }

    @Test
    fun printToXML() {
        challenge.printToXML("", "") shouldBe
                "<BuildChallenge created=\"${challenge.getCreated()}\" solved=\"${challenge.getSolved()}\"/>"
        challenge.printToXML("", "    ") shouldStartWith "    <"
        challenge.printToXML("test", "") shouldBe
                "<BuildChallenge created=\"${challenge.getCreated()}\" solved=\"0\" reason=\"test\"/>"
        challenge.toString() shouldBe "Let the Build run successfully"
    }
}