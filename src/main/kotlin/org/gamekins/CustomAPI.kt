package org.gamekins

import com.google.gson.Gson
import hudson.Extension
import hudson.model.Job
import hudson.model.RootAction
import hudson.model.User
import hudson.util.FormValidation
import jenkins.model.Jenkins
import net.sf.json.JSONArray
import net.sf.json.JSONObject
import net.sf.json.JsonConfig
import net.sf.json.util.CycleDetectionStrategy
import net.sf.json.util.PropertyFilter
import org.gamekins.event.EventHandler
import org.gamekins.util.ActionUtil
import org.gamekins.util.Constants
import org.kohsuke.stapler.QueryParameter
import org.kohsuke.stapler.WebMethod
import org.kohsuke.stapler.json.JsonBody
import org.kohsuke.stapler.json.JsonHttpResponse
import org.kohsuke.stapler.verb.GET
import org.kohsuke.stapler.verb.POST
import java.util.concurrent.CopyOnWriteArrayList

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.coroutines.*

@Extension
class CustomAPI : RootAction {

    private val lJsonConfig = JsonConfig();

    init {
        lJsonConfig.isAllowNonStringKeys = true;
        lJsonConfig.cycleDetectionStrategy = CycleDetectionStrategy.NOPROP
        lJsonConfig.excludes = arrayOf("File")
        lJsonConfig.jsonPropertyFilter = PropertyFilter { _, name, _ -> name ==  "changedByUsers" }
    }

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
        val myJsonObjects = property.getCompletedAchievements(job)

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
        val myJsonObjects = property.getCompletedChallenges(job)

        val responseJson = JSONObject()
        responseJson.accumulate("completedChallenges", myJsonObjects, lJsonConfig)

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of completed Quests by [projectName].
     */
    @GET
    @WebMethod(name = ["getCompletedQuests"])
    fun getCompletedQuests(@QueryParameter("job") job: String): String {

        val user: User = User.current()
            ?: throw FormValidation.error(Constants.Error.NO_USER_SIGNED_IN)
        val property = user.getProperty(GameUserProperty::class.java)
            ?: throw FormValidation.error(Constants.Error.RETRIEVING_PROPERTY)
        val myJsonObjects = property.getCompletedQuests(job)

        return Gson().toJson(myJsonObjects)
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
        val myJsonObjects = property.getCompletedQuestTasks(job)

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
        val myJsonObjects = property.getCurrentQuests(job)

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
        val myJsonObjects = property.getCurrentChallenges(job)

        val responseJson = JSONObject()
        responseJson.accumulate("currentChallenges", myJsonObjects, lJsonConfig)

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

        val myJsonObjects = property.getCurrentQuestTasks(job)
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

        val myJsonObjects = property.getRejectedChallenges(job)

        val responseJson = JSONObject()
        responseJson.accumulate("rejectedChallenges", myJsonObjects, lJsonConfig)

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

        val myJsonObjects = property.getStoredChallenges(job)

        val responseJson = JSONObject()
        responseJson.accumulate("storedChallenges", myJsonObjects, lJsonConfig)

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * stores Challenges by [projectName].
     */
    @POST
    @WebMethod(name = ["storeChallenge"])
    fun storeChallenge(@JsonBody body: StoreChallenge ): JsonHttpResponse {

        val job: Job<*, *> = Jenkins.get().getItemByFullName(body.job) as Job<*, *>;

        val response = JSONObject()
        response["message"] = ActionUtil.doStoreChallenge(job, body.challengeName)
        return JsonHttpResponse(response, 200)
    }

    /**
     * re-stores a Challenges by [projectName].
     */
    @POST
    @WebMethod(name = ["restoreChallenge"])
    fun restoreChallenge(@JsonBody body: StoreChallenge ): JsonHttpResponse {

        val job: Job<*, *> = Jenkins.get().getItemByFullName(body.job) as Job<*, *>;

        val response = JSONObject()
        response["message"] = ActionUtil.doRestoreChallenge(job, body.challengeName)
        return JsonHttpResponse(response, 200)
    }

    /**
     * un-stores a Challenges by [projectName].
     */
    @POST
    @WebMethod(name = ["unshelveChallenge"])
    fun unshelveChallenge(@JsonBody body: StoreChallenge ): JsonHttpResponse {

        val job: Job<*, *> = Jenkins.get().getItemByFullName(body.job) as Job<*, *>;

        val response = JSONObject()
        response["message"] = ActionUtil.doUndoStoreChallenge(job, body.challengeName)
        return JsonHttpResponse(response, 200)
    }

    /**
     * rejects a Challenges by [projectName].
     */
    @POST
    @WebMethod(name = ["rejectChallenge"])
    fun rejectChallenge(@JsonBody body: StoreChallenge ): JsonHttpResponse {

        val job: Job<*, *> = Jenkins.get().getItemByFullName(body.job) as Job<*, *>;

        val response = JSONObject()
        response["message"] = ActionUtil.doRejectChallenge(job, body.challengeName, body.reason)
        return JsonHttpResponse(response, 200)
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

        val myJsonObjects = property.getRejectedQuests(job)
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

        val myJsonObjects = property.getTeamName(job)
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

        val myJsonObjects = property.getUnfinishedQuests(job)
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

        val myJsonObjects = property.getUnsolvedAchievements(job)
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

        val myJsonObjects = property.getCurrentAvatar()
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

        val myJsonObjects = property.getDisplayName()
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
        val myJsonObjects = property.getNames()
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

        val myJsonObjects = property.getUser()
        //val response = JSONArray()
        //myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()

        responseJson["user"] = Gson().toJson(myJsonObjects)
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

    /**
     * Returns the parent/owner/user of the property.
     */
    @GET
    @WebMethod(name = ["getUsers"])
    fun getUsers(@QueryParameter("job") job: String): JsonHttpResponse {

        val lJob: Job<*, *> = Jenkins.get().getItemByFullName(job) as Job<*, *>;
        val myJsonObjects = ActionUtil.getUserDetails(lJob)

        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["users"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the parent/owner/user of the property.
     */
    @GET
    @WebMethod(name = ["getTeams"])
    fun getTeams(@QueryParameter("job") job: String): JsonHttpResponse {

        val lJob: Job<*, *> = Jenkins.get().getItemByFullName(job) as Job<*, *>;
        val myJsonObjects = ActionUtil.getTeamDetails(lJob)

        val response = JSONArray()
        myJsonObjects.forEach { response.add(it) }
        val responseJson = JSONObject()
        responseJson["teams"] = myJsonObjects
        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * Returns the list of completed Challenges by [projectName].
     */
    @GET
    @WebMethod(name = ["getStatistics"])
    fun getStatistics(@QueryParameter("job") job: String): JsonHttpResponse {

        val lJob: Job<*, *> = Jenkins.get().getItemByFullName(job) as Job<*, *>;

        val responseJson = JSONObject()
        responseJson["statistics"] = StatisticsAction(lJob).getStatistics()

        return JsonHttpResponse(responseJson, 200)
    }

    /**
     * startSocket [projectName].
     */
    @POST
    @WebMethod(name = ["startSocket"])
    fun startSocket(): JsonHttpResponse {
        try {
            WebSocketServer.startServer()
        }catch (e: Exception){
            println(e)
        }

        val response = JSONObject()
        return JsonHttpResponse(response, 200)
    }

}