package io.jenkins.plugins.gamekins.util

import hudson.FilePath
import io.jenkins.plugins.gamekins.util.JacocoUtil.FilesOfAllSubDirectoriesCallable
import java.io.IOException

object PublisherUtil {
    @JvmStatic
    fun doCheckJacocoResultsPath(workspace: FilePath, jacocoResultsPath: String): Boolean {
        var jacocoResultsPath = jacocoResultsPath
        if (!jacocoResultsPath.endsWith("/")) jacocoResultsPath += "/"
        if (jacocoResultsPath.startsWith("**")) jacocoResultsPath = jacocoResultsPath.substring(2)
        val files: List<FilePath>
        files = try {
            workspace.act(FilesOfAllSubDirectoriesCallable(workspace, "index.html"))
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } catch (e: InterruptedException) {
            e.printStackTrace()
            return false
        }
        for (file in files) {
            val path = file.remote
            if (path.substring(0, path.length - 10).endsWith(jacocoResultsPath)) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun doCheckJacocoCSVPath(workspace: FilePath, jacocoCSVPath: String): Boolean {
        var jacocoCSVPath = jacocoCSVPath
        if (jacocoCSVPath.startsWith("**")) jacocoCSVPath = jacocoCSVPath.substring(2)
        val split = jacocoCSVPath.split("/".toRegex()).toTypedArray()
        val files: List<FilePath>
        files = try {
            workspace.act(
                    FilesOfAllSubDirectoriesCallable(workspace, split[split.size - 1]))
        } catch (ignored: IOException) {
            return false
        } catch (ignored: InterruptedException) {
            return false
        }
        for (file in files) {
            if (file.remote.endsWith(jacocoCSVPath)) {
                return true
            }
        }
        return false
    }
}
