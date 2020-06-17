package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import io.jenkins.plugins.gamekins.challenge.Challenge;
import io.jenkins.plugins.gamekins.challenge.DummyChallenge;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameUserProperty extends UserProperty {

    private final HashMap<String, CopyOnWriteArrayList<Challenge>> completedChallenges;
    private final HashMap<String, CopyOnWriteArrayList<Challenge>> currentChallenges;
    private final HashMap<String, CopyOnWriteArrayList<Challenge>> rejectedChallenges;
    private final HashMap<String, String> participation;
    private final HashMap<String, Integer> score;

    public GameUserProperty() {
        this.completedChallenges = new HashMap<>();
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
        this.completedChallenges.putIfAbsent(projectName, new CopyOnWriteArrayList<>());
        this.currentChallenges.putIfAbsent(projectName, new CopyOnWriteArrayList<>());
        this.rejectedChallenges.putIfAbsent(projectName, new CopyOnWriteArrayList<>());
    }

    public void removeParticipation(String projectName) {
        this.participation.remove(projectName);
    }

    public String getTeamName(String projectName) {
        return this.participation.get(projectName);
    }

    public CopyOnWriteArrayList<Challenge> getCompletedChallenges(String projectName) {
        return this.completedChallenges.get(projectName);
    }

    public CopyOnWriteArrayList<Challenge> getCurrentChallenges(String projectName) {
        return this.currentChallenges.get(projectName);
    }

    public CopyOnWriteArrayList<Challenge> getRejectedChallenges(String projectName) {
        return this.rejectedChallenges.get(projectName);
    }

    public void completeChallenge(String projectName, Challenge challenge) {
        CopyOnWriteArrayList<Challenge> challenges;
        if (!(challenge instanceof DummyChallenge)) {
            this.completedChallenges.computeIfAbsent(projectName, k -> new CopyOnWriteArrayList<>());
            challenges = this.completedChallenges.get(projectName);
            challenges.add(challenge);
            this.completedChallenges.put(projectName, challenges);
        }
        challenges = this.currentChallenges.get(projectName);
        challenges.remove(challenge);
        this.currentChallenges.put(projectName, challenges);
    }

    /**
     * Only for debugging purposes
     * @param projectName the name of the project
     */
    public void removeChallenges(String projectName) {
        this.currentChallenges.put(projectName, new CopyOnWriteArrayList<>());
        this.completedChallenges.put(projectName, new CopyOnWriteArrayList<>());
        this.rejectedChallenges.put(projectName, new CopyOnWriteArrayList<>());
    }

    public void newChallenge(String projectName, Challenge challenge) {
        this.currentChallenges.computeIfAbsent(projectName, k -> new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<Challenge> challenges = this.currentChallenges.get(projectName);
        challenges.add(challenge);
        this.currentChallenges.put(projectName, challenges);
    }

    public void rejectChallenge(String projectName, Challenge challenge) {
        this.rejectedChallenges.computeIfAbsent(projectName, k -> new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<Challenge> challenges = this.rejectedChallenges.get(projectName);
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
         * @param user the user who needs the GameUserProperty
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
