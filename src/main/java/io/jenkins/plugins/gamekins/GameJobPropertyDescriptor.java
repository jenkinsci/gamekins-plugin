package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;

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
        return AbstractProject.class.isAssignableFrom(jobType);
    }

    private GameJobProperty getCurrentGameJobProperty() {
        String projectName = Stapler.getCurrentRequest().getOriginalRequestURI().split("/")[3];
        for (Project project : Jenkins.getInstanceOrNull().getProjects()) {
            if (project.getName().equals(projectName)) {
                return (GameJobProperty) project.getProperties().get(this);
            }
        }
        return null;
    }

    public FormValidation doAddTeam(@QueryParameter String teamName) {
        if (teamName.isEmpty()) return FormValidation.error("Insert a name for the team");
        GameJobProperty property = getCurrentGameJobProperty();
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

    public ListBoxModel doFillTeamsBoxItems() {
        GameJobProperty property = getCurrentGameJobProperty();
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
        GameJobProperty jobProperty = getCurrentGameJobProperty();
        if (jobProperty == null || jobProperty.getTeams() == null) return FormValidation.error("Unexpected Error");
        if (!jobProperty.getTeams().contains(teamsBox)) return FormValidation.error("The specified team does not exist");
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
