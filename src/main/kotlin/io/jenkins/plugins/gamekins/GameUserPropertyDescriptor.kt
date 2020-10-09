package io.jenkins.plugins.gamekins

import hudson.Extension
import hudson.model.User
import hudson.model.UserProperty
import hudson.model.UserPropertyDescriptor
import javax.annotation.Nonnull

@Extension
class GameUserPropertyDescriptor : UserPropertyDescriptor(GameUserProperty::class.java) {

    @Nonnull
    override fun getDisplayName(): String {
        return "Gamekins"
    }

    /**
     * Creates a default instance of [UserProperty] to be associated
     * with [User] object that wasn't created from a persisted XML data.
     *
     *
     *
     * See [User] class javadoc for more details about the life cycle
     * of [User] and when this method is invoked.
     *
     * @param user the user who needs the GameUserProperty
     * @return null
     * if the implementation choose not to add any property object for such user.
     */
    override fun newInstance(user: User): UserProperty {
        return GameUserProperty()
    }

    init {
        load()
    }
}
