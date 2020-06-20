package io.jenkins.plugins.gamekins.property;

import java.io.IOException;
import java.util.ArrayList;

public interface GameProperty {

    ArrayList<String> getTeams();

    boolean addTeam(String teamName) throws IOException;

    boolean removeTeam(String teamName) throws IOException;
}
