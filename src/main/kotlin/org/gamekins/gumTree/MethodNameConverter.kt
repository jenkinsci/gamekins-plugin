package org.gamekins.gumTree

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.type.ArrayType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.UnsolvedSymbolException
import java.util.*

/**
 * Converts a methodDeclaration into it´s method description.
 * e.g. void foo(int[] ints, String str) into ([ILjava/lang/String;)V
 *
 * @author Michael Gruener
 * @since versionNumber
 */
class MethodNameConverter {

    /**
     * Returns the method-description as a string for the given methodDeclaration or null if it was not possible to determine .
     */
    fun getByteCodeRepresentation(methodDeclaration: MethodDeclaration): String? {
        val stringBuilder = StringBuilder()
        stringBuilder.append('(')

        for (parameter in methodDeclaration.parameters) {
            if (!convertType(parameter.type, parameter.isVarArgs, stringBuilder)) return null
        }
        stringBuilder.append(')')
        if (!convertType(methodDeclaration.type, false, stringBuilder)) return null

        return stringBuilder.toString()
    }

    /**
     * Converts the different types of arguments into their bytecode representation.
     * Returns false if it was not possible to resolve the type with the symbol-solver, true otherwise.
     */
    private fun convertType(type: Type, isVarArgs: Boolean, stringBuilder: StringBuilder): Boolean {
        if (isVarArgs) {
            stringBuilder.append('[')
        }
        if (type.isPrimitiveType) {
            stringBuilder.append(convertPrimitiveType(type.resolve().asPrimitive().name))
        }
        else if (type.isVoidType) {
            stringBuilder.append("V")
        }
        else if (type.isArrayType) {
            stringBuilder.append('[')
            convertType((type as ArrayType).componentType, false, stringBuilder)
        }
        else if (type.isUnionType) {
            return false
        }
        else if (type.isReferenceType) {
            stringBuilder.append('L')
            try {
                stringBuilder.append(type.resolve().asReferenceType().qualifiedName.replace('.', '/'))
            } catch (e: UnsolvedSymbolException) {
                //Try to resolve dependency
                val fullyQualifiedName = resolveReferenceWithImports(type.toString().replace(Regex("<.*?>"), ""), type.findCompilationUnit())
                if (fullyQualifiedName == null) {
                    return false
                } else {
                    stringBuilder.append(fullyQualifiedName)
                }
            }
            stringBuilder.append(';')
        }
        return true
    }

    /**
     * Tries to resolve the fully-qualified name with the imports of the class.
     */
    private fun resolveReferenceWithImports(name: String, compilationUnit: Optional<CompilationUnit>): String? {
        if (compilationUnit.isEmpty) return null

        val imports = compilationUnit.get().imports
        for (import in imports) {
            if (import.nameAsString.endsWith(name)) return import.nameAsString.replace('.', '/')
        }
        return null
    }

    /**
     * Returns the bytecode representation of the primitive type.
     */
    private fun convertPrimitiveType(type: String): String {
        return when (type) {
            "INT" -> "I"
            "BOOLEAN" -> "Z"
            "BYTE" -> "B"
            "CHAR" -> "C"
            "LONG" -> "J"
            "FLOAT" -> "F"
            "DOUBLE" -> "D"
            else -> type
        }
    }
}