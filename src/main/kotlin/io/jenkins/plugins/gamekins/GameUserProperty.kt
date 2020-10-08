package io.jenkins.plugins.gamekins

import hudson.Extension
import hudson.model.User
import hudson.model.UserProperty
import hudson.model.UserPropertyDescriptor
import io.jenkins.plugins.gamekins.challenge.Challenge
import io.jenkins.plugins.gamekins.challenge.DummyChallenge
import net.sf.json.JSONObject
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.StaplerRequest
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import javax.annotation.Nonnull

class GameUserProperty : UserProperty() {

    private val completedChallenges: HashMap<String, CopyOnWriteArrayList<Challenge>> = HashMap()
    private val currentChallenges: HashMap<String, CopyOnWriteArrayList<Challenge>> = HashMap()
    private val rejectedChallenges: HashMap<String, CopyOnWriteArrayList<Pair<Challenge, String>>> = HashMap()
    private val participation: HashMap<String, String> = HashMap()
    private val score: HashMap<String, Int> = HashMap()
    private val pseudonym: UUID = UUID.randomUUID()
    private var gitNames: CopyOnWriteArraySet<String>? = null

    fun getUser(): User {
        return user
    }

    override fun setUser(u: User) {
        user = u
        if (gitNames == null) gitNames = initialGitNames
    }

    @set:DataBoundSetter
    var names: String
        get() {
            if (user == null) return ""
            if (gitNames == null) gitNames = initialGitNames
            val builder = StringBuilder()
            for (name in gitNames!!) {
                builder.append(name).append("\n")
            }
            return builder.substring(0, builder.length - 1)
        }
        set(names) {
            val split = names.split("\n".toRegex())
            gitNames = CopyOnWriteArraySet(split)
        }

    fun getGitNames(): CopyOnWriteArraySet<String> {
        return gitNames ?: CopyOnWriteArraySet()
    }

    fun getPseudonym(): String {
        return pseudonym.toString()
    }

    fun getScore(projectName: String): Int {
        if (isParticipating(projectName) && score[projectName] == null) {
            score[projectName] = 0
        }
        return score[projectName]!!
    }

    fun addScore(projectName: String, score: Int) {
        this.score[projectName] = this.score[projectName]!! + score
    }

    fun isParticipating(projectName: String): Boolean {
        return participation.containsKey(projectName)
    }

    fun isParticipating(projectName: String, teamName: String): Boolean {
        return if (participation[projectName] == null) false else participation[projectName] == teamName
    }

    fun setParticipating(projectName: String, teamName: String) {
        participation[projectName] = teamName
        score.putIfAbsent(projectName, 0)
        completedChallenges.putIfAbsent(projectName, CopyOnWriteArrayList())
        currentChallenges.putIfAbsent(projectName, CopyOnWriteArrayList())
        rejectedChallenges.putIfAbsent(projectName, CopyOnWriteArrayList())
    }

    fun removeParticipation(projectName: String) {
        participation.remove(projectName)
    }

    fun getTeamName(projectName: String): String {
        val name: String? = participation[projectName]
        //Should not happen since each call is of getTeamName() is surrounded with a call to isParticipating()
        return name ?: "null"
    }

    fun getCompletedChallenges(projectName: String?): CopyOnWriteArrayList<Challenge> {
        return completedChallenges[projectName]!!
    }

    fun getCurrentChallenges(projectName: String?): CopyOnWriteArrayList<Challenge> {
        return currentChallenges[projectName]!!
    }

    fun getRejectedChallenges(projectName: String?): CopyOnWriteArrayList<Challenge> {
        val list = CopyOnWriteArrayList<Challenge>()
        rejectedChallenges[projectName]!!.stream().map { obj: Pair<Challenge, String> -> obj.first }.forEach { e: Challenge -> list.add(e) }
        return list
    }

    fun completeChallenge(projectName: String, challenge: Challenge) {
        var challenges: CopyOnWriteArrayList<Challenge>
        if (challenge !is DummyChallenge) {
            completedChallenges.computeIfAbsent(projectName) { CopyOnWriteArrayList() }
            challenges = completedChallenges[projectName]!!
            challenges.add(challenge)
            completedChallenges[projectName] = challenges
        }
        challenges = currentChallenges[projectName]!!
        challenges.remove(challenge)
        currentChallenges[projectName] = challenges
    }

    fun newChallenge(projectName: String, challenge: Challenge) {
        currentChallenges.computeIfAbsent(projectName) { CopyOnWriteArrayList() }
        val challenges = currentChallenges[projectName]!!
        challenges.add(challenge)
        currentChallenges[projectName] = challenges
    }

    fun rejectChallenge(projectName: String, challenge: Challenge, reason: String) {
        rejectedChallenges.computeIfAbsent(projectName) { CopyOnWriteArrayList() }
        val challenges = rejectedChallenges[projectName]!!
        challenges.add(Pair(challenge, reason))
        rejectedChallenges[projectName] = challenges
        val currentChallenges = currentChallenges[projectName]!!
        currentChallenges.remove(challenge)
        this.currentChallenges[projectName] = currentChallenges
    }

    override fun getDescriptor(): UserPropertyDescriptor {
        return DESCRIPTOR
    }

    fun printToXML(projectName: String, indentation: String): String {
        val print = StringBuilder()
        print.append(indentation).append("<User id=\"").append(pseudonym).append("\" project=\"")
                .append(projectName).append("\" score=\"").append(getScore(projectName)).append("\">\n")
        print.append(indentation).append("    <CurrentChallenges count=\"")
                .append(getCurrentChallenges(projectName).size).append("\">\n")
        for (challenge in getCurrentChallenges(projectName)) {
            print.append(challenge.printToXML("", "$indentation        ")).append("\n")
        }
        print.append(indentation).append("    </CurrentChallenges>\n")
        print.append(indentation).append("    <CompletedChallenges count=\"")
                .append(getCompletedChallenges(projectName).size).append("\">\n")
        for (challenge in getCompletedChallenges(projectName)) {
            print.append(challenge.printToXML("", "$indentation        ")).append("\n")
        }
        print.append(indentation).append("    </CompletedChallenges>\n")
        print.append(indentation).append("    <RejectedChallenges count=\"")
                .append(getRejectedChallenges(projectName).size).append("\">\n")
        for (pair in rejectedChallenges[projectName]!!) {
            print.append(pair.first.printToXML(pair.second, "$indentation        ")).append("\n")
        }
        print.append(indentation).append("    </RejectedChallenges>\n")
        print.append(indentation).append("</User>")
        return print.toString()
    }

    private val initialGitNames: CopyOnWriteArraySet<String>
        get() {
            val set = CopyOnWriteArraySet<String>()
            if (user != null) {
                set.add(user.fullName)
                set.add(user.displayName)
                set.add(user.id)
            }
            return set
        }

    override fun reconfigure(req: StaplerRequest, form: JSONObject?): UserProperty? {
        if (form != null) names = form.getString("names")
        return this
    }

    @Extension
    class GameUserPropertyDescriptor : UserPropertyDescriptor(GameUserProperty::class.java) {

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

        @Nonnull
        override fun getDisplayName(): String {
            return "Gamekins"
        }

        init {
            load()
        }
    }

    companion object {
        val DESCRIPTOR = GameUserPropertyDescriptor()
    }

}
