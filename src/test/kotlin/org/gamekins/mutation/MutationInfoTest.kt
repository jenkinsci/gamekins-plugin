package org.gamekins.mutation

import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe

class MutationInfoTest: AnnotationSpec()  {

    @Test
    fun constructorMutationInfo() {
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
        val mutation1 = MutationInfo(mutationDetails, "survived", -1547277781)
        mutation1.result shouldBe "survived"

    }
}