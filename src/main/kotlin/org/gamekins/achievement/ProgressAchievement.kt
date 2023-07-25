package org.gamekins.achievement

import org.gamekins.LeaderboardAction
import java.util.HashMap
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class ProgressAchievement(var badgePaths: List<String>, var unsolvedBadgePaths: List<String>, val milestones: List<Int>,
                          val fullyQualifiedFunctionName: String, val description: String, val title: String,
                          var progress : Int, val additionalParameters: HashMap<String, String>) {

    @Transient private lateinit var callClass: KClass<out Any>
    @Transient private lateinit var callFunction: KCallable<*>

    init {
        initCalls()
    }

    fun clone(): ProgressAchievement {
        return clone(this)
    }

    fun clone(ach: ProgressAchievement): ProgressAchievement {
        val achievement = ProgressAchievement(badgePaths.toList(), unsolvedBadgePaths.toList(), milestones.toList(),
            fullyQualifiedFunctionName, description, title, progress, additionalParameters)
        return achievement
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is ProgressAchievement) return false
        return other.description == this.description && other.title == this.title
    }

    override fun hashCode(): Int {
        var result = badgePaths.hashCode()
        result = 31 * result + fullyQualifiedFunctionName.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + callClass.hashCode()
        result = 31 * result + callFunction.hashCode()
        return result
    }

    /**
     * Initializes the [callClass] and the [callFunction], which are both transient. The reason is that Kotlin classes
     * are not on the white list for serialisation by Jenkins. All of the needed classes could be added manually, but
     * that is not feasible for reflection types.
     */
    private fun initCalls() {
        val reference = fullyQualifiedFunctionName.split("::")
        callClass = Class.forName(reference[0]).kotlin
        callFunction = callClass.members.single { it.name == reference[1] }
    }

    /**
     * Returns the String representation of the [Achievement] for the [LeaderboardAction].
     */
    fun printToXML(indentation: String): String {
        return "$indentation<Achievement title=\"$title\" description=\"$description\" progress=\"$progress\"/>"
    }

    /**
     * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
     */
    @Suppress("unused")
    private fun readResolve(): Any {
        initCalls()
        return this
    }

    override fun toString(): String {
        return "$title: $description"
    }

    /**
     * Sets a new [badgePaths] and/or [unsolvedBadgePaths].
     */
    fun updateBadgePaths(badgePaths: List<String> = this.badgePaths, unsolvedBadgePaths: List<String> = this.unsolvedBadgePaths) {
        this.badgePaths = badgePaths
        this.unsolvedBadgePaths = unsolvedBadgePaths
    }

    /**
     * Updates progress made and returns true if a milestone is reached.
     */
    fun progress(): Boolean {
        return false
    }
}