package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GameJobProperty extends hudson.model.JobProperty<AbstractProject<?, ?>> {

    private boolean activated;

    @DataBoundConstructor
    public GameJobProperty(boolean activated) {
        this.activated = activated;
    }

    public boolean getActivated() {
        return this.activated;
    }

    @DataBoundSetter
    public void setActivated(boolean activated) {
        this.activated = activated;
        DESCRIPTOR.save();
    }

    @Override
    public JobPropertyDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public hudson.model.JobProperty<?> reconfigure(StaplerRequest req, JSONObject form) {
        if (form != null) this.activated = (boolean) form.get("activated");
        return this;
    }

    /**
     * {@link Action}s to be displayed in the job page.
     *
     * <p>
     * Returning actions from this method allows a job property to add them
     * to the left navigation bar in the job page.
     *
     * <p>
     * {@link Action} can implement additional marker interface to integrate
     * with the UI in different ways.
     *
     * @param job Always the same as {@link #owner} but passed in anyway for backward compatibility (I guess.)
     *            You really need not use this value at all.
     * @return can be empty but never null.
     * @see ProminentProjectAction
     * @see PermalinkProjectAction
     * @since 1.341
     */
    @Nonnull
    @Override
    public Collection<? extends Action> getJobActions(AbstractProject<?, ?> job) {
        List<Action> actions = new ArrayList<>(job.getActions());
        if (activated) {
            for (Action a : actions) {
                if (a instanceof LeaderboardAction) {
                    return actions;
                }
            }
            actions.add(new LeaderboardAction(job));
        } else {
            actions.removeIf(a -> a instanceof LeaderboardAction);
        }
        return actions;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    public static class DescriptorImpl extends JobPropertyDescriptor {

        private ArrayList<String> teams;

        public DescriptorImpl() {
            super(GameJobProperty.class);
            teams = new ArrayList<>();
            load();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Set the activation of the Gamekins plugin.";
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }

        public FormValidation doAddTeam(@QueryParameter String teamName) {
            if (teamName.isEmpty()) return FormValidation.error("Insert a name for the team");
            if (teams.contains(teamName))
                return FormValidation.error("The team already exists - please use another name for your team");
            teams.add(teamName);
            save();
            return FormValidation.ok();
        }

        public ListBoxModel doFillTeamsBoxItems() {
            ListBoxModel listBoxModel = new ListBoxModel();
            teams.forEach(listBoxModel::add);
            return listBoxModel;
        }

        public ListBoxModel doFillUsersBoxItems() {
            ListBoxModel listBoxModel = new ListBoxModel();
            User.getAll().stream().map(User::getFullName).forEach(listBoxModel::add);
            listBoxModel.remove("unknown");
            return listBoxModel;
        }

        public FormValidation doAddUserToTeam(@QueryParameter String teamsBox, @QueryParameter String usersBox) {
            for (User user : User.getAll()) {
                if (user.getFullName().equals(usersBox)) {
                    GameUserProperty property = user.getProperty(GameUserProperty.class);
                    if (property != null && property.getTeamName().equals("")) {
                        property.setTeamName(teamsBox);
                        property.setParticipating(true);
                        //TODO: Add challenges
                        try {
                            user.save();
                        } catch (IOException e) {
                            return FormValidation.error(e, "There was an error with saving");
                        }
                        return FormValidation.ok();
                    } else {
                        return FormValidation.error("The user is already participating in a team");
                    }
                }
            }
            return FormValidation.error("No user with the specified name found");
        }

        public FormValidation doRemoveUserFromTeam(@QueryParameter String teamsBox, @QueryParameter String usersBox) {
            for (User user : User.getAll()) {
                if (user.getFullName().equals(usersBox)) {
                    GameUserProperty property = user.getProperty(GameUserProperty.class);
                    if (property != null && property.getTeamName().equals(teamsBox)) {
                        property.setTeamName("");
                        property.setParticipating(false);
                        try {
                            user.save();
                        } catch (IOException e) {
                            return FormValidation.error(e, "There was an error with saving");
                        }
                        return FormValidation.ok();
                    } else {
                        return FormValidation.error("The user is not in the specified team");
                    }
                }
            }
            return FormValidation.error("No user with the specified name found");
        }

        public FormValidation doDeleteTeam(@QueryParameter String teamsBox) {
            if (!teams.contains(teamsBox)) return FormValidation.error("The specified team does not exist");
            for (User user : User.getAll()) {
                GameUserProperty property = user.getProperty(GameUserProperty.class);
                if (property != null && property.getTeamName().equals(teamsBox)) {
                    property.setTeamName("");
                    property.setParticipating(false);
                    try {
                        user.save();
                    } catch (IOException e) {
                        return FormValidation.error(e, "There was an error with saving");
                    }
                }
            }
            teams.remove(teamsBox);
            save();
            return FormValidation.ok();
        }
    }
}
