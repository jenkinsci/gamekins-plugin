/*
 * Copyright 2020 Gamekins contributors
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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import hudson.model.Action
import hudson.model.User
import hudson.model.UserProperty
import org.gamekins.challenge.Challenge
import org.gamekins.challenge.DummyChallenge
import org.gamekins.statistics.Statistics
import net.sf.json.JSONObject
import org.gamekins.achievement.Achievement
import org.gamekins.util.PropertyUtil
import org.kohsuke.stapler.DataBoundSetter
import org.kohsuke.stapler.QueryParameter
import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.collections.HashMap

/**
 * Property that is added to each [User] to extend his configuration and ability. Stores all current, completed and
 * rejected [Challenge]s, author names in git, score and participation information.
 *
 * @author Philipp Straubinger
 * @since 1.0
 */
class GameUserProperty : UserProperty(), Action {

    private var completedAchievements: HashMap<String, CopyOnWriteArrayList<Achievement>> = HashMap()
    private val completedChallenges: HashMap<String, CopyOnWriteArrayList<Challenge>> = HashMap()
    private val currentChallenges: HashMap<String, CopyOnWriteArrayList<Challenge>> = HashMap()
    private var gitNames: CopyOnWriteArraySet<String>? = null
    private val participation: HashMap<String, String> = HashMap()
    private val pseudonym: UUID = UUID.randomUUID()
    private val rejectedChallenges: HashMap<String, CopyOnWriteArrayList<Pair<Challenge, String>>> = HashMap()
    private val score: HashMap<String, Int> = HashMap()
    private var unsolvedAchievements: HashMap<String, CopyOnWriteArrayList<Achievement>> = HashMap()

    companion object {
        const val TYPE_JSON = "application/json"
    }

    /**
     * Adds an additional [score] to one project [projectName], since one user can participate in multiple projects.
     */
    fun addScore(projectName: String, score: Int) {
        this.score[projectName] = this.score[projectName]!! + score
    }

    /**
     * Sets a [challenge] of project [projectName] to complete and removes it from the [currentChallenges].
     */
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

    /**
     * Computes the initial git names of the user by full, display and user name.
     */
    private fun computeInitialGitNames(): CopyOnWriteArraySet<String> {
        val set = CopyOnWriteArraySet<String>()
        if (user != null) {
            set.add(user.fullName)
            set.add(user.displayName)
            set.add(user.id)
        }
        return set
    }

    /**
     * Returns the number of total [Achievement]s available in [completedAchievements] and [unsolvedAchievements] for
     * a specific [projectName].
     */
    fun doGetAchievementsCount(rsp: StaplerResponse, @QueryParameter projectName: String) {
        rsp.contentType = "text/plain"
        val printer = rsp.writer
        if (projectName.isEmpty() || completedAchievements[projectName] == null
            || unsolvedAchievements[projectName] == null) {
            printer.print(0)
        } else {
            printer.print(completedAchievements[projectName]!!.size + unsolvedAchievements[projectName]!!.size)
        }
        printer.flush()
    }

    /**
     * Returns the list of completed [Achievement]s for a specific [projectName].
     */
    fun doGetCompletedAchievements(rsp: StaplerResponse, @QueryParameter projectName: String) {
        val json = jacksonObjectMapper().writeValueAsString(completedAchievements[projectName])
        rsp.contentType = TYPE_JSON
        val printer = rsp.writer
        printer.print(json)
        printer.flush()
    }

    /**
     * Returns the list of projects the user is currently participating.
     */
    fun doGetProjects(rsp: StaplerResponse) {
        val json = jacksonObjectMapper().writeValueAsString(participation.keys)
        rsp.contentType = TYPE_JSON
        val printer = rsp.writer
        printer.print(json)
        printer.flush()
    }

    /**
     * Returns the list of unsolved [Achievement]s.
     */
    fun doGetUnsolvedAchievements(rsp: StaplerResponse, @QueryParameter projectName: String) {
        val json = jacksonObjectMapper().writeValueAsString(unsolvedAchievements[projectName]?.filter { !it.secret })
        rsp.contentType = TYPE_JSON
        val printer = rsp.writer
        printer.print(json)
        printer.flush()
    }

    /**
     * Returns the list of unsolved secret [Achievement]s for a specific [projectName].
     */
    fun doGetUnsolvedSecretAchievementsCount(rsp: StaplerResponse, @QueryParameter projectName: String) {
        rsp.contentType = "text/plain"
        val printer = rsp.writer
        if (projectName.isEmpty() || unsolvedAchievements[projectName] == null) {
            printer.print(0)
        } else {
            printer.print(unsolvedAchievements[projectName]!!.filter { it.secret }.size)
        }
        printer.flush()
    }

    /**
     * Returns the list of completed Challenges by [projectName].
     */
    fun getCompletedChallenges(projectName: String?): CopyOnWriteArrayList<Challenge> {
        return completedChallenges[projectName]!!
    }

    /**
     * Returns the list of current Challenges by [projectName].
     */
    fun getCurrentChallenges(projectName: String?): CopyOnWriteArrayList<Challenge> {
        return currentChallenges[projectName]!!
    }

    override fun getDisplayName(): String {
        return "Achievements"
    }

    /**
     * Returns the git author name sof the user.
     */
    fun getGitNames(): CopyOnWriteArraySet<String> {
        return gitNames ?: CopyOnWriteArraySet()
    }

    override fun getIconFileName(): String {
        return "/plugin/gamekins/icons/trophy.png"
    }

    /**
     * Returns the [gitNames] as String with line breaks after each entry.
     */
    fun getNames(): String {
        if (user == null) return ""
        if (gitNames == null) gitNames = computeInitialGitNames()
        val builder = StringBuilder()
        for (name in gitNames!!) {
            builder.append(name).append("\n")
        }
        return builder.substring(0, builder.length - 1)
    }

    /**
     * Returns the [pseudonym] of the user for [Statistics].
     */
    fun getPseudonym(): String {
        return pseudonym.toString()
    }

    /**
     * Returns the list of rejected Challenges by [projectName].
     */
    fun getRejectedChallenges(projectName: String?): CopyOnWriteArrayList<Pair<Challenge, String>> {
        return rejectedChallenges[projectName]!!
    }

    /**
     * Returns the score of the user by [projectName].
     */
    fun getScore(projectName: String): Int {
        if (isParticipating(projectName) && score[projectName] == null) {
            score[projectName] = 0
        }
        return score[projectName]!!
    }

    /**
     * Returns the name of the team the user is participating in the project [projectName].
     */
    fun getTeamName(projectName: String): String {
        val name: String? = participation[projectName]
        //Should not happen since each call is of getTeamName() is surrounded with a call to isParticipating()
        return name ?: "null"
    }

    override fun getUrlName(): String {
        return "achievements"
    }

    /**
     * Returns the parent/owner/user of the property.
     */
    fun getUser(): User {
        return user
    }

    /**
     * Checks whether the user is participating in the project [projectName].
     */
    fun isParticipating(projectName: String): Boolean {
        return participation.containsKey(projectName)
    }

    /**
     * Checks whether the user is participating in team [teamName] in the project [projectName].
     */
    fun isParticipating(projectName: String, teamName: String): Boolean {
        return participation[projectName] == teamName
    }

    /**
     * Adds a new [Challenge] to the user.
     */
    fun newChallenge(projectName: String, challenge: Challenge) {
        currentChallenges.computeIfAbsent(projectName) { CopyOnWriteArrayList() }
        val challenges = currentChallenges[projectName]!!
        challenges.add(challenge)
        currentChallenges[projectName] = challenges
    }

    /**
     * Returns the XML representation of the user.
     */
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

    /**
     * Called by Jenkins after the object has been created from his XML representation. Used for data migration.
     */
    @Suppress("unused", "SENSELESS_COMPARISON")
    private fun readResolve(): Any {
        if (completedAchievements == null) completedAchievements = hashMapOf()
        //if (unsolvedAchievements == null) unsolvedAchievements = hashMapOf()
        unsolvedAchievements = hashMapOf()

        if (participation.size != 0) {
            if (completedAchievements.size == 0) {
                for (project in participation.keys) {
                    completedAchievements[project] = CopyOnWriteArrayList()
                }
            }
            if (unsolvedAchievements.size == 0) {
                for (project in participation.keys) {
                    unsolvedAchievements[project] = CopyOnWriteArrayList(GamePublisherDescriptor.achievements)
                }
            }
        }

        participation.keys.forEach { project ->
            GamePublisherDescriptor.achievements
                .filter { !completedAchievements[project]!!.contains(it)
                        && !unsolvedAchievements[project]!!.contains(it) }
                .forEach { unsolvedAchievements[project]!!.add(it) }
        }

        return this
    }

    /**
     * Updates the git names if the user configuration is saved.
     */
    override fun reconfigure(req: StaplerRequest, form: JSONObject?): UserProperty {
        if (form != null) setNames(form.getString("names"))
        return this
    }

    /**
     * Rejects a given [challenge] of project [projectName] with a [reason].
     */
    fun rejectChallenge(projectName: String, challenge: Challenge, reason: String) {
        rejectedChallenges.computeIfAbsent(projectName) { CopyOnWriteArrayList() }
        val challenges = rejectedChallenges[projectName]!!
        challenges.add(Pair(challenge, reason))
        rejectedChallenges[projectName] = challenges
        val currentChallenges = currentChallenges[projectName]!!
        currentChallenges.remove(challenge)
        this.currentChallenges[projectName] = currentChallenges
    }

    /**
     * Removes the participation of the user in project [projectName].
     */
    fun removeParticipation(projectName: String) {
        participation.remove(projectName)
    }

    /**
     * Removes all Challenges for a specific [projectName] and resets the score.
     */
    fun reset(projectName: String) {
        completedChallenges[projectName] = CopyOnWriteArrayList()
        currentChallenges[projectName] = CopyOnWriteArrayList()
        rejectedChallenges[projectName] = CopyOnWriteArrayList()
        score[projectName] = 0
    }

    /**
     * Sets the git names.
     */
    @DataBoundSetter
    fun setNames(names: String) {
        val split = names.split("\n".toRegex())
        gitNames = CopyOnWriteArraySet(split)
    }

    /**
     * Adds the participation of the user for project [projectName] and team [teamName].
     */
    fun setParticipating(projectName: String, teamName: String) {
        participation[projectName] = teamName
        score.putIfAbsent(projectName, 0)
        completedChallenges.putIfAbsent(projectName, CopyOnWriteArrayList())
        currentChallenges.putIfAbsent(projectName, CopyOnWriteArrayList())
        rejectedChallenges.putIfAbsent(projectName, CopyOnWriteArrayList())
        unsolvedAchievements.putIfAbsent(projectName, CopyOnWriteArrayList(GamePublisherDescriptor.achievements))
    }

    /**
     * Sets the user of the property during start of Jenkins and computes the initial git namens.
     */
    override fun setUser(u: User) {
        user = u
        if (gitNames == null) gitNames = computeInitialGitNames()
        if (!PropertyUtil.realUser(user)) unsolvedAchievements = hashMapOf()
    }
}
