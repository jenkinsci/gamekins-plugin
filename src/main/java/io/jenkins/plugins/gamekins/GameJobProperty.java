package io.jenkins.plugins.gamekins;

import hudson.model.*;
import io.jenkins.plugins.gamekins.challenge.ChallengeFactory;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GameJobProperty extends hudson.model.JobProperty<AbstractProject<?, ?>> {

    private boolean activated;
    private ArrayList<String> teams;

    @DataBoundConstructor
    public GameJobProperty(boolean activated) {
        this.activated = activated;
        this.teams = new ArrayList<>();
    }

    public boolean getActivated() {
        return this.activated;
    }

    @DataBoundSetter
    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public ArrayList<String> getTeams() {
        return this.teams;
    }

    public boolean addTeam(String teamName) throws IOException {
        if (this.teams.contains(teamName)) {
            return false;
        } else {
            this.teams.add(teamName);
            owner.save();
            return true;
        }
    }

    public boolean removeTeam(String teamName) throws IOException {
        if (!this.teams.contains(teamName)) {
            return false;
        } else {
            this.teams.remove(teamName);
            owner.save();
            return true;
        }
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
}
