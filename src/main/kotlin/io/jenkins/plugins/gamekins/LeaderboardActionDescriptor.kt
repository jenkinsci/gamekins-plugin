package io.jenkins.plugins.gamekins

import hudson.Extension
import hudson.model.AbstractItem
import hudson.model.Descriptor
import hudson.model.User
import hudson.util.FormValidation
import io.jenkins.plugins.gamekins.challenge.Challenge
import org.kohsuke.stapler.AncestorInPath
import org.kohsuke.stapler.QueryParameter
import java.io.IOException
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
        var rejectReason = reason
        if (rejectReason.isEmpty()) return FormValidation.error("Please insert your reason for rejection")
        if (rejectReason.matches(Regex("\\s+"))) rejectReason = "No reason provided"

        val user: User = User.current()
                ?: return FormValidation.error("There is no user signed in")
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return FormValidation.error("Unexpected error while retrieving the property")

        val projectName = job.name
        var challenge: Challenge? = null
        for (chal in property.getCurrentChallenges(projectName)) {
            if (chal.toString() == reject) {
                challenge = chal
                break
            }
        }

        if (challenge == null) return FormValidation.error("The challenge does not exist")
        property.rejectChallenge(projectName, challenge, rejectReason)

        try {
            user.save()
        } catch (e: IOException) {
            e.printStackTrace()
            return FormValidation.error("Unexpected error while saving")
        }

        return FormValidation.ok("Challenge rejected")
    }

    @Nonnull
    override fun getDisplayName(): String {
        return super.getDisplayName()
    }
}
