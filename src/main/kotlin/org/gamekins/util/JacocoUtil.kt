/*
 * Copyright 2023 Gamekins contributors
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

package org.gamekins.util

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import jenkins.security.MasterToSlaveCallable
import org.gamekins.file.SourceFileDetails
import org.gamekins.util.Constants.Parameters
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.jsoup.safety.Safelist
import org.jsoup.select.Elements
import java.io.File
import java.io.IOException

import kotlin.jvm.Throws
import kotlin.math.abs
import kotlin.random.Random

/**
 * Util object for interaction with JaCoCo and Jsoup.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
object JacocoUtil {

    /**
     * Calculates the number of covered lines in a JaCoCo class files [document] with a given [modifier].
     *
     * Available modifiers are fc (fully covered), pc (partially covered) and nc (not covered).
     */
    @JvmStatic
    fun calculateCoveredLines(document: Document, modifier: String): Int {
        val elements = document.select("span.$modifier")
        return elements.size
    }

    /**
     * Returns the [FilePath] of a given [file] in the [workspace] for use on a remote machine.
     */
    @JvmStatic
    fun calculateCurrentFilePath(workspace: FilePath, file: File): FilePath {
        return FilePath(workspace.channel, file.absolutePath)
    }

    /**
     * Returns the [FilePath] of a given [file] in the [workspace] for use on a remote machine. Replaces the
     * [oldWorkspace] with a new one in causes where the branch in [WorkflowMultiBranchProject]s has changed and
     * therefore the path to the [workspace].
     */
    @JvmStatic
    fun calculateCurrentFilePath(workspace: FilePath, file: File, oldWorkspace: String): FilePath {
        var oldWorkspacePath = oldWorkspace
        if (!oldWorkspacePath.endsWith("/")) oldWorkspacePath += "/"
        var remote = workspace.remote
        if (!remote.endsWith("/")) remote += "/"
        return FilePath(workspace.channel, file.absolutePath.replace(oldWorkspacePath, remote))
    }

    /**
     * Checks whether the [previous] is a method header and [line] operates on some variable.
     */
    private fun checkMethodHeaderForGetterSetter(previous: String, line: String): Boolean {
        val regex = Regex("[a-zA-Z]+ +(get|set|is)([a-zA-Z_]+)\\(.*\\)")
        val result = regex.find(previous)
        if (result != null) {
            val variable = result.groupValues[2]
            if (line.contains(variable, ignoreCase = true)) return true
        }
        return false
    }

    /**
     * Chooses a random not fully covered line of the given [classDetails]. Returns null if there are no such lines.
     */
    @JvmStatic
    fun chooseRandomLine(classDetails: SourceFileDetails, workspace: FilePath, partially: Boolean = false)
    : Element? {
        val elements = getLines(calculateCurrentFilePath(
                workspace, classDetails.jacocoSourceFile, classDetails.parameters.remote), partially)
        return if (elements.isEmpty()) null else elements[Random.nextInt(elements.size)]
    }

    /**
     * Chooses a random not fully covered method of the given [classDetails]. Returns null if there are no
     * such methods.
     */
    @JvmStatic
    fun chooseRandomMethod(classDetails: SourceFileDetails, workspace: FilePath): CoverageMethod? {
        val methods = getNotFullyCoveredMethodEntries(calculateCurrentFilePath(
                workspace, classDetails.jacocoMethodFile, classDetails.parameters.remote))
        return if (methods.isEmpty()) null else methods[Random.nextInt(methods.size)]
    }

    /**
     * Extracts the package name according to the [shortFilePath] of the class.
     */
    fun computePackageName(shortFilePath: String): String {
        val pathSplit = shortFilePath.split("/".toRegex())
        if (!pathSplit.contains("src")) return ""
        var packageName = StringBuilder()
        for (i in pathSplit.size - 2 downTo 0) {
            if ((pathSplit[i] == "src" || pathSplit[i] == "main" || pathSplit[i] == "java" || pathSplit[i] == "kotlin")
                    && packageName.isNotEmpty()) {
                packageName = StringBuilder(packageName.substring(1))
                break
            }
            packageName.insert(0, "." + pathSplit[i])
        }
        return packageName.toString()
    }

    /**
     * Extracts a [CoverageMethod] from an [element] in a JaCoCo method file.
     */
    private fun extractCoverageMethod(element: Element): CoverageMethod {
        var methodName = ""
        var lines = 0
        var missedLines = 0
        var firstLineID = ""

        for (node in element.childNodes()) {
            for ((key, value) in node.attributes()) {
                if (key == "id") {
                    when {
                        value.matches(Regex("a\\d+")) -> {
                            methodName = node.childNode(0).childNode(0).toString()
                            val temp = node.childNode(0).attributes()
                                .find { it.key == "href" && it.value.matches(Regex(".*#L\\d+")) }
                            firstLineID = temp?.value?.substringAfterLast("#") ?: ""
                        }
                        value.matches(Regex("h\\d+")) -> {
                            missedLines = node.childNode(0).toString().toInt()
                        }
                        value.matches(Regex("i\\d+")) -> {
                            lines = node.childNode(0).toString().toInt()
                        }
                    }
                    break
                }
            }
        }

        return CoverageMethod(methodName, lines, missedLines, firstLineID)
    }

    /**
     * Generates the Jsoup document of a HTML [file].
     */
    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun generateDocument(file: FilePath): Document {
        return Jsoup.parse(file.readToString())
    }

    /**
     * Tries to generate the [Document] representation of the [jacocoSourceFile] if both the [jacocoSourceFile] and
     * the [jacocoCSVFile] exists.
     */
    fun generateDocument(jacocoSourceFile: FilePath, jacocoCSVFile: FilePath, listener: TaskListener): Document? {
        return try {
            if (!jacocoSourceFile.exists() || !jacocoCSVFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCo source file " + jacocoSourceFile.remote
                        + Constants.EXISTS + jacocoSourceFile.exists())
                listener.logger.println("[Gamekins] JaCoCo csv file " + jacocoCSVFile.remote
                        + Constants.EXISTS + jacocoCSVFile.exists())
                return null
            }
            generateDocument(jacocoSourceFile)
        } catch (e: Exception) {
            e.printStackTrace(listener.logger)
            return null
        }
    }

    /**
     * Returns the coverage of a [className] according to the JaCoCo [csv] file of the project.
     */
    @JvmStatic
    fun getCoverageInPercentageFromJacoco(className: String, csv: FilePath): Double {
        try {
            val content = csv.readToString()
            val lines = content.split("\n".toRegex()).filter { line -> line.isNotEmpty() }
            for (coverageLine in lines) {
                val entries = coverageLine.split(",".toRegex())
                if (className.contains(entries[2])) {
                    return entries[4].toDouble() / (entries[3].toDouble() + entries[4].toDouble())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0.0
    }

    @JvmStatic
    fun getCoveredBranches(csv: FilePath): Int {
        val content = csv.readToString()
        val lines = ArrayList(content.split("\n".toRegex()).filter { line -> line.isNotEmpty() })
        lines.removeFirst()
        var coveredLines = 0
        for (coverageLine in lines) {
            val entries = coverageLine.split(",".toRegex())
            coveredLines += entries[6].toInt()
        }

        return coveredLines
    }

    @JvmStatic
    fun getCoveredLines(csv: FilePath): Int {
        val content = csv.readToString()
        val lines = ArrayList(content.split("\n".toRegex()).filter { line -> line.isNotEmpty() })
        lines.removeFirst()
        var coveredLines = 0
        for (coverageLine in lines) {
            val entries = coverageLine.split(",".toRegex())
            coveredLines += entries[8].toInt()
        }

        return coveredLines
    }

    /**
     * Returns all files in a given [directory] including subdirectories that matches a specific [regex].
     */
    @JvmStatic
    fun getFilesInAllSubDirectories(directory: FilePath, regex: String): ArrayList<FilePath> {
        val files = ArrayList<FilePath>()
        try {
            for (path in directory.list()) {
                if (path.isDirectory) {
                    files.addAll(getFilesInAllSubDirectories(path, regex))
                } else {
                    if (path.name.matches(Regex(regex))) files.add(path)
                }
            }
        } catch (ignored: Exception) {
            return ArrayList()
        }
        return files
    }

    /**
     * Similar to [JacocoUtil.calculateCurrentFilePath], only specific to JaCoCo files.
     */
    @JvmStatic
    fun getJacocoFileInMultiBranchProject(run: Run<*, *>, parameters: Parameters,
                                          jacocoFile: FilePath, oldBranch: String): FilePath {
        return if (run.parent.parent is WorkflowMultiBranchProject
                && parameters.branch != oldBranch) {
            FilePath(jacocoFile.channel, jacocoFile.remote.replace(
                    parameters.projectName + "_" + oldBranch,
                    parameters.projectName + "_" + parameters.branch))
        } else {
            jacocoFile
        }
    }

    /**
     * Returns the new line number of a challenges if the code has changed. Returns the original line number if
     * unchanged. Returns the nearest element if it has changed. Trim [originalLine] before passing to this method.
     */
    @JvmStatic
    fun getLineNumberAfterCodeChange(details: SourceFileDetails, originalLine: String, originalLineNumber: Int,
                                     parameters: Parameters, run: Run<*, *>, listener: TaskListener): Int {
        val jacocoSourceFile = getJacocoFileInMultiBranchProject(run, parameters,
            calculateCurrentFilePath(parameters.workspace, details.jacocoSourceFile,
                details.parameters.remote), details.parameters.branch)
        val jacocoCSVFile = getJacocoFileInMultiBranchProject(run, parameters,
            calculateCurrentFilePath(parameters.workspace, details.jacocoCSVFile,
                details.parameters.remote), details.parameters.branch)

        val document = generateDocument(jacocoSourceFile, jacocoCSVFile, listener) ?: return -1

        val elements = document.select("span." + "fc")
        elements.addAll(document.select("span." + "pc"))
        elements.addAll(document.select("span." + "nc"))
        for (element in elements) {
            if (element.html().trim() == originalLine
                && element.attr("id").substring(1).toInt() == originalLineNumber) {
                return originalLineNumber
            }
        }

        elements.removeIf { it.html().trim() != originalLine }

        if (elements.isNotEmpty()) {
            if (elements.size == 1) {
                return elements[0].attr("id").substring(1).toInt()
            } else {
                val nearestElement = elements.minByOrNull {
                    abs(originalLineNumber - it.attr("id").substring(1).toInt()) }
                if (nearestElement != null) {
                    return nearestElement.attr("id").substring(1).toInt()
                }
            }
        }

        return -1
    }

    /**
     * Returns all lines of the [jacocoSourceFile] that does not contain specific identifiers.
     */
    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun getLines(jacocoSourceFile: FilePath, partially: Boolean = false): Elements {
        val document = Jsoup.parse(jacocoSourceFile.readToString())
        val elements = if (partially) {
            document.select("span." + "pc")
        } else {
            document.select("span." + "nc")
        }
        elements.removeIf { e: Element ->
            (e.text().trim() == "{" || e.text().trim() == "}"
                    || e.text().trim() == "(" || e.text().trim() == ")"
                    || e.text().contains("class")
                    || e.text().contains("void")
                    || e.text().contains("public")
                    || e.text().contains("private")
                    || e.text().contains("protected")
                    || e.text().contains("static"))
        }
        elements.removeIf { isGetterOrSetter(jacocoSourceFile.readToString().split("\n"), it.text()) }
        return elements
    }

    /**
     * Returns lines of code within specific lines of code range in the [jacocoSourceFile].
     * The return value is a pair with the first element is the target code snippet and
     * the second element is the target line as normal string. The target line as normal string (cleaned) is needed
     * to create mutated line of code for mutation test challenge
     * [target]: could be Int or String
     */
    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun getLinesInRange(jacocoSourceFile: FilePath, target: Any, linesAround: Int): Pair<String, String> {
        val document = Jsoup.parse(jacocoSourceFile.readToString())
        val lines: List<String> = document.html().lines()
        val outputSettings = Document.OutputSettings()
        outputSettings.prettyPrint(false)
        document.outputSettings(outputSettings)
        document.select("br").before("\\n")
        document.select("p").before("\\n")
        var targetLine = ""
        val lineIndex: Int = when (target) {
            is Int -> {
                if (target < 0) { return Pair("", "") }
                val elem = document.selectFirst("#L$target") ?: return Pair("", "")
                targetLine = elem.toString()
                lines.indexOfFirst { it == elem.toString() }
            }
            is String -> {
                lines.indexOfFirst { it.contains(target) }
            }
            else ->  return Pair("", "")
        }

        if (lineIndex < 0) { return Pair("", "") }

        var res = ""
        val offset = if (linesAround % 2 != 0) 1 else 0
        for (i in (lineIndex - (linesAround/2))..(lineIndex + (linesAround/2 + offset))) {
            val temp = lines.getOrNull(i)
            if (temp != null) {
                res += Jsoup.clean(temp, "", Safelist.none(), outputSettings) + System.lineSeparator()
            }
        }
        val cleanTargetLine = Jsoup.clean(targetLine, "", Safelist.none(), outputSettings)
        return Pair(res, Parser.unescapeEntities(cleanTargetLine, true))
    }

    /**
     * Returns all methods of a given class by their [jacocoMethodFile].
     */
    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun getMethodEntries(jacocoMethodFile: FilePath): ArrayList<CoverageMethod> {
        val elements = generateDocument(jacocoMethodFile).select("tr")
        val methods = ArrayList<CoverageMethod>()

        for (element in elements) {
            if (isMethodEntry(element)) {
                methods.add(extractCoverageMethod(element))
            }
        }
        methods.removeIf { it.methodName.startsWith("lambda$") }

        return methods
    }

    /**
     * Returns all methods of a given class by their [jacocoMethodFile], which are not fully covered.
     */
    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun getNotFullyCoveredMethodEntries(jacocoMethodFile: FilePath): ArrayList<CoverageMethod> {
        val methods = getMethodEntries(jacocoMethodFile)
        methods.removeIf { method: CoverageMethod -> method.missedLines == 0 }
        return methods
    }

    /**
     * Returns the coverage of the project according to the csv file(s) [csvName] in the [workspace].
     */
    @JvmStatic
    fun getProjectCoverage(workspace: FilePath, csvName: String): Double {
        val files: ArrayList<FilePath> = try {
            workspace.act(FilesOfAllSubDirectoriesCallable(workspace, csvName))
        } catch (e: Exception) {
            e.printStackTrace()
            return 0.0
        }

        var instructionCount = 0
        var coveredInstructionCount = 0
        for (file in files) {
            try {
                val content = file.readToString()
                val lines = content.split("\n".toRegex()).filter { line -> line.isNotEmpty() }
                for (coverageLine in lines) {
                    val entries = coverageLine.split(",".toRegex())
                    if (entries[2] != "CLASS") {
                        coveredInstructionCount += entries[4].toDouble().toInt()
                        instructionCount += (entries[3].toDouble() + entries[4].toDouble()).toInt()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return coveredInstructionCount / instructionCount.toDouble()
    }

    /**
     * Checks whether the given [line] is a getter or setter. The ordered list of [lines] must be JaCoCo *.java.html
     * file split by \n. It searches in the list of [lines] for the correct [line] and then goes back in the ordered
     * list one time (or multiple times if the previous line only contains blanks or a {). If the line is a
     * method declaration, it contains get/set/is and the name is equal to the call in the [line], it is a getter
     * or setter.
     */
    fun isGetterOrSetter(lines: List<String>, line: String): Boolean {
        val linesIterator = lines.listIterator()
        while (linesIterator.hasNext()) {
            if (linesIterator.next().contains(line)) {
                while (linesIterator.hasPrevious()) {
                    val previous = linesIterator.previous()
                    if (!previous.contains(line) && previous.isNotBlank() && previous.trim() != "{")  {
                        return checkMethodHeaderForGetterSetter(previous, line)
                    }
                }
            }
        }
        return false
    }

    /**
     * Checks whether the [element] is a method entry in the JaCoCo method file.
     */
    private fun isMethodEntry(element: Element): Boolean {
        for (node in element.childNodes()) {
            for ((key, value) in node.attributes()) {
                if (key == "id" && value.matches(Regex("a\\d+"))) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Replaces special entities with HTML codes. Jenkins does that automatically when saving the current state.
     */
    fun replaceSpecialEntities(string: String): String {
        return string.replace("&&", "&amp;&amp;").replace("& ", "&amp; ")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("\'", "&#39;")
    }

    /**
     * Returns the last changed files of a repository on a remote machine.
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    class FilesOfAllSubDirectoriesCallable(private val directory: FilePath, private val regex: String)
        : MasterToSlaveCallable<ArrayList<FilePath>, IOException?>() {

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        override fun call(): ArrayList<FilePath> {
            return getFilesInAllSubDirectories(directory, regex)
        }
    }

    /**
     * The internal representation of a method from JaCoCo.
     *
     * @author Philipp Straubinger
     * @since 0.1
     */
    class CoverageMethod(val methodName: String, val lines: Int, val missedLines: Int,
                         val firstLineID: String)
}
