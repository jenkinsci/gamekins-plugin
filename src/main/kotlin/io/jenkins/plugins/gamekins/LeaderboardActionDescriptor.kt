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

@Extension
class LeaderboardActionDescriptor : Descriptor<LeaderboardAction>(LeaderboardAction::class.java) {

    fun doRejectChallenge(@AncestorInPath job: AbstractItem, @QueryParameter reject: String,
                          @QueryParameter reason: String): FormValidation {
        var rejectReason = reason
        if (rejectReason.isEmpty()) return FormValidation.error("Please insert your reason for rejection")
        if (rejectReason.matches(Regex("\\s+"))) rejectReason = "No reason provided"
        val user: User = User.current()
                ?: return FormValidation.error("There is no user signed in")
        val property = user.getProperty(GameUserProperty::class.java)
                ?: return FormValidation.error("Unexpected error")
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
            return FormValidation.error("Unexpected error")
        }
        return FormValidation.ok("Challenge rejected")
    }

    /**
     * Human readable name of this kind of configurable object.
     * Should be overridden for most descriptors, if the display name is visible somehow.
     * As a fallback it uses [Class.getSimpleName] on [.clazz], so for example `MyThing`
     * from `some.pkg.MyThing.DescriptorImpl`.
     * Historically some implementations returned null as a way of hiding the descriptor from the UI,
     * but this is generally managed by an explicit method such as `isEnabled` or `isApplicable`.
     */
    @Nonnull
    override fun getDisplayName(): String {
        return super.getDisplayName()
    }

    init {
        load()
    }
}
