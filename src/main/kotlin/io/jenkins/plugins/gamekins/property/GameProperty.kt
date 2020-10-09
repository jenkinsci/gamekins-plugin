package io.jenkins.plugins.gamekins.property

import hudson.model.AbstractItem
import io.jenkins.plugins.gamekins.statistics.Statistics
import java.io.IOException
import java.util.*

interface GameProperty {

    @Throws(IOException::class)
    fun addTeam(teamName: String)

    fun getOwner(): AbstractItem

    fun getStatistics(): Statistics

    fun getTeams(): ArrayList<String>

    @Throws(IOException::class)
    fun removeTeam(teamName: String)


}
