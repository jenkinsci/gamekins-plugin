package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import io.jenkins.plugins.gamekins.challenge.Challenge;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class LeaderboardAction implements ProminentProjectAction, Describable<LeaderboardAction> {

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

    /**
     * Gets the descriptor for this instance.
     *
     * <p>
     * {@link Descriptor} is a singleton for every concrete {@link Describable}
     * implementation, so if {@code a.getClass() == b.getClass()} then by default
     * {@code a.getDescriptor() == b.getDescriptor()} as well.
     * (In rare cases a single implementation class may be used for instances with distinct descriptors.)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<LeaderboardAction> getDescriptor() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins has not been started");
        }
        return jenkins.getDescriptorOrDie(getClass());
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

    @Extension
    public static final class DescriptorImpl extends Descriptor<LeaderboardAction> {

        public DescriptorImpl() {
            super(LeaderboardAction.class);
        }

        /**
         * Human readable name of this kind of configurable object.
         * Should be overridden for most descriptors, if the display name is visible somehow.
         * As a fallback it uses {@link Class#getSimpleName} on {@link #clazz}, so for example {@code MyThing} from {@code some.pkg.MyThing.DescriptorImpl}.
         * Historically some implementations returned null as a way of hiding the descriptor from the UI,
         * but this is generally managed by an explicit method such as {@code isEnabled} or {@code isApplicable}.
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return super.getDisplayName();
        }

        public FormValidation doRejectChallenge(@QueryParameter String reject) {
            User user = User.current();
            if (user == null) return FormValidation.error("There is no user signed in");
            GameUserProperty property = user.getProperty(GameUserProperty.class);
            if (property == null) return FormValidation.error("Unexpected error");
            String projectName = GameJobPropertyDescriptor.getCurrentProject().getName();
            Challenge challenge = null;
            for (Challenge chal : property.getCurrentChallenges(projectName)) {
                if (chal.toString().equals(reject)) {
                    challenge = chal;
                    break;
                }
            }
            if (challenge == null) return FormValidation.error("The challenge does not exist");
            property.rejectChallenge(projectName, challenge);
            try {
                user.save();
            } catch (IOException e) {
                e.printStackTrace();
                return FormValidation.error("Unexpected error");
            }
            return FormValidation.ok("Challenge rejected");
        }
    }
}
