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

package org.gamekins.event

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Handler that stores all events happening in the context of Gamekins. Runs the event after adding it to the list.
 *
 * @author Philipp Straubinger
 * @since 0.3
 */
object EventHandler {

    private val events: ArrayList<Event> = arrayListOf()

    /**
     * Deletes old events, adds a new [event] to the list of [events] and runs it.
     */
    fun addEvent(event: Event) {
        events.removeIf { it.delete }
        events.add(event)
        GlobalScope.launch { event.run() }
    }

    /**
     * Returns a new list of [events].
     */
    fun getEvents(): List<Event> {
        return events.toList()
    }
}