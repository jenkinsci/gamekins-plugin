/*
 * Copyright 2022 Gamekins contributors
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

import org.gamekins.util.Constants

/**
 * The internal representation of other files than [SourceFileDetails] and [TestFileDetails].
 *
 * @param parameters Constants about the project, needed for generating new challenges after rejection
 * @param filePath Path of the file, starting in the workspace root directory
 *
 * @author Philipp Straubinger
 * @since 0.5
 */
class OtherFileDetails(parameters: Constants.Parameters, filePath: String) : FileDetails(parameters, filePath) {

    override fun isTest(): Boolean {
        return false
    }
}