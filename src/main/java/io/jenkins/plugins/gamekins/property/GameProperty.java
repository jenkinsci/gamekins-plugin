package io.jenkins.plugins.gamekins.property;

import hudson.model.AbstractItem;
import io.jenkins.plugins.gamekins.statistics.Statistics;

import java.io.IOException;
import java.util.ArrayList;

public interface GameProperty {

    ArrayList<String> getTeams();

    boolean addTeam(String teamName) throws IOException;

    boolean removeTeam(String teamName) throws IOException;

    Statistics getStatistics();

    AbstractItem getOwner();
}
