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

package org.gamekins.event.build

import hudson.model.Run
import hudson.model.User
import org.gamekins.GameUserProperty
import org.gamekins.event.EventHandler
import org.gamekins.event.user.*
import org.gamekins.util.MailUtil
import kotlin.collections.ArrayList

/**
 * Created when a build is finished. Sends notification mails to the participants.
 *
 * @author Philipp Straubinger
 * @since 0.3
 */
class BuildFinishedEvent(projectName: String, branch: String, build: Run<*, *>)
    : BuildEvent(projectName, branch, build) {

    override fun run() {
        val events = EventHandler.events
            .filterIsInstance<UserEvent>()
            .filter { it.projectName == this.projectName && it.branch == this.branch }
        val userEvents = hashMapOf<User, ArrayList<UserEvent>>()
        events.forEach {
            var list = userEvents[it.user]
            if (list == null) list = arrayListOf()
            list.add(it)
            userEvents[it.user] = list
        }

        for ((user, list) in userEvents) {
            if (user.getProperty(GameUserProperty::class.java).getNotifications()) {
                MailUtil.sendMail(user, "Gamekins results", "results@gamekins.org", "Gamekins",
                    EventHandler.generateMailText(projectName, build, user, list))
            }
        }

        EventHandler.events.remove(this)
    }
}