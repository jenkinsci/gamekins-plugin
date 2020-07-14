package io.jenkins.plugins.gamekins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.*;
import hudson.util.FormValidation;
import io.jenkins.plugins.gamekins.challenge.BuildChallenge;
import io.jenkins.plugins.gamekins.challenge.Challenge;
import io.jenkins.plugins.gamekins.challenge.ChallengeFactory;
import io.jenkins.plugins.gamekins.challenge.DummyChallenge;
import io.jenkins.plugins.gamekins.property.GameJobProperty;
import io.jenkins.plugins.gamekins.property.GameMultiBranchProperty;
import io.jenkins.plugins.gamekins.property.GameProperty;
import io.jenkins.plugins.gamekins.statistics.Statistics;
import io.jenkins.plugins.gamekins.util.GitUtil;
import io.jenkins.plugins.gamekins.util.JacocoUtil;
import io.jenkins.plugins.gamekins.util.PublisherUtil;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class GamePublisher extends Notifier implements SimpleBuildStep {

    private final int SEARCH_COMMIT_COUNT = 50;

    private String jacocoResultsPath;
    private String jacocoCSVPath;

    @DataBoundConstructor
    public GamePublisher(String jacocoResultsPath, String jacocoCSVPath) {
        this.jacocoResultsPath = jacocoResultsPath;
        this.jacocoCSVPath = jacocoCSVPath;
    }

    @DataBoundSetter
    public void setJacocoResultsPath(String jacocoResultsPath) {
        this.jacocoResultsPath = jacocoResultsPath;
    }

    @DataBoundSetter
    public void setJacocoCSVPath(String jacocoCSVPath) {
        this.jacocoCSVPath = jacocoCSVPath;
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
     * @return Delegates to {@link SimpleBuildStep#perform(Run, FilePath, Launcher, TaskListener)}
     * if possible, always returning true or throwing an error.
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        if (build.getProject() == null || build.getProject().getProperty(GameJobProperty.class) == null
                || !build.getProject().getProperty(GameJobProperty.class).getActivated()) {
            listener.getLogger().println("[Gamekins] Not activated");
            return true;
        }
        HashMap<String, String> constants = new HashMap<>();
        constants.put("projectName", build.getProject().getName());
        executePublisher(build, constants, build.getResult(), listener, build.getWorkspace());
        return true;
    }

    /**
     * Declares the scope of the synchronization monitor this {@link BuildStep} expects from outside.
     *
     * <p>
     * This method is introduced for preserving compatibility with plugins written for earlier versions of Hudson,
     * which never run multiple builds of the same job in parallel. Such plugins often assume that the outcome
     * of the previous build is completely available, which is no longer true when we do concurrent builds.
     *
     * <p>
     * To minimize the necessary code change for such plugins, {@link BuildStep} implementations can request
     * Hudson to externally perform synchronization before executing them. This behavior is as follows:
     *
     * <dl>
     * <dt>{@link BuildStepMonitor#BUILD}
     * <dd>
     * This {@link BuildStep} is only executed after the previous build is fully
     * completed (thus fully restoring the earlier semantics of one build at a time.)
     *
     * <dt>{@link BuildStepMonitor#STEP}
     * <dd>
     * This {@link BuildStep} is only executed after the same step in the previous build is completed.
     * For build steps that use a weaker assumption and only rely on the output from the same build step of
     * the early builds, this improves the concurrency.
     *
     * <dt>{@link BuildStepMonitor#NONE}
     * <dd>
     * No external synchronization is performed on this build step. This is the most efficient, and thus
     * <b>the recommended value for newer plugins</b>. Wherever necessary, you can directly use {@link CheckPoint}s
     * to perform necessary synchronizations.
     * </dl>
     *
     * <h2>Migrating Older Implementation</h2>
     * <p>
     * If you are migrating {@link BuildStep} implementations written for earlier versions of Hudson,
     * here's what you can do:
     *
     * <ul>
     * <li>
     * To demand the backward compatible behavior from Jenkins, leave this method unoverridden,
     * and make no other changes to the code. This will prevent users from reaping the benefits of concurrent
     * builds, but at least your plugin will work correctly, and therefore this is a good easy first step.
     * <li>
     * If your build step doesn't use anything from a previous build (for example, if you don't even call
     * {@link Run#getPreviousBuild()}), then you can return {@link BuildStepMonitor#NONE} without making further
     * code changes and you are done with migration.
     * <li>
     * If your build step only depends on {@link Action}s that you added in the previous build by yourself,
     * then you only need {@link BuildStepMonitor#STEP} scope synchronization. Return it from this method
     * ,and you are done with migration without any further code changes.
     * <li>
     * If your build step makes more complex assumptions, return {@link BuildStepMonitor#NONE} and use
     * {@link CheckPoint}s directly in your code. The general idea is to call {@link CheckPoint#block()} before
     * you try to access the state from the previous build.
     * </ul>
     *
     * @since 1.319
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.STEP;
    }

    public String getJacocoResultsPath() {
        return jacocoResultsPath;
    }

    public String getJacocoCSVPath() {
        return jacocoCSVPath;
    }

    /**
     * Run this step.
     *
     * @param run       a build this is running as a part of
     * @param workspace a workspace to use for any file operations
     * @param launcher  a way to start processes
     * @param listener  a place to send output
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher, @Nonnull TaskListener listener) {
        HashMap<String, String> constants = new HashMap<>();
        if (run.getParent().getParent() instanceof WorkflowMultiBranchProject) {
            WorkflowMultiBranchProject project = (WorkflowMultiBranchProject) run.getParent().getParent();
            if (project.getProperties().get(GameMultiBranchProperty.class) == null
                    || !project.getProperties().get(GameMultiBranchProperty.class).getActivated()) {
                listener.getLogger().println("[Gamekins] Not activated");
                return;
            }
            constants.put("projectName", project.getName());
        } else {
            if (run.getParent().getProperty(GameJobProperty.class) == null
                    || !run.getParent().getProperty(GameJobProperty.class).getActivated()) {
                listener.getLogger().println("[Gamekins] Not activated");
                return;
            }
            constants.put("projectName", run.getParent().getName());
        }
        constants.put("jacocoResultsPath", getJacocoResultsPath());
        constants.put("jacocoCSVPath", getJacocoCSVPath());
        executePublisher(run, constants, run.getResult(), listener, workspace);
    }

    private void executePublisher(Run<?, ?> run, HashMap<String, String> constants, Result result,
                                  TaskListener listener, FilePath workspace) {
        if (!PublisherUtil.doCheckJacocoResultsPath(workspace, this.jacocoResultsPath)) {
            listener.getLogger().println("[Gamekins] JaCoCo folder is not correct");
            return;
        }
        if (!PublisherUtil.doCheckJacocoCSVPath(workspace, this.jacocoCSVPath)) {
            listener.getLogger().println("[Gamekins] JaCoCo csv file could not be found");
            return;
        }
        constants.put("jacocoResultsPath", getJacocoResultsPath());
        constants.put("jacocoCSVPath", getJacocoCSVPath());
        if (run.getParent().getParent() instanceof WorkflowMultiBranchProject) {
            constants.put("branch", run.getParent().getName());
        } else {
            constants.put("branch", GitUtil.getBranch(workspace));
        }

        listener.getLogger().println("[Gamekins] Start");
        listener.getLogger().println("[Gamekins] Solve Challenges and generate new Challenges");

        ArrayList<JacocoUtil.ClassDetails> classes;
        try {
            classes = workspace.act(new GitUtil.LastChangedClassesCallable(SEARCH_COMMIT_COUNT, constants, listener,
                            GitUtil.mapUsersToGameUsers(User.getAll()), workspace));
            listener.getLogger().println("[Gamekins] Found " + classes.size() + " last changed files");
            classes.removeIf(classDetails -> classDetails.getCoverage() == 1.0);
            listener.getLogger().println("[Gamekins] Found " + classes.size()
                    + " last changed files without 100% coverage");
            classes.sort(Comparator.comparingDouble(JacocoUtil.ClassDetails::getCoverage));
            Collections.reverse(classes);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(listener.getLogger());
            return;
        }

        int solved = 0;
        int generated = 0;
        for (User user : User.getAll()) {
            GameUserProperty property = user.getProperty(GameUserProperty.class);
            if (property != null && property.isParticipating(constants.get("projectName"))) {
                try {
                    if (result != Result.SUCCESS) {
                        BuildChallenge challenge = new BuildChallenge();
                        User mapUser = GitUtil.mapUser(
                                workspace.act(new GitUtil.HeadCommitCallable(workspace.getRemote()))
                                        .getAuthorIdent(), User.getAll());
                        if (mapUser != null && mapUser.equals(user)
                                && !property.getCurrentChallenges(constants.get("projectName")).contains(challenge)) {
                            property.newChallenge(constants.get("projectName"), challenge);
                            listener.getLogger().println("[Gamekins] Generated new BuildChallenge");
                            generated++;
                            user.save();
                        }
                    }
                } catch (IOException | InterruptedException e){
                    e.printStackTrace(listener.getLogger());
                }

                listener.getLogger().println("[Gamekins] Start checking solved status of challenges for user "
                        + user.getFullName());
                for (Challenge challenge : property.getCurrentChallenges(constants.get("projectName"))) {
                    if (challenge.isSolved(constants, run, listener, workspace)) {
                        property.completeChallenge(constants.get("projectName"), challenge);
                        property.addScore(constants.get("projectName"), challenge.getScore());
                        listener.getLogger().println("[Gamekins] Solved challenge " + challenge.toString());
                        solved++;
                    }
                }

                listener.getLogger().println("[Gamekins] Start checking solvable state of challenges for user "
                        + user.getFullName());
                for (Challenge challenge : property.getCurrentChallenges(constants.get("projectName"))) {
                    if (!challenge.isSolvable(constants, run, listener, workspace)) {
                        property.rejectChallenge(constants.get("projectName"), challenge, "Not solvable");
                        listener.getLogger().println("[Gamekins] Challenge " + challenge.toString()
                                + " can not be solved anymore");
                    }
                }

                if (property.getCurrentChallenges(constants.get("projectName")).size() < 3) {
                    listener.getLogger().println("[Gamekins] Start generating challenges for user "
                            + user.getFullName());

                    ArrayList<JacocoUtil.ClassDetails> userClasses = new ArrayList<>(classes);
                    userClasses.removeIf(classDetails -> !classDetails.getChangedByUsers()
                            .contains(new GitUtil.GameUser(user)));
                    listener.getLogger().println("[Gamekins] Found " + userClasses.size()
                            + " last changed files of user " + user.getFullName());

                    for (int i = property.getCurrentChallenges(constants.get("projectName")).size(); i < 3; i++) {
                        if (userClasses.size() == 0) {
                            property.newChallenge(constants.get("projectName"), new DummyChallenge());
                            break;
                        }
                        try {
                            Challenge challenge;
                            boolean isChallengeUnique;
                            int count = 0;
                            do {
                                if (count == 3) {
                                    challenge = new DummyChallenge();
                                    break;
                                }
                                isChallengeUnique = true;
                                listener.getLogger().println("[Gamekins] Started to generate challenge");
                                challenge = ChallengeFactory.generateChallenge(user, constants, listener, userClasses,
                                        workspace);
                                listener.getLogger().println("[Gamekins] Generated challenge " + challenge.toString());
                                if (challenge instanceof DummyChallenge) break;
                                for (Challenge currentChallenge
                                        : property.getCurrentChallenges(constants.get("projectName"))) {
                                    if (currentChallenge.toString().equals(challenge.toString())) {
                                        isChallengeUnique = false;
                                        listener.getLogger().println("[Gamekins] Challenge is not unique");
                                        break;
                                    }
                                }
                                count++;
                            } while (!isChallengeUnique);
                            property.newChallenge(constants.get("projectName"), challenge);
                            listener.getLogger().println("[Gamekins] Added challenge " + challenge.toString());
                            generated++;
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace(listener.getLogger());
                        }
                    }
                    try {
                        user.save();
                    } catch (IOException e) {
                        e.printStackTrace(listener.getLogger());
                    }
                }
            }
        }

        listener.getLogger().println("[Gamekins] Solved " + solved + " Challenges and generated "
                + generated + " Challenges");
        listener.getLogger().println("[Gamekins] Update Statistics");

        GameProperty property;
        AbstractItem job;
        if (run.getParent().getParent() instanceof WorkflowMultiBranchProject) {
            job = (AbstractItem) run.getParent().getParent();
            property = ((WorkflowMultiBranchProject) run.getParent().getParent())
                    .getProperties().get(GameMultiBranchProperty.class);
        } else {
            job = run.getParent();
            property = (GameProperty) run.getParent().getProperty(GameProperty.class.getName());
            if (property == null) {
                property = (GameJobProperty) run.getParent().getProperty(GameJobProperty.class.getName());
            }
        }

        if (property != null) {
            property.getStatistics()
                    .addRunEntry(
                            job,
                            constants.get("branch"),
                            new Statistics.RunEntry(
                                    run.getNumber(),
                                    constants.get("branch"),
                                    run.getResult(),
                                    run.getStartTimeInMillis(),
                                    generated,
                                    solved,
                                    JacocoUtil.getTestCount(workspace, run),
                                    JacocoUtil.getProjectCoverage(workspace,
                                            constants.get("jacocoCSVPath").split("/")
                                                    [constants.get("jacocoCSVPath").split("/").length - 1])
                            ), listener);

            try {
                property.getOwner().save();
            } catch (IOException e) {
                e.printStackTrace(listener.getLogger());
            }
        } else {
            listener.getLogger().println("[Gamekins] No entry for Statistics added");
        }

        listener.getLogger().println("[Gamekins] Finished");
    }

    public static final GamePublisher.GameDescriptor DESCRIPTOR = new GamePublisher.GameDescriptor();
    @Extension @Symbol("gamekins")
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

        public FormValidation doCheckJacocoResultsPath(@AncestorInPath AbstractProject<?, ?> project,
                                                       @QueryParameter String jacocoResultsPath) {
            if (project == null) {
                return FormValidation.ok();
            }
            return PublisherUtil.doCheckJacocoResultsPath(project.getSomeWorkspace(), jacocoResultsPath)
                    ? FormValidation.ok() : FormValidation.error("The folder is not correct");
        }

        public FormValidation doCheckJacocoCSVPath(@AncestorInPath AbstractProject<?, ?> project,
                                                   @QueryParameter String jacocoCSVPath) {
            if (project == null) {
                return FormValidation.ok();
            }
            return PublisherUtil.doCheckJacocoCSVPath(project.getSomeWorkspace(), jacocoCSVPath)
                    ? FormValidation.ok() : FormValidation.error("The file could not be found");
        }
    }
}
