package io.jenkins.plugins.gamekins;

import hudson.model.AbstractProject;
import hudson.model.ProminentProjectAction;
import hudson.model.User;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LeaderboardAction implements ProminentProjectAction {

    private final transient AbstractProject job;

    public LeaderboardAction(AbstractProject job) {
        this.job = job;
    }

    public AbstractProject getJob() {
        return this.job;
    }

    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @Override
    public String getDisplayName() {
        return "Leaderboard";
    }

    @Override
    public String getUrlName() {
        return "leaderboard";
    }

    public List<UserDetails> getUserDetails() {
        ArrayList<UserDetails> details = new ArrayList<>();
        for (User user : User.getAll()) {
            GameUserProperty property = user.getProperty(GameUserProperty.class);
            if (property != null && property.isParticipating()) {
                details.add(
                        new UserDetails(
                                user.getFullName(),
                                property.getTeamName(),
                                property.getScore(),
                                property.getAbsolvedChallenges().size()
                        )
                );
            }
        }
        details.sort(Comparator.comparingInt(UserDetails::getScore));
        return details;
    }

    @ExportedBean(defaultVisibility = 999)
    public class UserDetails {

        private String userName;
        private String teamName;
        private int score;
        private int absolvedChallenges;

        public UserDetails(String userName, String teamName, int score, int absolvedChallenges) {
            this.userName = userName;
            this.teamName = teamName;
            this.score = score;
            this.absolvedChallenges = absolvedChallenges;
        }

        @Exported
        public String getUserName() {
            return this.userName;
        }

        @Exported
        public String getTeamName() {
            return this.teamName;
        }

        @Exported
        public int getScore() {
            return this.score;
        }

        @Exported
        public int getAbsolvedChallenges() {
            return this.absolvedChallenges;
        }
    }
}
