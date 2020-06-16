package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;

@Extension
public class GameJobPropertyDescriptor extends JobPropertyDescriptor {

    public GameJobPropertyDescriptor() {
        super(GameJobProperty.class);
        load();
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return "Set the activation of the Gamekins plugin.";
    }

    @Override
    public boolean isApplicable(Class<? extends Job> jobType) {
        //TODO: Only for FreeStyle and WorkflowJob
        return true;
    }

    public FormValidation doAddTeam(@AncestorInPath Job<?, ?> job, @QueryParameter String teamName) {
        if (teamName.isEmpty()) return FormValidation.error("Insert a name for the team");
        GameJobProperty property = job == null ? null : (GameJobProperty) job.getProperties().get(this);
        if (property == null || property.getTeams() == null) return FormValidation.error("Unexpected Error");
        if (property.getTeams().contains(teamName))
            return FormValidation.error("The team already exists - please use another name for your team");
        try {
            property.addTeam(teamName);
        } catch (IOException e) {
            e.printStackTrace();
            return FormValidation.error("Unexpected Error");
        }
        save();
        return FormValidation.ok();
    }

    public ListBoxModel doFillTeamsBoxItems(@AncestorInPath Job<?, ?> job) {
        GameJobProperty property = job == null ? null : (GameJobProperty) job.getProperties().get(this);
        ListBoxModel listBoxModel = new ListBoxModel();
        if (property != null && property.getTeams() != null) property.getTeams().forEach(listBoxModel::add);
        return listBoxModel;
    }

    public ListBoxModel doFillUsersBoxItems() {
        ListBoxModel listBoxModel = new ListBoxModel();
        User.getAll().stream().map(User::getFullName).forEach(listBoxModel::add);
        listBoxModel.remove("unknown");
        return listBoxModel;
    }

    public FormValidation doAddUserToTeam(@AncestorInPath Job<?, ?> job, @QueryParameter String teamsBox, @QueryParameter String usersBox) {
        for (User user : User.getAll()) {
            if (user.getFullName().equals(usersBox)) {
                String projectName = job.getName();
                GameUserProperty property = user.getProperty(GameUserProperty.class);
                if (property != null && !property.isParticipating(projectName)) {
                    property.setParticipating(projectName, teamsBox);
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

    public FormValidation doRemoveUserFromTeam(@AncestorInPath Job<?, ?> job, @QueryParameter String teamsBox, @QueryParameter String usersBox) {
        for (User user : User.getAll()) {
            if (user.getFullName().equals(usersBox)) {
                String projectName = job.getName();
                GameUserProperty property = user.getProperty(GameUserProperty.class);
                if (property != null && property.isParticipating(projectName, teamsBox)) {
                    property.removeParticipation(projectName);
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

    public FormValidation doDeleteTeam(@AncestorInPath Job<?, ?> job, @QueryParameter String teamsBox) {
        String projectName = job.getName();
        GameJobProperty jobProperty = (GameJobProperty) job.getProperties().get(this);
        if (jobProperty == null || jobProperty.getTeams() == null) return FormValidation.error("Unexpected Error");
        if (!jobProperty.getTeams().contains(teamsBox)) return FormValidation.error("The specified team does not exist");
        for (User user : User.getAll()) {
            GameUserProperty property = user.getProperty(GameUserProperty.class);
            if (property != null && property.isParticipating(projectName, teamsBox)) {
                property.removeParticipation(projectName);
                try {
                    user.save();
                } catch (IOException e) {
                    return FormValidation.error(e, "There was an error with saving");
                }
            }
        }
        try {
            jobProperty.removeTeam(teamsBox);
        } catch (IOException e) {
            e.printStackTrace();
            return FormValidation.error("Unexpected Error");
        }
        save();
        return FormValidation.ok();
    }
}
