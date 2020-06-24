package io.jenkins.plugins.gamekins;

import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.ProminentProjectAction;
import io.jenkins.plugins.gamekins.property.GameJobProperty;
import io.jenkins.plugins.gamekins.property.GameMultiBranchProperty;
import io.jenkins.plugins.gamekins.property.GameProperty;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import javax.annotation.CheckForNull;

public class StatisticsAction implements ProminentProjectAction {

    private final AbstractItem job;

    public StatisticsAction(AbstractItem job) {
        this.job = job;
    }

    public AbstractItem getJob() {
        return this.job;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Statistics";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "statistics";
    }

    public String getStatistics() {
        GameProperty property;
        if (job instanceof WorkflowMultiBranchProject) {
            property = ((WorkflowMultiBranchProject) job).getProperties().get(GameMultiBranchProperty.class);
        } else if (job instanceof WorkflowJob) {
            property = ((WorkflowJob) job).getProperty(GameJobProperty.class);
        } else {
            property = ((AbstractProject<?, ?>) job).getProperty(GameJobProperty.class);
        }
        if (property.getStatistics() == null) return "Statistics is null";
        return property.getStatistics().printToXML();
    }
}
