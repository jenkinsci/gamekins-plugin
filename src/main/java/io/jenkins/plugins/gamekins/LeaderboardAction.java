package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.model.*;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.util.FormValidation;
import io.jenkins.plugins.gamekins.challenge.Challenge;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class LeaderboardAction implements ProminentProjectAction, Describable<LeaderboardAction> {

    private final AbstractItem job;

    public LeaderboardAction(AbstractItem job) {
        this.job = job;
    }

    public AbstractItem getJob() {
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
            if (user.getProperty(HudsonPrivateSecurityRealm.Details.class) == null) continue;
            GameUserProperty property = user.getProperty(GameUserProperty.class);
            if (property != null && property.isParticipating(job.getName())) {
                details.add(
                        new UserDetails(
                                user.getFullName(),
                                property.getTeamName(job.getName()),
                                property.getScore(job.getName()),
                                property.getCompletedChallenges(job.getName()).size()
                        )
                );
            }
        }
        details.sort(Comparator.comparingInt(UserDetails::getScore));
        Collections.reverse(details);
        return details;
    }

    public List<TeamDetails> getTeamDetails() {
        ArrayList<TeamDetails> details = new ArrayList<>();
        for (User user : User.getAll()) {
            if (user.getProperty(HudsonPrivateSecurityRealm.Details.class) == null) continue;
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
                    details.get(index).addCompletedChallenges(property.getCompletedChallenges(job.getName()).size());
                    details.get(index).addScore(property.getScore(job.getName()));
                } else {
                    details.add(
                            new TeamDetails(
                                    property.getTeamName(job.getName()),
                                    property.getScore(job.getName()),
                                    property.getCompletedChallenges(job.getName()).size()
                            )
                    );
                }
            }
        }
        details.sort(Comparator.comparingInt(TeamDetails::getScore));
        Collections.reverse(details);
        return details;
    }

    public CopyOnWriteArrayList<Challenge> getCompletedChallenges() {
        User user = User.current();
        if (user == null) return new CopyOnWriteArrayList<>();
        GameUserProperty property = user.getProperty(GameUserProperty.class);
        if (property == null) return new CopyOnWriteArrayList<>();
        return property.getCompletedChallenges(job.getName());
    }

    public CopyOnWriteArrayList<Challenge> getCurrentChallenges() {
        User user = User.current();
        if (user == null) return new CopyOnWriteArrayList<>();
        GameUserProperty property = user.getProperty(GameUserProperty.class);
        if (property == null) return new CopyOnWriteArrayList<>();
        return property.getCurrentChallenges(job.getName());
    }

    public CopyOnWriteArrayList<Challenge> getRejectedChallenges() {
        User user = User.current();
        if (user == null) return new CopyOnWriteArrayList<>();
        GameUserProperty property = user.getProperty(GameUserProperty.class);
        if (property == null) return new CopyOnWriteArrayList<>();
        return property.getRejectedChallenges(job.getName());
    }

    public boolean isParticipating() {
        User user = User.current();
        if (user == null) return false;
        GameUserProperty property = user.getProperty(GameUserProperty.class);
        if (property == null) return false;
        return property.isParticipating(job.getName());
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
        Jenkins jenkins = Jenkins.get();
        return jenkins.getDescriptorOrDie(getClass());
    }

    @ExportedBean(defaultVisibility = 999)
    public static class UserDetails {

        private final String userName;
        private final String teamName;
        private final int score;
        private final int completedChallenges;

        public UserDetails(String userName, String teamName, int score, int completedChallenges) {
            this.userName = userName;
            this.teamName = teamName;
            this.score = score;
            this.completedChallenges = completedChallenges;
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
        public int getCompletedChallenges() {
            return this.completedChallenges;
        }
    }

    @ExportedBean(defaultVisibility = 999)
    public static class TeamDetails {

        private final String teamName;
        private int score;
        private int completedChallenges;

        public TeamDetails(String teamName, int score, int completedChallenges) {
            this.teamName = teamName;
            this.score = score;
            this.completedChallenges = completedChallenges;
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
        public int getCompletedChallenges() {
            return this.completedChallenges;
        }

        @Exported
        public void addCompletedChallenges(int completedChallenges) {
            this.completedChallenges += completedChallenges;
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<LeaderboardAction> {

        public DescriptorImpl() {
            super(LeaderboardAction.class);
            load();
        }

        /**
         * Human readable name of this kind of configurable object.
         * Should be overridden for most descriptors, if the display name is visible somehow.
         * As a fallback it uses {@link Class#getSimpleName} on {@link #clazz}, so for example {@code MyThing}
         * from {@code some.pkg.MyThing.DescriptorImpl}.
         * Historically some implementations returned null as a way of hiding the descriptor from the UI,
         * but this is generally managed by an explicit method such as {@code isEnabled} or {@code isApplicable}.
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return super.getDisplayName();
        }

        public FormValidation doRejectChallenge(@AncestorInPath AbstractItem job, @QueryParameter String reject,
                                                @QueryParameter String reason) {
            if (reason.isEmpty()) return FormValidation.error("Please insert your reason for rejection");
            if (reason.matches("\\s+")) reason = "No reason provided";
            User user = User.current();
            if (user == null) return FormValidation.error("There is no user signed in");
            GameUserProperty property = user.getProperty(GameUserProperty.class);
            if (property == null) return FormValidation.error("Unexpected error");
            String projectName = job.getName();
            Challenge challenge = null;
            for (Challenge chal : property.getCurrentChallenges(projectName)) {
                if (chal.toString().equals(reject)) {
                    challenge = chal;
                    break;
                }
            }
            if (challenge == null) return FormValidation.error("The challenge does not exist");
            property.rejectChallenge(projectName, challenge, reason);
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
