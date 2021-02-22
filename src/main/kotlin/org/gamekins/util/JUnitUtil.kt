package org.gamekins.util

import hudson.FilePath
import hudson.model.Run
import hudson.tasks.junit.TestResultAction
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

object JUnitUtil {

    /**
     * Returns the number of tests of a project in the [workspace] according to the JUnit results.
     */
    @JvmStatic
    fun getTestCount(workspace: FilePath): Int {
        try {
            val files: List<FilePath> = workspace.act(
                JacocoUtil.FilesOfAllSubDirectoriesCallable(workspace, "TEST-.+\\.xml")
            )
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
     * Returns the number of failed tests of a project in the [workspace] according to the JUnit results.
     */
    @JvmStatic
    fun getTestFailCount(workspace: FilePath): Int {
        try {
            val files: List<FilePath> = workspace.act(
                JacocoUtil.FilesOfAllSubDirectoriesCallable(workspace, "TEST-.+\\.xml")
            )
            var testCount = 0
            for (file in files) {
                val document = Jsoup.parse(file.readToString(), "", Parser.xmlParser())
                val elements = document.select("testsuite")
                testCount += elements.first().attr("failures").toInt()
                testCount += elements.first().attr("errors").toInt()
            }
            return testCount
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    /**
     * Returns the number of failed tests of a project run or according to the JUnit results.
     */
    @JvmStatic
    fun getTestFailCount(workspace: FilePath?, run: Run<*, *>?): Int {
        if (run != null) {
            val action = run.getAction(TestResultAction::class.java)
            if (action != null) {
                return action.failCount
            }
        }
        return if (workspace == null) 0 else getTestFailCount(workspace)
    }
}