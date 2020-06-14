package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Mailer;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import io.jenkins.plugins.gamekins.challenge.BuildChallenge;
import io.jenkins.plugins.gamekins.challenge.Challenge;
import io.jenkins.plugins.gamekins.challenge.ChallengeFactory;
import jenkins.tasks.SimpleBuildStep;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

public class GamePublisher extends Notifier {

    private final String jacocoResultsPath;

    @DataBoundConstructor
    public GamePublisher(String jacocoResultsPath) {
        this.jacocoResultsPath = jacocoResultsPath;
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
                try {
                    FileRepositoryBuilder builder = new FileRepositoryBuilder();
                    Repository repo = builder.setGitDir(
                            new File(build.getWorkspace().getRemote() + "/.git")).setMustExist(true).build();
                    RevCommit head = ChallengeFactory.getHead(repo);
                    BuildChallenge challenge = new BuildChallenge();
                    if (build.getResult() != Result.SUCCESS
                            && (head.getAuthorIdent().getName().equals(user.getFullName())
                            || head.getAuthorIdent().getEmailAddress()
                            .equals(user.getProperty(Mailer.UserProperty.class).getAddress()))
                            && !property.getCurrentChallenges(projectName).contains(challenge)) {
                        property.newChallenge(projectName, challenge);
                        user.save();
                    }
                } catch (IOException ignored){}

                for (Challenge challenge : property.getCurrentChallenges(projectName)) {
                    if (challenge.isSolved(build)) {
                        property.absolveChallenge(projectName, challenge);
                        property.addScore(projectName, challenge.getScore());
                    }
                }
                if (property.getCurrentChallenges(projectName).size() < 3) {
                    for (int i = property.getCurrentChallenges(projectName).size(); i < 3; i++) {
                        try {
                            Challenge challenge;
                            boolean isChallengeUnique;
                            do {
                                isChallengeUnique = true;
                                challenge = ChallengeFactory.generateChallenge(build, user, this.jacocoResultsPath);
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

    public String getJacocoResultsPath() {
        return jacocoResultsPath;
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

        public FormValidation doCheckJacocoResultsPath(@AncestorInPath AbstractProject project, @QueryParameter String jacocoResultsPath) {
            if (project == null) {
                return FormValidation.ok();
            }
            try {
                return FilePath.validateFileMask(project.getSomeWorkspace(), jacocoResultsPath);
            } catch (IOException e) {
                return FormValidation.error(e, "IOException");
            }
        }
    }
}
