package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import io.jenkins.plugins.gamekins.challenge.Challenge;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class GameUserProperty extends UserProperty {

    //TODO: Improve for multi-project participation
    private int score;
    private boolean participating;
    private String teamName;
    private ArrayList<Challenge> absolvedChallenges;
    private ArrayList<Challenge> currentChallenges;
    private ArrayList<Challenge> rejectedChallenges;

    public GameUserProperty() {
        this.score = 0;
        this.participating = false;
        this.teamName = "";
        this.absolvedChallenges = new ArrayList<>();
        this.currentChallenges = new ArrayList<>();
        this.rejectedChallenges = new ArrayList<>();
    }

    public User getUser() {
        return this.user;
    }

    public int getScore() {
        return this.score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isParticipating() {
        return this.participating;
    }

    public void setParticipating(boolean participating) {
        this.participating = participating;
    }

    public String getTeamName() {
        return this.teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
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
