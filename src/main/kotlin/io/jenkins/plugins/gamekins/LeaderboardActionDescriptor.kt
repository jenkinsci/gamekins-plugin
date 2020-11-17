package io.jenkins.plugins.gamekins

import hudson.Extension
import hudson.model.AbstractItem
import hudson.model.Descriptor
import hudson.util.FormValidation
import io.jenkins.plugins.gamekins.challenge.Challenge
import io.jenkins.plugins.gamekins.util.ActionUtil
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.QueryParameter
import javax.annotation.Nonnull

/**
 * Registers the [LeaderboardAction] to Jenkins as an extension and also works as an communication point between the
 * Jetty server and the [LeaderboardAction]. Actions normally have no [Descriptor], but it can be added. In this case
 * for rejecting Challenges.
 */
@Extension
class LeaderboardActionDescriptor : Descriptor<LeaderboardAction>(LeaderboardAction::class.java) {

    init {
        load()
    }

    /**
     * Rejects a [Challenge] with the String representation [reject] and a [reason].
     */
    fun doRejectChallenge(@AncestorInPath job: AbstractItem, @QueryParameter reject: String,
                          @QueryParameter reason: String): FormValidation {
        return ActionUtil.doRejectChallenge(job, reject, reason)
    }

    @Nonnull
    override fun getDisplayName(): String {
        return super.getDisplayName()
    }
}
