package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import io.jenkins.plugins.gamekins.challenge.Challenge;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;

public class GameUserProperty extends UserProperty {

    private ArrayList<Challenge> absolvedChallenges;
    private ArrayList<Challenge> currentChallenges;
    private ArrayList<Challenge> rejectedChallenges;
    private HashMap<String, String> participation;
    private HashMap<String, Integer> score;

    public GameUserProperty() {
        this.absolvedChallenges = new ArrayList<>();
        this.currentChallenges = new ArrayList<>();
        this.rejectedChallenges = new ArrayList<>();
        this.participation = new HashMap<>();
        this.score = new HashMap<>();
    }

    public User getUser() {
        return this.user;
    }

    public int getScore(String projectName) {
        return this.score.get(projectName);
    }

    public void setScore(String projectName, int score) {
        this.score.put(projectName, score);
    }

    public boolean isParticipating(String projectName) {
        return this.participation.containsKey(projectName);
    }

    public boolean isParticipating(String projectName, String teamName) {
        return this.participation.get(projectName).equals(teamName);
    }

    public void setParticipating(String projectName, String teamName) {
        this.participation.put(projectName, teamName);
    }

    public void removeParticipation(String projectName) {
        this.participation.remove(projectName);
    }

    public String getTeamName(String projectName) {
        return this.participation.get(projectName);
    }

    public ArrayList<Challenge> getAbsolvedChallenges() {
        return this.absolvedChallenges;
    }

    public ArrayList<Challenge> getCurrentChallenges() {
        return this.currentChallenges;
    }

    public ArrayList<Challenge> getRejectedChallenges() {
        return this.rejectedChallenges;
    }

    public void absolveChallenge(Challenge challenge) {
        this.absolvedChallenges.add(challenge);
        this.currentChallenges.remove(challenge);
    }

    public void newChallenge(Challenge challenge) {
        this.currentChallenges.add(challenge);
    }

    public void rejectChallenge(Challenge challenge) {
        this.rejectedChallenges.add(challenge);
        this.currentChallenges.remove(challenge);
    }

    @Override
    public UserPropertyDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final GameUserPropertyDescriptor DESCRIPTOR = new GameUserPropertyDescriptor();
    public static class GameUserPropertyDescriptor extends UserPropertyDescriptor {

        public GameUserPropertyDescriptor() {
            super(GameUserProperty.class);
            load();
        }

        /**
         * Creates a default instance of {@link UserProperty} to be associated
         * with {@link User} object that wasn't created from a persisted XML data.
         *
         * <p>
         * See {@link User} class javadoc for more details about the life cycle
         * of {@link User} and when this method is invoked.
         *
         * @param user
         * @return null
         * if the implementation choose not to add any property object for such user.
         */
        @Override
        public UserProperty newInstance(User user) {
            return new GameUserProperty();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Gamekins";
        }
    }
}
