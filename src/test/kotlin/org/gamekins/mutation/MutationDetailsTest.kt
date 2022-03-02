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

class MutationDetailsTest : AnnotationSpec()  {

    @Test
    fun constructorMutationDetails() {
        val mutationDetails = MutationDetails(
            methodInfo = mapOf("className" to "io/jenkins/plugins/gamekins/challenge/Challenge",
                "methodName" to "foo1",
                "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;"),
            instructionIndices = listOf(14),
            mutationOperatorName = "AOR",
            mutatorID = "IADD-ISUB-51",
            fileName = "ABC.java", 51,
            mutationDescription = "replace integer addition with integer subtraction",
            instructionsOrder = listOf("14"),
            additionalInfo = mapOf("varName" to "numEntering"))
        mutationDetails.fileName shouldBe "ABC.java"
        mutationDetails.methodInfo shouldBe mapOf("className" to "io/jenkins/plugins/gamekins/challenge/Challenge",
            "methodName" to "foo1",
            "methodDescription" to "(Ljava/lang/Integer;)Ljava/lang/Integer;")
        mutationDetails.instructionIndices shouldBe listOf(14)
        mutationDetails.mutationOperatorName shouldBe "AOR"
        mutationDetails.mutatorID shouldBe "IADD-ISUB-51"
        mutationDetails.instructionsOrder shouldBe listOf("14")
        mutationDetails.additionalInfo shouldBe mapOf("varName" to "numEntering")
    }
}