package org.gamekins.gumTree

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import org.gamekins.util.Constants
import org.gamekins.util.Constants.Parameters
import java.io.File

/**
 * Static Class for parsing a java class into an abstract syntax tree represented by a compilation unit.
 *
 * @author Michael Gruener
 * @since versionNumber
 */
class JavaParser private constructor(){
    companion object {

        fun parse(sourceFile: String, mutatedClass: String, parameters: Parameters): CompilationUnit {

            synchronized(this) {
                StaticJavaParser.setConfiguration(ParserConfiguration().setAttributeComments(false))
                StaticJavaParser.getParserConfiguration().setLanguageLevel(Constants.JAVA_PARSER_LANGUAGE_LEVEL)
                val combinedSolver = CombinedTypeSolver()
                //ReflectionTypeSolver is used to get the fully qualified name of standard java classes e.g. String
                combinedSolver.add(ReflectionTypeSolver())
                //JavaParserTypeSolver is used to get the fully qualified name in the source folder.
                combinedSolver.add(JavaParserTypeSolver(parameters.remote + "/src/main/java"))
                val symbolSolver = JavaSymbolSolver(combinedSolver)
                StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver)

                var filePath = ""
                if (mutatedClass.contains('.')) filePath = mutatedClass.substringBeforeLast('.').replace('.','/') + '/'
                filePath = parameters.remote + "/src/main/java/$filePath$sourceFile"
                return StaticJavaParser.parse(File(filePath))
            }
        }

        fun parse(sourceCode: String): CompilationUnit {
            StaticJavaParser.getParserConfiguration().setAttributeComments(false)
            StaticJavaParser.getParserConfiguration().setLanguageLevel(Constants.JAVA_PARSER_LANGUAGE_LEVEL)
            return StaticJavaParser.parse(sourceCode)
        }
    }
}