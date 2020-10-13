package io.jenkins.plugins.gamekins

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
 * @since 1.0
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
