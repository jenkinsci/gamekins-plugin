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

package org.gamekins.file

import hudson.FilePath
import org.gamekins.util.GitUtil
import org.gamekins.util.JacocoUtil
import java.io.File
import java.io.Serializable

/**
 * The internal representation of a file received from git.
 *
 * @param constants Constants about the project, needed for generating new challenges after rejection
 * @param filePath Path of the file, starting in the workspace root directory
 * @param workspace Workspace of the project
 *
 * @author Philipp Straubinger
 * @since 0.4
 */
open class FileDetails(val constants: HashMap<String, String>, val filePath: String, workspace: FilePath)
    : Serializable {

    val changedByUsers: HashSet<GitUtil.GameUser> = hashSetOf()
    val file: File
    val fileName: String
    val fileExtension: String
    val packageName: String
    val workspace: String = workspace.remote

    init {
        val pathSplit = filePath.split("/".toRegex())
        //Compute class, package and extension name
        val lastPartOfFile = pathSplit[pathSplit.size - 1]
        fileName = lastPartOfFile.split("\\.".toRegex())[0]
        fileExtension = lastPartOfFile.removePrefix(lastPartOfFile.split("\\.".toRegex())[0] + ".")
        packageName = JacocoUtil.computePackageName(filePath)

        file = if (!this.workspace.endsWith("/") && !filePath.startsWith("/")) {
            File(this.workspace + "/" + filePath)
        } else {
            File(this.workspace + filePath)
        }
    }

    /**
     * Adds a new [user], who has recently changed the class.
     */
    fun addUser(user: GitUtil.GameUser) {
        changedByUsers.add(user)
    }

    /**
     * Check whether the file exists.
     */
    fun filesExists(): Boolean {
        return file.exists()
    }
}