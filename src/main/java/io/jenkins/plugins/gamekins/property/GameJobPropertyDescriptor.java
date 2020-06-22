package io.jenkins.plugins.gamekins.property;

import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.gamekins.util.PropertyUtil;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;

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
        return jobType == FreeStyleProject.class || jobType == WorkflowJob.class;
    }

    @Override
    public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        return new GameJobProperty((AbstractItem) req.findAncestor(AbstractItem.class).getObject(),
                formData.getBoolean("activated"), formData.getBoolean("showStatistics"));
    }

    public FormValidation doAddTeam(@AncestorInPath Job<?, ?> job, @QueryParameter String teamName) {
        if (teamName.isEmpty()) return FormValidation.error("Insert a name for the team");
        GameJobProperty property = job == null ? null : (GameJobProperty) job.getProperties().get(this);
        FormValidation validation = PropertyUtil.doAddTeam( property, teamName);
        save();
        return validation;
    }

    public ListBoxModel doFillTeamsBoxItems(@AncestorInPath Job<?, ?> job) {
        GameJobProperty property = job == null ? null : (GameJobProperty) job.getProperties().get(this);
        return PropertyUtil.doFillTeamsBoxItems(property);
    }

    public ListBoxModel doFillUsersBoxItems() {
        return PropertyUtil.doFillUsersBoxItems();
    }

    public FormValidation doAddUserToTeam(@AncestorInPath Job<?, ?> job, @QueryParameter String teamsBox,
                                          @QueryParameter String usersBox) {
        return PropertyUtil.doAddUserToTeam(job, teamsBox, usersBox);
    }

    public FormValidation doRemoveUserFromTeam(@AncestorInPath Job<?, ?> job, @QueryParameter String teamsBox,
                                               @QueryParameter String usersBox) {
        return PropertyUtil.doRemoveUserFromTeam(job, teamsBox, usersBox);
    }

    public FormValidation doDeleteTeam(@AncestorInPath Job<?, ?> job, @QueryParameter String teamsBox) {
        String projectName = job.getName();
        GameJobProperty property = (GameJobProperty) job.getProperties().get(this);
        FormValidation validation = PropertyUtil.doDeleteTeam(projectName, property, teamsBox);
        save();
        return validation;
    }
}
