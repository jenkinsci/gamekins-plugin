/*
 * Copyright 2022 Gamekins contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gamekins.challenge

import hudson.model.Run
import hudson.model.TaskListener
import io.kotest.core.spec.style.FeatureSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.mockkClass
import io.mockk.unmockkAll
import org.gamekins.util.Constants
import org.gamekins.util.Constants.Parameters

class DummyChallengeTest : FeatureSpec({

    val challenge = DummyChallenge(Parameters(), Constants.NOTHING_DEVELOPED)

    afterSpec {
        unmockkAll()
    }

    feature("Dummy Values") {
        val run = mockkClass(Run::class)
        val parameters = Parameters()
        val listener = TaskListener.NULL

        scenario("isSolvable")
        {
            challenge.isSolvable(parameters, run, listener) shouldBe true
        }

        scenario("isSolved")
        {
            challenge.isSolved(parameters, run, listener) shouldBe true
        }

        scenario("getScore")
        {
            challenge.getScore() shouldBe 0
        }

        scenario("getCreated")
        {
            challenge.getCreated() shouldBe 0
        }

        scenario("getSolved")
        {
            challenge.getSolved() shouldBe 0
        }

        scenario("getName")
        {
            challenge.getName()  shouldBe "Dummy"
        }
    }

    feature("printToXML") {
        scenario("No Reason, no Indentation")
        {
            challenge.printToXML("", "") shouldBe "<DummyChallenge/>"
        }

        scenario("No Reason, with Indentation")
        {
            challenge.printToXML("", "    ") shouldStartWith "    <"
        }

        scenario("With Reason, no Indentation")
        {
            challenge.printToXML("test", "") shouldBe "<DummyChallenge/>"
        }
    }

    feature("toString")
    {
        scenario("Nothing developed")
        {
            challenge.toString() shouldBe Constants.NOTHING_DEVELOPED
        }

        val challenge1 = DummyChallenge(Parameters(), "Other reason")
        scenario("Other reason")
        {
            challenge1.toString() shouldBe "Other reason"
        }
    }
})
