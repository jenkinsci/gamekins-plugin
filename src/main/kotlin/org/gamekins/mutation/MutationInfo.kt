package org.gamekins.mutation

import hudson.FilePath
import org.gamekins.util.JacocoUtil
import org.jsoup.nodes.Element

data class MutationInfo(
    val mutationDetails: MutationDetails,
    val result: String,
    val uniqueID: Int,
) {
    fun getCodeSnippet(classDetails: JacocoUtil.ClassDetails, lineOfCode: Int, workspace: FilePath): String {
        if (lineOfCode < 0) {
            return ""
        }
        if (classDetails.jacocoSourceFile.exists()) {
            val javaHtmlPath = JacocoUtil.calculateCurrentFilePath(
                workspace, classDetails.jacocoSourceFile, classDetails.workspace
            )
            val range = if (lineOfCode > 0) Pair(lineOfCode - 1, lineOfCode + 1) else Pair(lineOfCode, lineOfCode + 2)
            val snippetElements: List<Element>? = JacocoUtil.getLinesInRange(javaHtmlPath, range)
            return snippetElements.toString()
        }
        return ""
    }
}

