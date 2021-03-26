/*
 * Copyright 2020 Gamekins contributors
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

package org.gamekins.event.build

import hudson.model.Run
import org.gamekins.event.EventHandler

/**
 * Created when a build is started. Deletes all old events associated with the project and branch.
 *
 * @author Philipp Straubinger
 * @since 0.3
 */
class BuildStartedEvent(projectName: String, branch: String, build: Run<*, *>): BuildEvent(projectName, branch, build) {

    override fun run() {
        EventHandler.events
            .filter { it.projectName == projectName && it.branch == branch && it != this }
            .forEach { EventHandler.events.remove(it) }
    }
}