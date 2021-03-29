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

package org.gamekins

import hudson.Extension
import hudson.model.User
import hudson.model.UserProperty
import hudson.model.UserPropertyDescriptor
import javax.annotation.Nonnull

/**
 * Registers the [GameUserProperty] to Jenkins as an extension and also works as an communication point between the
 * Jetty server and the [GameUserProperty].
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
@Extension
class GameUserPropertyDescriptor : UserPropertyDescriptor(GameUserProperty::class.java) {

    init {
        load()
    }

    @Nonnull
    override fun getDisplayName(): String {
        return "Gamekins"
    }

    override fun newInstance(user: User): UserProperty {
        return GameUserProperty()
    }
}
