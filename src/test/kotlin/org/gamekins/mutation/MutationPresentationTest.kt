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

package org.gamekins.mutation

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import org.gamekins.util.Pair

class MutationPresentationTest : AnnotationSpec() {

    private val mutationDetails = MutationDetails(
        mapOf(
            "className" to "org/example/ABC",
            "methodName" to "foo1",
            "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"
        ),
        listOf(14),
        "AOR",
        "IADDISUB51",
        "ABC.java", 51,
        "replace integer addition with integer subtraction",
        listOf("14"), mapOf("varName" to "numEntering")
    )
    private val mutation = MutationInfo(mutationDetails, "survived", -1547277781)


    private val mutationDetails1 = MutationDetails(
        mapOf(
            "className" to "org/example/ABC",
            "methodName" to "foo1",
            "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"
        ),
        listOf(14),
        "AOR",
        "IADD-ISUB-51",
        "ABC.java", 51,
        "replace integer addition with integer subtraction",
        listOf("14"), mapOf("varName" to "numEntering")
    )
    private val mutation1 = MutationInfo(mutationDetails1, "survived", -1547277781)

    private val mutationDetails2 = MutationDetails(
        mapOf(
            "className" to "org/example/Feature",
            "methodName" to "foo",
            "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"
        ),
        listOf(22),
        "AOD",
        "IADD-S-56",
        "Feature.java", 56,
        "delete second operand after arithmetic operator integer addition",
        listOf("22"), mapOf()
    )
    private val mutation2 = MutationInfo(mutationDetails2, "killed", -1547277782)

    private val mutationDetails6= MutationDetails(
        mapOf(
            "className" to "org/example/Feature",
            "methodName" to "foo",
            "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"
        ),
        listOf(22),
        "AOD",
        "ISUB-F-56",
        "Feature.java", 56,
        "delete first operand before arithmetic operator integer addition",
        listOf("22"), mapOf()
    )
    private val mutation6 = MutationInfo(mutationDetails6, "killed", -1547277782)


    private val mutationDetails3 = MutationDetails(
        mapOf(
            "className" to "org/example/Feature",
            "methodName" to "foo",
            "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"
        ),
        listOf(30),
        "PRUOI",
        "GETFIELD-PRUOI-I-56",
        "Hihi.java", 56,
        "replace less than or equal operator with greater than or equal operator",
        listOf("30"), mapOf("fieldName" to "b")
    )
    private val mutation3 = MutationInfo(mutationDetails3, "survived", -1547277782)


    private val mutationDetails4 = MutationDetails(
        mapOf(
            "className" to "org/example/Feature",
            "methodName" to "foo",
            "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"
        ),
        listOf(30),
        "PRUOI",
        "ILOAD-POUOI-I-50",
        "Hihi.java", 50,
        "replace less than or equal operator with greater than or equal operator",
        listOf("30"), mapOf("varName" to "b")
    )
    private val mutation4 = MutationInfo(mutationDetails4, "survived", -1547277782)

    private val mutationDetails5 = MutationDetails(
        mapOf(
            "className" to "org/example/Feature",
            "methodName" to "foo",
            "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"
        ),
        listOf(30),
        "PRUOI",
        "ILOAD-PRUORI-I-50",
        "Hihi.java", 50,
        "replace less than or equal operator with greater than or equal operator",
        listOf("30"), mapOf("varName" to "b")
    )
    private val mutation5 = MutationInfo(mutationDetails5, "survived", -1547277782)

    @Test
    fun testCreateMutatedLine() {
        mockkObject(MutationPresentation)
        every { MutationPresentation.getReplacementMutatedLine(any(), any()) } returns ""
        every { MutationPresentation.getInsertionMutatedLine(any(), any()) } returns "abc"
        every { MutationPresentation.getDeletionMutatedLine(any(), any()) } returns ""
        MutationPresentation.createMutatedLine("", mutation1, 90) shouldBe ""
        MutationPresentation.createMutatedLine("", mutation2, 90) shouldBe ""
        MutationPresentation.createMutatedLine("", mutation3, 90) shouldBe "<pre class='prettyprint linenums:90 mt-2'><code class='language-java'>abc</code></pre>"
    }

    @Test
    fun testGetReplacementMutatedLine() {
        unmockkAll()
        MutationUtils.mutationBlackList.clear()
        mockkObject(MutationPresentation)
        every { MutationPresentation.getReplacementText(any()) } returns "-"
        MutationPresentation.getReplacementMutatedLine("int a = b + d", mutation1) shouldBe "int a = b - d"
        MutationPresentation.getReplacementMutatedLine("int a = b++", mutation1) shouldBe ""
        assertEquals(MutationUtils.mutationBlackList, mutableSetOf(mutation1))
        MutationPresentation.getReplacementMutatedLine("int a = b++", mutation) shouldBe ""
        MutationPresentation.getReplacementMutatedLine("int a = ++b - a", mutation1) shouldBe ""
    }

    @Test
    fun testGetReplacementText() {
        unmockkAll()
        MutationPresentation.getReplacementText("LAND") shouldBe "&"
        MutationPresentation.getReplacementText("LOR") shouldBe "|"
        MutationPresentation.getReplacementText("IADD") shouldBe "+"
        MutationPresentation.getReplacementText("LSUB") shouldBe "-"
        MutationPresentation.getReplacementText("IF_ICMPLE") shouldBe ">"
        MutationPresentation.getReplacementText("IF_ICMPGE") shouldBe "<"
        MutationPresentation.getReplacementText("IDIV") shouldBe "/"
    }

    @Test
    fun testGetInsertionMutatedLine() {
        MutationPresentation.getInsertionMutatedLine("int a = ABC.b + d", mutation3) shouldBe "int a = (++ABC.b) + d"
        MutationPresentation.getInsertionMutatedLine("int a = (ABC.b) - d", mutation3) shouldBe "int a = ((++ABC.b)) - d"
        MutationPresentation.getInsertionMutatedLine("int a = d + b + b;", mutation4) shouldBe "int a = d + b + (b++);"
        MutationPresentation.getInsertionMutatedLine("int a = b + d", mutation5) shouldBe ""
    }

    @Test
    fun testGetDeletionMutatedLine() {
        MutationPresentation.getDeletionMutatedLine("int a = ABC.b + d;", mutation2) shouldBe "int a = ABC.b;"
        MutationPresentation.getDeletionMutatedLine("int a = (ABC.b - d) + 6;", mutation6) shouldBe "int a = ( d) + 6;"
        MutationPresentation.getDeletionMutatedLine("int a = ABC.b--;", mutation6) shouldBe ""
        assertEquals(MutationUtils.mutationBlackList, mutableSetOf(mutation6))
        MutationPresentation.getDeletionMutatedLine("int a = ABC.b--;", mutation) shouldBe ""
        mockkObject(MutationPresentation)
        every { MutationPresentation.getDeleteRange(any(), any(), any()) } returns Pair(2,100)
        MutationPresentation.getDeletionMutatedLine("int a = ABC.b - d;", mutation6) shouldBe ""
    }

    @Test
    fun testGetDeleteRange() {
        unmockkAll()
        MutationPresentation.getDeleteRange("int a = x + ABC.b + d + m;", IntRange(18, 18), "F") shouldBe Pair(12, 18)
        MutationPresentation.getDeleteRange("int a = ABC.b + d + v;", IntRange(14, 14), "S") shouldBe Pair(14, 16)
        MutationPresentation.getDeleteRange("int a = ABC.b + d;", IntRange(14, 14), "D") shouldBe null
        MutationPresentation.getDeleteRange("int a = ADBC.b  + d;", IntRange(14, 14), "S") shouldBe null
        MutationPresentation.getDeleteRange("int a = (ABC.b + d) + v;", IntRange(15, 15), "F") shouldBe Pair(9, 15)
        MutationPresentation.getDeleteRange("int a = (5 + ABC.b) + d + v;", IntRange(20, 20), "F") shouldBe null

    }
}