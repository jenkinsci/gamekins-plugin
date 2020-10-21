package io.jenkins.plugins.gamekins.util

import hudson.FilePath
import hudson.model.Run
import hudson.model.TaskListener
import hudson.tasks.junit.TestResultAction
import io.jenkins.plugins.gamekins.util.GitUtil.GameUser
import jenkins.security.MasterToSlaveCallable
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.jsoup.select.Elements
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.util.*
import kotlin.jvm.Throws

/**
 * Util object for interaction with JaCoCo and Jsoup.
 *
 * @author Philipp Straubinger
 * @since 1.0
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
     * Extracts the package name according to the [shortFilePath] of the class.
     */
    fun computePackageName(shortFilePath: String): String {
        val pathSplit = shortFilePath.split("/".toRegex())
        var packageName = StringBuilder()
        for (i in pathSplit.size - 2 downTo 0) {
            if ((pathSplit[i] == "src" || pathSplit[i] == "main" || pathSplit[i] == "java")
                    && packageName.isNotEmpty()) {
                packageName = StringBuilder(packageName.substring(1))
                break
            }
            packageName.insert(0, "." + pathSplit[i])
        }
        return packageName.toString()
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
     * Returns the coverage of a [className] according to the JaCoCo [csv] file of the project.
     */
    @JvmStatic
    fun getCoverageInPercentageFromJacoco(className: String, csv: FilePath): Double {
        try {
            val content = csv.readToString()
            val lines = content.split("\n".toRegex()).filter { line -> line.isNotEmpty() }
            for (coverageLine in lines) {
                //TODO: Improve
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
    fun getJacocoFileInMultiBranchProject(run: Run<*, *>, constants: HashMap<String, String>,
                                          jacocoFile: FilePath, oldBranch: String): FilePath {
        return if (run.getParent().getParent() is WorkflowMultiBranchProject
                && constants["branch"] != oldBranch) {
            FilePath(jacocoFile.channel, jacocoFile.remote.replace(
                    constants["projectName"].toString() + "_" + oldBranch,
                    constants["projectName"].toString() + "_" + constants["branch"]))
        } else {
            jacocoFile
        }
    }

    /**
     * Returns all lines of the [jacocoSourceFile] that does not contain specific identifiers.
     */
    @JvmStatic
    @Throws(IOException::class, InterruptedException::class)
    fun getLines(jacocoSourceFile: FilePath): Elements {
        val document = Jsoup.parse(jacocoSourceFile.readToString())
        val elements = document.select("span." + "pc")
        elements.addAll(document.select("span." + "nc"))
        elements.removeIf { e: Element ->
            (e.text().contains("{")
                    || e.text().contains("}")
                    || e.text().contains("class")
                    || e.text().contains("void")
                    || e.text().contains("public")
                    || e.text().contains("private")
                    || e.text().contains("protected")
                    || e.text().contains("static")
                    || e.text() == "(" || e.text() == ")")
        }
        return elements
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
            var matches = false
            for (node in element.childNodes()) {
                for ((key, value) in node.attributes()) {
                    if (key == "id" && value.matches(Regex("a\\d+"))) {
                        matches = true
                        break
                    }
                }
                if (matches) break
            }

            if (matches) {
                var methodName = ""
                var lines = 0
                var missedLines = 0
                for (node in element.childNodes()) {
                    for ((key, value) in node.attributes()) {
                        if (key == "id") {
                            when {
                                value.matches(Regex("a\\d+")) -> {
                                    methodName = node.childNode(0).childNode(0).toString()
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
                methods.add(CoverageMethod(methodName, lines, missedLines))
            }
        }
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
        val files: ArrayList<FilePath>
        files = try {
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
                    //TODO: Improve
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
     * Returns the number of tests of a project in the [workspace] according to the JUnit results.
     */
    @JvmStatic
    fun getTestCount(workspace: FilePath): Int {
        try {
            val files: List<FilePath> = workspace.act(
                    FilesOfAllSubDirectoriesCallable(workspace, "TEST-.+\\.xml"))
            var testCount = 0
            for (file in files) {
                val document = Jsoup.parse(file.readToString(), "", Parser.xmlParser())
                val elements = document.select("testsuite")
                testCount += elements.first().attr("tests").toInt()
            }
            return testCount
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * Returns the number of tests of a project run or according to the JUnit results.
     */
    @JvmStatic
    fun getTestCount(workspace: FilePath?, run: Run<*, *>?): Int {
        if (run != null) {
            val action = run.getAction(TestResultAction::class.java)
            if (action != null) {
                return action.totalCount
            }
        }
        return if (workspace == null) 0 else getTestCount(workspace)
    }

    /**
     * Returns the last changed files of a repository on a remote machine.
     *
     * @author Philipp Straubinger
     * @since 1.0
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
     * @since 1.0
     */
    class CoverageMethod internal constructor(val methodName: String?, val lines: Int, val missedLines: Int)

    /**
     * The internal representation of a class from JaCoCo.
     *
     * @param workspace Workspace of the project
     * @param shortFilePath Path of the file, starting in the workspace root directory
     * @param shortJacocoPath Path of the JaCoCo root directory, beginning with ** / (without space)
     * @param shortJacocoCSVPath Path of the JaCoCo csv file, beginning with ** / (without space)
     *
     * @author Philipp Straubinger
     * @since 1.0
     */
    class ClassDetails(workspace: FilePath,
                       shortFilePath: String,
                       shortJacocoPath: String,
                       shortJacocoCSVPath: String,
                       listener: TaskListener)
        : Serializable {

        val className: String
        val extension: String
        val packageName: String
        val jacocoMethodFile: File
        val jacocoSourceFile: File
        val jacocoCSVFile: File
        val coverage: Double
        val changedByUsers: HashSet<GameUser>
        val workspace: String = workspace.remote

        init {
            val pathSplit = shortFilePath.split("/".toRegex())
            //Compute class, package and extension name
            className = pathSplit[pathSplit.size - 1].split("\\.".toRegex())[0]
            this.extension = pathSplit[pathSplit.size - 1].split("\\.".toRegex())[1]
            packageName = computePackageName(shortFilePath)

            //Build the paths to the JaCoCo files
            val jacocoPath = StringBuilder(workspace.remote)
            var i = 0
            while (pathSplit[i] != "src") {
                if (pathSplit[i].isNotEmpty()) jacocoPath.append("/").append(pathSplit[i])
                i++
            }
            jacocoCSVFile = File(jacocoPath.toString() + shortJacocoCSVPath.substring(2))
            if (!jacocoCSVFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCoCSVPath: " + jacocoCSVFile.absolutePath
                        + " exists " + jacocoCSVFile.exists())
            }

            jacocoPath.append(shortJacocoPath.substring(2))
            if (!jacocoPath.toString().endsWith("/")) jacocoPath.append("/")
            jacocoPath.append(packageName).append("/")
            jacocoMethodFile = File("$jacocoPath$className.html")
            if (!jacocoMethodFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCoMethodPath: "
                        + jacocoMethodFile.absolutePath + " exists " + jacocoMethodFile.exists())
            }

            jacocoSourceFile = File(jacocoPath.toString() + className + "." + this.extension + ".html")
            if (!jacocoSourceFile.exists()) {
                listener.logger.println("[Gamekins] JaCoCoSourcePath: "
                        + jacocoSourceFile.absolutePath + " exists " + jacocoSourceFile.exists())
            }

            coverage = getCoverageInPercentageFromJacoco(className,
                    calculateCurrentFilePath(workspace, jacocoCSVFile))
            changedByUsers = HashSet()
        }

        /**
         * Adds a new [user], who has recently changed the class.
         */
        fun addUser(user: GameUser) {
            changedByUsers.add(user)
        }

        /**
         * Check whether all JaCoCo files are existing, which is not always the case.
         */
        fun filesExists(): Boolean {
            return jacocoCSVFile.exists() && jacocoSourceFile.exists() && jacocoMethodFile.exists()
        }

        override fun toString(): String {
            var value = StringBuilder("ClassDetails{" +
                    "className='" + className + '\'' +
                    ", extension='" + extension + '\'' +
                    ", packageName='" + packageName + '\'' +
                    ", changedByUsers=")
            for (user in changedByUsers) {
                value.append(user.fullName).append(",")
            }
            value = StringBuilder(value.substring(0, value.length - 1))
            value.append('}')
            return value.toString()
        }
    }
}
