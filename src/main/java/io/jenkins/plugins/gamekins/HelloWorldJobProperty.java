package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.model.*;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HelloWorldJobProperty extends JobProperty<AbstractProject<?, ?>> {

    private boolean activated;

    @DataBoundConstructor
    public HelloWorldJobProperty(boolean activated) {
        this.activated = activated;
    }

    public boolean getActivated() {
        return this.activated;
    }

    @DataBoundSetter
    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    @Override
    public JobPropertyDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public JobProperty<?> reconfigure(StaplerRequest req, JSONObject form) {
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
                if (a instanceof HelloWorldAction) {
                    return actions;
                }
            }
            actions.add(new HelloWorldAction(job));
        } else {
            actions.removeIf(a -> a instanceof HelloWorldAction);
        }
        return actions;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    public static class DescriptorImpl extends JobPropertyDescriptor {

        public DescriptorImpl() {
            super(HelloWorldJobProperty.class);
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
    }
}
