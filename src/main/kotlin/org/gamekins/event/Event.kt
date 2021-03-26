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

/**
 * Abstract event for event handling. Can be directly assigned to a project (and branch for multi branch projects).
 *
 * @author Philipp Straubinger
 * @since 0.3
 */
abstract class Event(val projectName: String, val branch: String, val entryTime: Long = System.currentTimeMillis())
    : Runnable