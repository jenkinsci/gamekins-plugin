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

package org.gamekins.event.user

import hudson.model.User
import org.gamekins.challenge.quest.Quest

/**
 * Created when a participant solves one step of a Quest.
 *
 * @author Philipp Straubinger
 * @since 0.4
 */
class QuestStepSolvedEvent(projectName: String, branch: String, user: User, val quest: Quest)
    : UserEvent(projectName, branch, user) {

    val questStep = quest.getLastStep()

    override fun run() = Unit
}