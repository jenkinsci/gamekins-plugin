package io.jenkins.plugins.gamekins;

import hudson.model.AbstractProject;
import hudson.model.ProminentProjectAction;

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
}
