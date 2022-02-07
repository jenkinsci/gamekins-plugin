/*
 * Copyright 2021 Gamekins contributors
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

/**
 * Contains all related functions to create mutated line of code from mutation information
 *
 * @author Tran Phan
 * @since 0.3
 */

object MutationPresentation {

    /**
     * Entry point for creating mutated line of code
     * For each type of mutation operators, there is a corresponding function to create the mutated line of code
     *
     * MoCo collects mutation information by instrumenting bytecode, since no actual mutated source code is
     * available, Gamekins has to construct the mutated line of code from mutation information.
     */
    fun createMutatedLine(originalLine: String, mutation: MutationInfo, lineOfCode: Int): String {
        // TODO: test the regular expressions more thoroughly and improve they over time so they can detect more cases
        val operatorName = mutation.mutationDetails.mutationOperatorName
        var res = ""
        when (operatorNameToType[operatorName]) {
            "replacement" -> res = getReplacementMutatedLine(originalLine, mutation)
            "insertion" -> res = getInsertionMutatedLine(originalLine, mutation)
            "deletion" -> res = getDeletionMutatedLine(originalLine, mutation)
        }
        if (res == "") return ""
        return "<pre class='prettyprint linenums:${lineOfCode} mt-2'><code class='language-java'>" +
                res + "</code></pre>"
    }

    /**
     * Create mutated line for mutation of replacement operator
     */
    fun getReplacementMutatedLine(originalLine: String, mutation: MutationInfo): String {
        // MoCo use "-" to separate old opcode and new opcode for mutator ID of replacement operator
        // Example: IAND-IOR -> this mutator id denotes replacement of IAND with IOR
        val temp = mutation.mutationDetails.mutatorID.split("-")
        val originalOpcode: String
        val newOpcode: String
        val insOrder = mutation.mutationDetails.instructionsOrder.indexOf(
            mutation.mutationDetails.instructionIndices.joinToString(",")
        )
        // instruction of original opcode not found
        if (insOrder == -1) return ""
        if (!temp.isNullOrEmpty() && temp.size > 1) {
            originalOpcode = temp[0]
            if (!opcodeToRegexPattern.containsKey(originalOpcode)) return ""
            newOpcode = temp[1]
            if (!opcodeToRegexPattern.containsKey(newOpcode)) return ""
        } else return ""

        var count = 0
        val r = Regex(opcodeToRegexPattern[originalOpcode]!!)
        val hits = r.findAll(originalLine).iterator()
        var operatorIndices: IntRange? = null
        while (hits.hasNext()) {
            val hit = hits.next()
            if (count == insOrder) operatorIndices = hit.range
            count++
        }
        if (count != mutation.mutationDetails.instructionsOrder.size || operatorIndices == null) {
            if ((originalOpcode.substring(1) == "ADD" && originalLine.contains("++")) ||
                        (originalOpcode.substring(1) == "SUB" && originalLine.contains("--"))) {
                MutationUtils.mutationBlackList.add(mutation)
            }
            if (((originalOpcode == "IFEQ" || originalOpcode == "IFNE") && originalLine.contains(".equals("))) {
                MutationUtils.mutationBlackList.add(mutation)
            }
            return ""
        }
        // Replacement
        return originalLine.substring(0, operatorIndices.first) +
                getReplacementText(newOpcode) +
                originalLine.substring(operatorIndices.last + 1)
    }

    fun getReplacementText(newOpcode: String): String {
        return when (newOpcode.substring(1)) {
            "AND" -> "&"
            "OR" -> "|"
            "ADD" -> "+"
            "SUB" -> "-"
            "F_ICMPLE", "FLE" -> ">"
            "F_ICMPGE", "FGE" -> "<"
            else -> opcodeToRegexPattern[newOpcode]?.replace("\\", "")!!
        }
    }

    /**
     * Create mutated line for mutation of insertion operator
     */
    fun getInsertionMutatedLine(originalLine: String, mutation: MutationInfo): String {
        // Insertion mutator id format: [OPCODE]-[MutationTYPE]-[I for increment or D for decrement]-[lineNo]
        // Example: GETFIELD-PRUOI-D-51, ILOAD-PRUOI-I-3"
        val temp = mutation.mutationDetails.mutatorID.split("-")
        var reg: Regex? = null
        if (listOf("GETFIELD", "GETSTATIC").contains(temp.getOrNull(0))) {
            val fieldName = mutation.mutationDetails.additionalInfo["fieldName"] ?: return ""
            reg = Regex("((([a-zA-Z_\$][\\w_\$]*)\\.)*($fieldName))")
        } else {
            val varName = mutation.mutationDetails.additionalInfo["varName"] ?: return ""
            reg = Regex(varName)
        }
        val insertOperator = insertionTypeToText[temp.getOrNull(2)] ?: return ""

        val hits = reg.findAll(originalLine).iterator()
        var foundVar: MatchResult? = null
        while (hits.hasNext()) {
            foundVar = hits.next()
        }
        if (foundVar == null) return ""
        if (originalLine.substring(0, foundVar.range.first).all { it.isWhitespace() }) return ""
        val replacement = when (temp[1]) {
            "PRUOI" -> "($insertOperator${foundVar.value})"
            "POUOI" -> "(${foundVar.value}$insertOperator)"
            else -> return ""
        }
        return originalLine.substring(0, foundVar.range.first) +
                replacement +
                originalLine.substring(foundVar.range.last + 1)
    }

    /**
     * Create mutated line for mutation of deletion operator
     */
    fun getDeletionMutatedLine(originalLine: String, mutation: MutationInfo): String {
        // MoCo use "-" to separate old opcode and new opcode for mutator ID
        // Example: IAND-IOR -> this mutator id denotes replacement of IAND with IOR
        val opcode: String
        val position: String
        val temp = mutation.mutationDetails.mutatorID.split("-")
        if (!temp.isNullOrEmpty() && temp.size > 1) {
            if (!opcodeToRegexPattern.containsKey(temp[0])) return ""
            opcode = temp[0]
            position = temp[1]
            if (position != "F" && position != "S") return ""
        } else return ""

        val insOrder = mutation.mutationDetails.instructionsOrder.indexOf(
            mutation.mutationDetails.instructionIndices.joinToString(",")
        )
        // instruction of original opcode not found
        if (insOrder == -1) return ""
        var count = 0
        var textIndices: IntRange? = null
        val r = Regex(opcodeToRegexPattern[opcode]!!)
        val hits = r.findAll(originalLine).iterator()
        while (hits.hasNext()) {
            val hit = hits.next()
            if (count == insOrder) textIndices = hit.range
            count++
        }

        if (count != mutation.mutationDetails.instructionsOrder.size || textIndices == null) {
            if ((opcode.substring(1) == "ADD" && originalLine.contains("++")) ||
                (opcode.substring(1) == "SUB" && originalLine.contains("--"))) {
                MutationUtils.mutationBlackList.add(mutation)
            }
            return ""
        }
        // Deletion
        val deleteRange = getDeleteRange(originalLine, textIndices, position) ?: return ""
        return if (deleteRange.second < originalLine.length) {
            originalLine.substring(0, deleteRange.first).trimEnd() +
                    originalLine.substring(deleteRange.second + 1)
        } else ""
    }

    /**
     * Find the range (start and end index) of the substring to be deleted
     * We need to distinguish between deletion of the first operand and second operand (F or S in mutator ID)
     */
    fun getDeleteRange(originalLine: String, operatorRange: IntRange, removedPosition: String): Pair<Int, Int>? {
        // Reg to find java variable names
        val reg = Regex("((\\+{2}|-{2})?)([\\w\$.])+((\\+{2}|-{2})?)")
        val hits = reg.findAll(originalLine).iterator()
        while (hits.hasNext()) {
            val operand = hits.next()
            val operandRange = operand.range
            var betweenOperatorAndOperand: String? = null
            if (removedPosition == "F") {
                // Remove operand before operator
                if (operandRange.last < operatorRange.first) {
                    betweenOperatorAndOperand =
                        originalLine.substring(operandRange.last + 1, operatorRange.first)
                }
            } else if (removedPosition == "S") {
                if (operandRange.first > operatorRange.last) {
                    betweenOperatorAndOperand =
                        originalLine.substring(operatorRange.last + 1, operandRange.first)
                }
            } else {
                return null
            }
            if (betweenOperatorAndOperand != null && (betweenOperatorAndOperand.isEmpty() ||
                        betweenOperatorAndOperand.all { it.isWhitespace() })
            ) {
                val firstOperandChar = originalLine.getOrNull(operandRange.first)
                val lastOperandIChar = originalLine.getOrNull(operandRange.last)
                val start = if (removedPosition == "F") operandRange.first else operatorRange.last
                val end = if (removedPosition == "F") operatorRange.first else operandRange.last
                return if (firstOperandChar == '(' && lastOperandIChar != ')') {
                    Pair(start + 1, end)
                } else if (firstOperandChar != '(' && lastOperandIChar == ')') {
                    Pair(start, end - 1)
                } else Pair(start, end)
            }
        }
        return null
    }

    val operatorNameToType: Map<String, String> = mapOf(
        "PRUOI" to "insertion", "POUOI" to "insertion",
        "AOD" to "deletion", "BLD" to "deletion",
        "AOR" to "replacement", "BLR" to "replacement", "ROR" to "replacement",
    )

    val insertionTypeToText: Map<String, String> = mapOf(
        "I" to "++", "D" to "--"
    )

    val opcodeToRegexPattern: Map<String, String> = mapOf(
        /**
         * AOR - Arithmetic Operator Replacement
         */
        "IADD" to "(?<!\\+)\\+(?!\\+)", "ISUB" to "(?<!\\-)\\-(?!\\-)",
        "IMUL" to "\\*", "IDIV" to "\\/", "IREM" to "%",

        "LADD" to "(?<!\\+)\\+(?!\\+)", "LSUB" to "(?<!\\-)\\-(?!\\-)",
        "LMUL" to "\\*", "LDIV" to "\\/", "LREM" to "%",

        "FADD" to "(?<!\\+)\\+(?!\\+)", "FSUB" to "(?<!\\-)\\-(?!\\-)",
        "FMUL" to "\\*", "FDIV" to "\\/", "FREM" to "%",

        "DADD" to "(?<!\\+)\\+(?!\\+)", "DSUB" to "(?<!\\-)\\-(?!\\-)",
        "DMUL" to "\\*", "DDIV" to "\\/", "DREM" to "%",
        /**
         * BLR - Bitwise Logical Operator Replacement
         */
        "IAND" to "(?<!&)&(?!&)", "IOR" to "(?<!\\|)\\|(?!\\|)",
        "LAND" to "(?<!&)&(?!&)", "LOR" to "(?<!\\|)\\|(?!\\|)",
        /**
         * ROR - Relational Operator Replacement
         */
        "IFLT" to ">=", "IFLE" to ">(?!=)",
        "IFGT" to "<=", "IFGE" to "<(?!=)",
        "IFEQ" to "!=", "IFNE" to "==",

        "IF_ICMPLT" to ">=", "IF_ICMPLE" to ">(?!=)",
        "IF_ICMPGT" to "<=", "IF_ICMPGE" to "<(?!=)",
        "IF_ICMPEQ" to "!=", "IF_ICMPNE" to "==",

        "IFNULL" to "!=", "IFNONNULL" to "==",
        "IF_ACMPEQ" to "!=", "IF_ACMPNE" to "==",
    )
}