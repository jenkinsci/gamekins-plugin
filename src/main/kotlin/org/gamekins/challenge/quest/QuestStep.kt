/*
 * Copyright 2023 Gamekins contributors
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

package org.gamekins.challenge.quest

import org.gamekins.challenge.Challenge

/**
 * One step of a Quest, containing an optional description and a challenge.
 *
 * @author Philipp Straubinger
 * @since 0.4
 */
data class QuestStep(val description: String, val challenge: Challenge) {

    /**
     * Returns the XML representation of the quest step.
     */
    fun printToXML(indentation: String): String {
        var print = "$indentation<QuestStep description=\"$description\">\n"
        print += challenge.printToXML("", "$indentation    ")
        print += "\n$indentation</QuestStep>"
        return print
    }

    override fun toString(): String {
        return description.ifEmpty { challenge.toEscapedString() }
    }
}