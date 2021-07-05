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
import hudson.model.AbstractItem
import hudson.model.Descriptor
import hudson.util.FormValidation
import org.gamekins.challenge.Challenge
import org.gamekins.challenge.quest.Quest
import org.gamekins.util.ActionUtil
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.QueryParameter
import javax.annotation.Nonnull

/**
 * Registers the [LeaderboardAction] to Jenkins as an extension and also works as an communication point between the
 * Jetty server and the [LeaderboardAction]. Actions normally have no [Descriptor], but it can be added. In this case
 * for rejecting Challenges.
 *
 * @author Philipp Straubinger
 * @since 0.1
 */
@Extension
class LeaderboardActionDescriptor : Descriptor<LeaderboardAction>(LeaderboardAction::class.java) {

    /**
     * Rejects a [Challenge] with the String representation [reject] and a [reason].
     */
    fun doRejectChallenge(@AncestorInPath job: AbstractItem, @QueryParameter reject: String,
                          @QueryParameter reason: String): FormValidation {
        return ActionUtil.doRejectChallenge(job, reject, reason)
    }

    /**
     * Rejects a [Quest] with the String representation [reject] and a [reason].
     */
    fun doRejectQuest(@AncestorInPath job: AbstractItem, @QueryParameter reject: String,
                          @QueryParameter reason: String): FormValidation {
        return ActionUtil.doRejectQuest(job, reject, reason)
    }

    @Nonnull
    override fun getDisplayName(): String {
        return super.getDisplayName()
    }
}
