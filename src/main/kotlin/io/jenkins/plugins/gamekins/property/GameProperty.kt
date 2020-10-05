package io.jenkins.plugins.gamekins.property

import hudson.model.AbstractItem
import io.jenkins.plugins.gamekins.statistics.Statistics
import java.io.IOException
import java.util.*

interface GameProperty {

    fun getTeams(): ArrayList<String>

    @Throws(IOException::class)
    fun addTeam(teamName: String)

    @Throws(IOException::class)
    fun removeTeam(teamName: String)

    fun getStatistics(): Statistics

    fun getOwner(): AbstractItem
}
