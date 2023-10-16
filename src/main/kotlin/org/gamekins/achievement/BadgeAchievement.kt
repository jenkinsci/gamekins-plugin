package org.gamekins.achievement

import hudson.model.Run
import hudson.model.TaskListener
import org.gamekins.GameUserProperty
import org.gamekins.LeaderboardAction
import org.gamekins.file.FileDetails
import org.gamekins.util.Constants
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class BadgeAchievement(var badgePaths: List<String>, val lowerBounds: List<Int>,
                       val fullyQualifiedFunctionName: String, val description: String, val title: String,
                       var badgeCounts : MutableList<Int>, val unit: String, var titles: List<String>) {

    @Transient private lateinit var callClass: KClass<out Any>
    @Transient private lateinit var callFunction: KCallable<*>

    init {
        initCalls()
    }

    fun clone(): BadgeAchievement {
        return BadgeAchievement(
            badgePaths.toList(), lowerBounds.toList(),
            fullyQualifiedFunctionName, description, title, badgeCounts.toMutableList(), unit, titles.toMutableList()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is BadgeAchievement) return false
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
        return "$indentation<Achievement title=\"$title\" description=\"$description\" badgeCounts=\"$badgeCounts\"/>"
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
     * Updates progress made and returns true if a badge is earned.
     */
    fun update(
        files: ArrayList<FileDetails>, parameters: Constants.Parameters, run: Run<*, *>,
        property: GameUserProperty, listener: TaskListener = TaskListener.NULL): Boolean {
        val array = arrayOf(callClass.objectInstance, files, parameters, run, property, listener)
        val result: Int = callFunction.call(*array) as Int

        lowerBounds.reversed()

        for (lowerBound in lowerBounds.reversed()) {
            if (result > lowerBound) {
                badgeCounts[lowerBounds.indexOf(lowerBound)]++
                return true
            }
        }
        return false
    }
}