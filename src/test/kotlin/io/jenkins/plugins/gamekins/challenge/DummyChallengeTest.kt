package io.jenkins.plugins.gamekins.challenge

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.mockkClass
import io.mockk.mockkStatic
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class DummyChallengeTest : AnnotationSpec() {

    private val challenge = DummyChallenge()

    @Test
    fun isSolved() {
        val run = mockkClass(Run::class)
        val map = HashMap<String, String>()
        val listener = TaskListener.NULL
        val path = FilePath(null, "")

        challenge.isSolvable(map, run, listener, path) shouldBe true
        challenge.isSolved(map, run, listener, path) shouldBe true
        challenge.getScore() shouldBe 0
        challenge.getCreated() shouldBe 0
        challenge.getSolved() shouldBe 0
    }

    @Test
    fun printToXML() {
        challenge.printToXML("", "") shouldBe "<DummyChallenge>"
        challenge.printToXML("", "    ") shouldStartWith "    <"
        challenge.printToXML("test", "") shouldBe "<DummyChallenge>"
        challenge.toString() shouldBe "You have nothing developed recently"
    }
}