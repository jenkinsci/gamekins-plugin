package org.gamekins

import hudson.Extension
import hudson.model.RootAction
import hudson.model.User
import hudson.util.FormValidation
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.gamekins.event.EventHandler
import org.gamekins.util.Constants
import org.kohsuke.stapler.QueryParameter
import org.kohsuke.stapler.WebMethod
import org.kohsuke.stapler.json.JsonHttpResponse
import org.kohsuke.stapler.verb.GET
import java.util.concurrent.CopyOnWriteArrayList


@Extension
class CustomAPI : RootAction {

    override fun getIconFileName(): String? {
        return null
    }

    override fun getDisplayName(): String? {
        return null
    }

    override fun getUrlName(): String {
        return "gamekins"
    }

    /**
     * Returns the list of completed achievements.
     */
    @GET
    @WebMethod(name = ["getCompletedAchievements"])
    fun getCompletedAchievements(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = arrayListOf(property.getCompletedAchievements(job))

        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["completedAchievements"] = myJsonObjects

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of completed Challenges by [projectName].
     */
    @GET
    @WebMethod(name = ["getCompletedChallenges"])
    fun getCompletedChallenges(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = arrayListOf(property.getCompletedChallenges(job))

        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["completedChallenges"] = myJsonObjects

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of completed Quests by [projectName].
     */
    @GET
    @WebMethod(name = ["getCompletedQuests"])
    fun getCompletedQuests(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = arrayListOf(property.getCompletedQuests(job))

        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["completedQuests"] = myJsonObjects

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of completed QuestTasks by [projectName].
     */
    @GET
    @WebMethod(name = ["getCompletedQuestTasks"])
    fun getCompletedQuestTasks(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = arrayListOf(property.getCompletedQuestTasks(job))

        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["completedQuestTasks"] = myJsonObjects

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of current Quests by [projectName].
     */
    @GET
    @WebMethod(name = ["getCurrentQuests"])
    fun getCurrentQuests(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = arrayListOf(property.getCurrentQuests(job))

        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["currentQuests"] = myJsonObjects

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of current Challenges by [projectName].
     */
    @GET
    @WebMethod(name = ["getCurrentChallenges"])
    fun getCurrentChallenges(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = arrayListOf(property.getCurrentChallenges(job))
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["currentChallenges"] = myJsonObjects

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of current QuestTasks by [projectName].
     */
    @GET
    @WebMethod(name = ["getCurrentQuestTasks"])
    fun getCurrentQuestTasks(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getCurrentQuestTasks(job))
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["currentQuestTasks"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of rejected Challenges by [projectName].
     */
    @GET
    @WebMethod(name = ["getRejectedChallenges"])
    fun getRejectedChallenges(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getRejectedChallenges(job))
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["rejectedChallenges"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of stored Challenges by [projectName].
     */
    @GET
    @WebMethod(name = ["getStoredChallenges"])
    fun getStoredChallenges(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getStoredChallenges(job))
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["storedChallenges"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of rejected Quests by [projectName].
     */
    @GET
    @WebMethod(name = ["getRejectedQuests"])
    fun getRejectedQuests(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getRejectedQuests(job))
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["rejectedQuests"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the score of the user by [projectName].
     */
    @GET
    @WebMethod(name = ["getScore"])
    fun getScore(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getScore(job))
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["score"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the name of the team the user is participating in the project [projectName].
     */
    @GET
    @WebMethod(name = ["getTeamName"])
    fun getTeamName(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getTeamName(job))
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["teamName"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of unfinished Quests by [projectName].
     */
    @GET
    @WebMethod(name = ["getUnfinishedQuests"])
    fun getUnfinishedQuests(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getUnfinishedQuests(job))
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["unfinishedQuests"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of unsolved achievements.
     */
    @GET
    @WebMethod(name = ["getUnsolvedAchievements"])
    fun getUnsolvedAchievements(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getUnsolvedAchievements(job))
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["unsolvedAchievements"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the filename of the current avatar.
     */
    @GET
    @WebMethod(name = ["getAvatar"])
    fun getCurrentAvatar(): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getCurrentAvatar())
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["currentAvatar"] = myJsonObjects

        return JsonHttpResponse(responseJson, 200)
    }

    @GET
    @WebMethod(name = ["getDisplayName"])
    fun getName(): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getDisplayName())
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["displayName"] = myJsonObjects

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the [gitNames] as String with line breaks after each entry.
     */
    @GET
    @WebMethod(name = ["getNames"])
    fun getNames(): JsonHttpResponse {
        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = arrayListOf(property.getNames())
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["names"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the parent/owner/user of the property.
     */
    @GET
    @WebMethod(name = ["getUser"])
    fun getUser(): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getUser())
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["user"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the parent/owner/user of the property.
     */
    @GET
    @WebMethod(name = ["getTarget"])
    fun getTarget(): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)

        val myJsonObjects = arrayListOf(property.getTarget())
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["target"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    @GET
    @WebMethod(name = ["getEvents"])
    fun getEvents(@QueryParameter("job") job: String): JsonHttpResponse {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)

        val projectEvents = CopyOnWriteArrayList(EventHandler.events)

        projectEvents
            .filter { it.projectName == job}
            .forEach { EventHandler.events.remove(it) }

        val myJsonObjects = arrayListOf(projectEvents)
        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["events"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

   /* @GET
    @WebMethod(name = ["getLeaderboard"])
    fun getLeaderboard(@QueryParameter("job") job: String): JsonHttpResponse {

        return null;
    }*/
}