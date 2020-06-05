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

    private final HashMap<String, ArrayList<Challenge>> absolvedChallenges;
    private final HashMap<String, ArrayList<Challenge>> currentChallenges;
    private final HashMap<String, ArrayList<Challenge>> rejectedChallenges;
    private final HashMap<String, String> participation;
    private final HashMap<String, Integer> score;

    public GameUserProperty() {
        this.absolvedChallenges = new HashMap<>();
        this.currentChallenges = new HashMap<>();
        this.rejectedChallenges = new HashMap<>();
        this.participation = new HashMap<>();
        this.score = new HashMap<>();
    }

    public User getUser() {
        return this.user;
    }

    public int getScore(String projectName) {
        if (isParticipating(projectName) && this.score.get(projectName) == null) {
            this.score.put(projectName, 0);
        }
        return this.score.get(projectName);
    }

    public void setScore(String projectName, int score) {
        this.score.put(projectName, score);
    }

    public void addScore(String projectName, int score) {
        this.score.put(projectName, this.score.get(projectName) + score);
    }

    public boolean isParticipating(String projectName) {
        return this.participation.containsKey(projectName);
    }

    public boolean isParticipating(String projectName, String teamName) {
        return this.participation.get(projectName).equals(teamName);
    }

    public void setParticipating(String projectName, String teamName) {
        this.participation.put(projectName, teamName);
        this.score.putIfAbsent(projectName, 0);
        this.absolvedChallenges.putIfAbsent(projectName, new ArrayList<>());
        this.currentChallenges.putIfAbsent(projectName, new ArrayList<>());
        this.rejectedChallenges.putIfAbsent(projectName, new ArrayList<>());
    }

    public void removeParticipation(String projectName) {
        this.participation.remove(projectName);
    }

    public String getTeamName(String projectName) {
        return this.participation.get(projectName);
    }

    public ArrayList<Challenge> getAbsolvedChallenges(String projectName) {
        return this.absolvedChallenges.get(projectName);
    }

    public ArrayList<Challenge> getCurrentChallenges(String projectName) {
        return this.currentChallenges.get(projectName);
    }

    public ArrayList<Challenge> getRejectedChallenges(String projectName) {
        return this.rejectedChallenges.get(projectName);
    }

    public void absolveChallenge(String projectName, Challenge challenge) {
        this.absolvedChallenges.computeIfAbsent(projectName, k -> new ArrayList<>());
        ArrayList<Challenge> challenges = this.absolvedChallenges.get(projectName);
        challenges.add(challenge);
        this.absolvedChallenges.put(projectName, challenges);
        challenges = this.currentChallenges.get(projectName);
        challenges.remove(challenge);
        this.currentChallenges.put(projectName, challenges);
    }

    /**
     * Only for debugging purposes
     * @param projectName the name of the project
     */
    public void removeCurrentChallenges(String projectName) {
        this.currentChallenges.put(projectName, new ArrayList<>());
    }

    public void newChallenge(String projectName, Challenge challenge) {
        this.currentChallenges.computeIfAbsent(projectName, k -> new ArrayList<>());
        ArrayList<Challenge> challenges = this.currentChallenges.get(projectName);
        challenges.add(challenge);
        this.currentChallenges.put(projectName, challenges);
    }

    public void rejectChallenge(String projectName, Challenge challenge) {
        this.rejectedChallenges.computeIfAbsent(projectName, k -> new ArrayList<>());
        ArrayList<Challenge> challenges = this.rejectedChallenges.get(projectName);
        challenges.add(challenge);
        this.rejectedChallenges.put(projectName, challenges);
        challenges = this.currentChallenges.get(projectName);
        challenges.remove(challenge);
        this.currentChallenges.put(projectName, challenges);
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
