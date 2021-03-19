/*
 * Copyright 2020 Gamekins contributors
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

import java.io.Serializable


/**
 * Represents details of a mutation that was collected by MoCo
 *
 * [methodInfo]: a map contains info about mutated method: className, methodName, methodDescription
 * [instructionIndices]: indices of the instructions that were mutated
 * [mutationOperatorName]: operator names that are used in MoCo: AOD, BLD, POUOI, PRUOI, AOR, BLR, ROR
 * AOD: Arithmetic Operator Deletion, BLD: Bitwise Logical Operator Deletion, POUOI: Post- Unary Operator Insertion,
 * PRUOI: Pre- Unary Operator Insertion, AOR: Arithmetic Operator Replacement, BLR: Bitwise Logical Operator Replacement
 * ROR: Relational Operator Replacement
 * [mutatorID]: each mutation in a class has its unique ID (just in that class)
 * [fileName]: file name of mutated class
 * [loc]: line of code
 * [mutationDescription]: textual description of the mutation
 * [instructionsOrder]: This is recorded by MoCo to identify the exact position of the mutated instruction
 * [additionalInfo]: Other info that are necessary to construct mutated line of code such as mutated variable names, etc.
 *
 * @author Tran Phan
 * @since 0.3
 */
data class MutationDetails(
    val methodInfo: Map<String, String>,
    val instructionIndices: List<Int>,
    val mutationOperatorName: String,
    val mutatorID: String,
    val fileName: String,
    val loc: Int,
    val mutationDescription: String,
    val instructionsOrder: List<String>,
    val additionalInfo: Map<String, String>
) : Serializable