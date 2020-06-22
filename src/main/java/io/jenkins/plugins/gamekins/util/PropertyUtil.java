package io.jenkins.plugins.gamekins.util;

import hudson.model.AbstractItem;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.User;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gamekins.StatisticsAction;
import io.jenkins.plugins.gamekins.property.GameProperty;
import io.jenkins.plugins.gamekins.GameUserProperty;
import io.jenkins.plugins.gamekins.LeaderboardAction;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

public class PropertyUtil {

    private PropertyUtil() {}

    public static void reconfigure(AbstractItem owner, boolean activated, boolean showStatistics) {
        if (owner instanceof WorkflowJob) {
            if (activated) {
                owner.addOrReplaceAction(new LeaderboardAction(owner));
            } else {
                owner.removeAction(new LeaderboardAction(owner));
            }
            if (showStatistics) {
                owner.addOrReplaceAction(new StatisticsAction(owner));
            } else {
                owner.removeAction(new StatisticsAction(owner));
            }
            try {
                owner.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (owner instanceof WorkflowMultiBranchProject) {
            //TODO: Without reflection (Trigger)
            try {
                Field actionField = Actionable.class.getDeclaredField("actions");
                actionField.setAccessible(true);
                if (activated) {
                    ((List<Action>) actionField.get(owner)).removeIf(action -> action instanceof LeaderboardAction);
                    ((List<Action>) actionField.get(owner)).add(new LeaderboardAction(owner));
                } else {
                    ((List<Action>) actionField.get(owner)).removeIf(action -> action instanceof LeaderboardAction);
                }
                if (showStatistics) {
                    ((List<Action>) actionField.get(owner)).removeIf(action -> action instanceof StatisticsAction);
                    ((List<Action>) actionField.get(owner)).add(new StatisticsAction(owner));
                } else {
                    ((List<Action>) actionField.get(owner)).removeIf(action -> action instanceof StatisticsAction);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            try {
                owner.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static ListBoxModel doFillTeamsBoxItems(GameProperty property) {
        ListBoxModel listBoxModel = new ListBoxModel();
        if (property != null && property.getTeams() != null) property.getTeams().forEach(listBoxModel::add);
        return listBoxModel;
    }

    public static ListBoxModel doFillUsersBoxItems() {
        ListBoxModel listBoxModel = new ListBoxModel();
        User.getAll().stream().map(User::getFullName).forEach(listBoxModel::add);
        listBoxModel.remove("unknown");
        return listBoxModel;
    }

    public static FormValidation doAddUserToTeam(AbstractItem job, String teamsBox, String usersBox) {
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

    public static FormValidation doRemoveUserFromTeam(AbstractItem job, String teamsBox, String usersBox) {
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

    public static FormValidation doDeleteTeam(String projectName, GameProperty property, String teamsBox) {
        if (property == null || property.getTeams() == null) return FormValidation.error("Unexpected Error");
        if (!property.getTeams().contains(teamsBox))
            return FormValidation.error("The specified team does not exist");
        for (User user : User.getAll()) {
            GameUserProperty userProperty = user.getProperty(GameUserProperty.class);
            if (userProperty != null && userProperty.isParticipating(projectName, teamsBox)) {
                userProperty.removeParticipation(projectName);
                try {
                    user.save();
                } catch (IOException e) {
                    return FormValidation.error(e, "There was an error with saving");
                }
            }
        }
        try {
            property.removeTeam(teamsBox);
        } catch (IOException e) {
            e.printStackTrace();
            return FormValidation.error("Unexpected Error");
        }
        return FormValidation.ok();
    }

    public static FormValidation doAddTeam(GameProperty property, String teamName) {
        if (property == null || property.getTeams() == null) return FormValidation.error("Unexpected Error");
        if (property.getTeams().contains(teamName))
            return FormValidation.error("The team already exists - please use another name for your team");
        try {
            property.addTeam(teamName);
        } catch (IOException e) {
            e.printStackTrace();
            return FormValidation.error("Unexpected Error");
        }
        return FormValidation.ok();
    }
}
