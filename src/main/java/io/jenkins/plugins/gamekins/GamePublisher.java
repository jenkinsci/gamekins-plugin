package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import io.jenkins.plugins.gamekins.challenge.Challenge;
import io.jenkins.plugins.gamekins.challenge.ChallengeFactory;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class GamePublisher extends Notifier {

    @DataBoundConstructor
    public GamePublisher() {

    }

    @Override
    public GameDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Return true if this {@link Publisher} needs to run after the build result is
     * fully finalized.
     *
     * <p>
     * The execution of normal {@link Publisher}s are considered within a part
     * of the build. This allows publishers to mark the build as a failure, or
     * to include their execution time in the total build time.
     *
     * <p>
     * So normally, that is the preferable behavior, but in a few cases
     * this is problematic. One of such cases is when a publisher needs to
     * trigger other builds, which in turn need to see this build as a
     * completed build. Those plugins that need to do this can return true
     * from this method, so that the {@link #perform(AbstractBuild, Launcher, BuildListener)}
     * method is called after the build is marked as completed.
     *
     * <p>
     * When {@link Publisher} behaves this way, note that they can no longer
     * change the build status anymore.
     *
     * @since 1.153
     */
    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @param build
     * @param launcher
     * @param listener
     * @return Delegates to {@link SimpleBuildStep#perform(Run, FilePath, Launcher, TaskListener)} if possible, always returning true or throwing an error.
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        String projectName = build.getProject().getName();
        for (User user : User.getAll()) {
            GameUserProperty property = user.getProperty(GameUserProperty.class);
            if (property != null && property.isParticipating(projectName)) {
                //TODO: Solved challenge?
                //TODO: Remove line
                property.removeCurrentChallenges(projectName);
                if (property.getCurrentChallenges(projectName).size() < 3) {
                    for (int i = property.getCurrentChallenges(projectName).size(); i < 3; i++) {
                        try {
                            Challenge challenge;
                            boolean isChallengeUnique = true;
                            do {
                                challenge = ChallengeFactory.generateChallenge(build.getWorkspace().getRemote(), user);
                                for (Challenge currentChallenge : property.getCurrentChallenges(projectName)) {
                                    if (currentChallenge.toString().equals(challenge.toString())) {
                                        isChallengeUnique = false;
                                        break;
                                    }
                                }
                            } while (!isChallengeUnique);
                            property.newChallenge(projectName, challenge);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        user.save();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }

    @Extension
    public static final GamePublisher.GameDescriptor DESCRIPTOR = new GamePublisher.GameDescriptor();
    public static class GameDescriptor extends BuildStepDescriptor<Publisher> {

        public GameDescriptor() {
            super(GamePublisher.class);
            load();
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Publisher for Gamekins plugin.";
        }

        /**
         * Returns true if this task is applicable to the given project.
         *
         * @param jobType the type of job
         * @return true to allow user to configure this post-promotion task for the given project.
         * @see AbstractProject.AbstractProjectDescriptor#isApplicable(Descriptor)
         */
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }
    }
}
