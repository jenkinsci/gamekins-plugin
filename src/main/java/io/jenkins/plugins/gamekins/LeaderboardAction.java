package io.jenkins.plugins.gamekins;

import hudson.model.AbstractProject;
import hudson.model.ProminentProjectAction;
import hudson.model.User;
import io.jenkins.plugins.gamekins.challenge.Challenge;
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
            if (property != null && property.isParticipating(job.getName())) {
                details.add(
                        new UserDetails(
                                user.getFullName(),
                                property.getTeamName(job.getName()),
                                property.getScore(job.getName()),
                                property.getAbsolvedChallenges(job.getName()).size()
                        )
                );
            }
        }
        details.sort(Comparator.comparingInt(UserDetails::getScore));
        return details;
    }

    public List<TeamDetails> getTeamDetails() {
        ArrayList<TeamDetails> details = new ArrayList<>();
        for (User user : User.getAll()) {
            GameUserProperty property = user.getProperty(GameUserProperty.class);
            if (property != null && property.isParticipating(job.getName())) {
                int index = -1;
                for (int i = 0; i < details.size(); i++) {
                    TeamDetails teamDetail = details.get(i);
                    if (teamDetail.getTeamName().equals(property.getTeamName(job.getName()))) {
                        index = i;
                    }
                }
                if (index != -1) {
                    details.get(index).addAbsolvedChallenges(property.getAbsolvedChallenges(job.getName()).size());
                    details.get(index).addScore(property.getScore(job.getName()));
                } else {
                    details.add(
                            new TeamDetails(
                                    property.getTeamName(job.getName()),
                                    property.getScore(job.getName()),
                                    property.getAbsolvedChallenges(job.getName()).size()
                            )
                    );
                }
            }
        }
        details.sort(Comparator.comparingInt(TeamDetails::getScore));
        return details;
    }

    public ArrayList<Challenge> getAbsolvedChallenges() {
        User user = User.current();
        if (user == null) return new ArrayList<>();
        GameUserProperty property = user.getProperty(GameUserProperty.class);
        if (property == null) return new ArrayList<>();
        return property.getAbsolvedChallenges(job.getName());
    }

    public ArrayList<Challenge> getCurrentChallenges() {
        User user = User.current();
        if (user == null) return new ArrayList<>();
        GameUserProperty property = user.getProperty(GameUserProperty.class);
        if (property == null) return new ArrayList<>();
        return property.getCurrentChallenges(job.getName());
    }

    public ArrayList<Challenge> getRejectedChallenges() {
        User user = User.current();
        if (user == null) return new ArrayList<>();
        GameUserProperty property = user.getProperty(GameUserProperty.class);
        if (property == null) return new ArrayList<>();
        return property.getRejectedChallenges(job.getName());
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

    @ExportedBean(defaultVisibility = 999)
    public class TeamDetails {

        private String teamName;
        private int score;
        private int absolvedChallenges;

        public TeamDetails(String teamName, int score, int absolvedChallenges) {
            this.teamName = teamName;
            this.score = score;
            this.absolvedChallenges = absolvedChallenges;
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
        public void addScore(int score) {
            this.score += score;
        }

        @Exported
        public int getAbsolvedChallenges() {
            return this.absolvedChallenges;
        }

        @Exported
        public void addAbsolvedChallenges(int absolvedChallenges) {
            this.absolvedChallenges += absolvedChallenges;
        }
    }
}
