package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import io.jenkins.plugins.gamekins.challenge.Challenge;
import io.jenkins.plugins.gamekins.challenge.DummyChallenge;
import io.jenkins.plugins.gamekins.util.Pair;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameUserProperty extends UserProperty {

    private final HashMap<String, CopyOnWriteArrayList<Challenge>> completedChallenges;
    private final HashMap<String, CopyOnWriteArrayList<Challenge>> currentChallenges;
    private final HashMap<String, CopyOnWriteArrayList<Pair<Challenge, String>>> rejectedChallenges;
    private final HashMap<String, String> participation;
    private final HashMap<String, Integer> score;
    private final UUID pseudonym;
    private HashSet<String> gitNames;

    public GameUserProperty() {
        this.completedChallenges = new HashMap<>();
        this.currentChallenges = new HashMap<>();
        this.rejectedChallenges = new HashMap<>();
        this.participation = new HashMap<>();
        this.score = new HashMap<>();
        this.pseudonym = UUID.randomUUID();
    }

    public User getUser() {
        return this.user;
    }

    @Override
    protected void setUser(User u) {
        this.user = u;
        if (this.gitNames == null) this.gitNames = getInitialGitNames();
    }

    public String getNames() {
        if (this.user == null) return "";
        if (this.gitNames == null) this.gitNames = getInitialGitNames();
        StringBuilder builder = new StringBuilder();
        for (String name : this.gitNames) {
            builder.append(name).append("\n");
        }
        return builder.substring(0, builder.length() - 1);
    }

    @DataBoundSetter
    public void setNames(String names) {
        String[] split = names.split("\n");
        this.gitNames = new HashSet<>(Arrays.asList(split));
    }

    public HashSet<String> getGitNames() {
        if (this.gitNames == null) return new HashSet<>();
        return gitNames;
    }

    public String getPseudonym() {
        return pseudonym.toString();
    }

    public int getScore(String projectName) {
        if (isParticipating(projectName) && this.score.get(projectName) == null) {
            this.score.put(projectName, 0);
        }
        return this.score.get(projectName);
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
        CopyOnWriteArrayList<Challenge> list = new CopyOnWriteArrayList<>();
        this.rejectedChallenges.get(projectName).stream().map(Pair::getFirst).forEach(list::add);
        return list;
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

    public void newChallenge(String projectName, Challenge challenge) {
        this.currentChallenges.computeIfAbsent(projectName, k -> new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<Challenge> challenges = this.currentChallenges.get(projectName);
        challenges.add(challenge);
        this.currentChallenges.put(projectName, challenges);
    }

    public void rejectChallenge(String projectName, Challenge challenge, String reason) {
        this.rejectedChallenges.computeIfAbsent(projectName, k -> new CopyOnWriteArrayList<>());
        CopyOnWriteArrayList<Pair<Challenge, String>> challenges = this.rejectedChallenges.get(projectName);
        challenges.add(new Pair<>(challenge, reason));
        this.rejectedChallenges.put(projectName, challenges);
        CopyOnWriteArrayList<Challenge> currentChallenges = this.currentChallenges.get(projectName);
        currentChallenges.remove(challenge);
        this.currentChallenges.put(projectName, currentChallenges);
    }

    @Override
    public UserPropertyDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public String printToXML(String projectName, String indentation) {
        StringBuilder print = new StringBuilder();
        print.append(indentation).append("<User id=\"").append(this.pseudonym).append("\" project=\"")
                .append(projectName).append("\" score=\"").append(getScore(projectName)).append("\">\n");
        print.append(indentation).append("    <CurrentChallenges count=\"")
                .append(getCompletedChallenges(projectName).size()).append("\">\n");
        for (Challenge challenge : getCurrentChallenges(projectName)) {
            print.append(challenge.printToXML("", indentation + "        ")).append("\n");
        }
        print.append(indentation).append("    </CurrentChallenges>\n");
        print.append(indentation).append("    <CompletedChallenges count=\"")
                .append(getCompletedChallenges(projectName).size()).append("\">\n");
        for (Challenge challenge : getCompletedChallenges(projectName)) {
            print.append(challenge.printToXML("", indentation + "        ")).append("\n");
        }
        print.append(indentation).append("    </CompletedChallenges>\n");
        print.append(indentation).append("    <RejectedChallenges count=\"")
                .append(getRejectedChallenges(projectName).size()).append("\">\n");
        for (Pair<Challenge, String> pair : this.rejectedChallenges.get(projectName)) {
            print.append(pair.getFirst().printToXML(pair.getSecond(),indentation + "        ")).append("\n");
        }
        print.append(indentation).append("    </RejectedChallenges>\n");
        print.append(indentation).append("</User>");
        return print.toString();
    }

    private HashSet<String> getInitialGitNames() {
        HashSet<String> set = new HashSet<>();
        if (this.user != null) {
            set.add(this.user.getFullName());
            set.add(this.user.getDisplayName());
            set.add(this.user.getId());
        }
        return set;
    }

    @Override
    public UserProperty reconfigure(StaplerRequest req, JSONObject form) {
        if (form != null) this.setNames(form.getString("names"));
        return this;
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
