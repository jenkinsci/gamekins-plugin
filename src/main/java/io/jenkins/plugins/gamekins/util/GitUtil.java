package io.jenkins.plugins.gamekins.util;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.tasks.Mailer;
import io.jenkins.plugins.gamekins.GameUserProperty;
import jenkins.security.MasterToSlaveCallable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.*;

public class GitUtil {

    private GitUtil() {}

    public static RevCommit getHead(Repository repo) throws IOException {
        return getCommit(repo, Constants.HEAD);
    }

    private static RevCommit getCommit(Repository repo, String hash) throws IOException {
        RevWalk walk = new RevWalk(repo);

        ObjectId id = repo.resolve(hash);
        RevCommit commit = walk.parseCommit(id);
        walk.dispose();

        return commit;
    }

    public static Set<String> getLastChangedSourceFilesOfUser(FilePath workspace, User user, int commitCount,
                                                              String commitHash, Collection<User> users)
            throws IOException, InterruptedException {
        Set<String> pathsToFiles = workspace.act(
                new LastChangedFilesCallable(
                        workspace.getRemote(), new GameUser(user), commitCount, commitHash, mapUsersToGameUsers(users)
                ));
        if (!pathsToFiles.isEmpty()) {
            pathsToFiles.removeIf(path -> Arrays.asList(path.split("/")).contains("test"));
            pathsToFiles.removeIf(path -> !(path.contains(".java") || path.contains(".kt")));
        }
        return pathsToFiles;
    }

    public static Set<String> getLastChangedTestFilesOfUser(FilePath workspace, User user, int commitCount,
                                                            String commitHash, Collection<User> users)
            throws IOException, InterruptedException {
        Set<String> pathsToFiles = workspace.act(
                new LastChangedFilesCallable(
                        workspace.getRemote(), new GameUser(user), commitCount, commitHash, mapUsersToGameUsers(users)
                ));
        if (!pathsToFiles.isEmpty()) {
            pathsToFiles.removeIf(path -> !Arrays.asList(path.split("/")).contains("test"));
            pathsToFiles.removeIf(path -> !(path.contains(".java") || path.contains(".kt")));
        }
        return pathsToFiles;
    }

    private static Set<String> getLastChangedFilesOfUser(String workspace, GameUser user, int commitCount,
                                                         String commitHash, ArrayList<GameUser> users)
            throws IOException {
        if (commitCount <= 0) commitCount = Integer.MAX_VALUE;

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.setGitDir(new File(workspace + "/.git")).setMustExist(true).build();
        RevWalk walk = new RevWalk(repo);

        RevCommit targetCommit = null;
        if (!commitHash.isEmpty()) {
            targetCommit = getCommit(repo, commitHash);
        }
        RevCommit headCommit = getHead(repo);
        Git git = new Git(repo);

        if (targetCommit == headCommit) return new LinkedHashSet<>();

        int countUserCommit = 0;
        int totalCount = 0;
        ArrayList<RevCommit> currentCommits = new ArrayList<>();
        currentCommits.add(headCommit);
        LinkedHashSet<String> pathsToFiles = new LinkedHashSet<>();

        while (countUserCommit < commitCount && totalCount < commitCount * 5) {
            if (currentCommits.isEmpty()) break;
            ArrayList<RevCommit> newCommits = new ArrayList<>();
            for (RevCommit commit : currentCommits) {
                GameUser mapUser = mapUser(commit.getAuthorIdent(), users);
                if (mapUser != null &&  mapUser.equals(user)) {
                    String diff = getDiffOfCommit(git, repo, commit);

                    String[] lines = diff.split("\n");
                    for (String line : lines) {
                        if (line.contains("diff --git")) {
                            pathsToFiles.add(line.split(" ")[2].substring(1));
                        }
                    }

                    countUserCommit++;
                }
                
                for (RevCommit parent : commit.getParents()) {
                    newCommits.add(walk.parseCommit(repo.resolve(parent.getName())));
                    walk.dispose();
                }
            }

            if (targetCommit != null) {
                newCommits.remove(targetCommit);
                newCommits.removeAll(Arrays.asList(targetCommit.getParents()));
            }

            currentCommits = new ArrayList<>(newCommits);
            totalCount++;
        }

        return pathsToFiles;
    }

    private static String getDiffOfCommit(Git git, Repository repo, RevCommit newCommit) throws IOException {
        RevCommit oldCommit = getPrevHash(repo, newCommit);
        AbstractTreeIterator oldTreeIterator = oldCommit == null
                ? new EmptyTreeIterator() : getCanonicalTreeParser(git, oldCommit);
        AbstractTreeIterator newTreeIterator = getCanonicalTreeParser(git, newCommit);

        OutputStream outputStream = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(outputStream)) {
            formatter.setRepository(git.getRepository());
            formatter.format(oldTreeIterator, newTreeIterator);
        }
        return outputStream.toString();
    }

    private static RevCommit getPrevHash(Repository repo, RevCommit commit)  throws  IOException {
        try (RevWalk walk = new RevWalk(repo)) {
            walk.markStart(commit);
            int count = 0;
            for (RevCommit rev : walk) {
                if (count == 1) {
                    return rev;
                }
                count++;
            }
            walk.dispose();
        }

        return null;
    }

    private static AbstractTreeIterator getCanonicalTreeParser(Git git, ObjectId commitId) throws IOException {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit commit = walk.parseCommit(commitId);
            ObjectId treeId = commit.getTree().getId();
            try (ObjectReader reader = git.getRepository().newObjectReader()) {
                return new CanonicalTreeParser(null, reader, treeId);
            }
        }
    }

    public static String getBranch(FilePath workspace) {
        try {
            return workspace.act(new BranchCallable(workspace.getRemote()));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static User mapUser(PersonIdent ident, Collection<User> users) {
        String[] split = ident.getName().split(" ");
        for (User user : users) {
            GameUserProperty property = user.getProperty(GameUserProperty.class);
            if (
                    (property != null && property.getGitNames().contains(ident.getName()))
                    || (user.getFullName().contains(split[0]) && user.getFullName().contains(split[split.length - 1]))
                    || (user.getProperty(Mailer.UserProperty.class) != null
                    && ident.getEmailAddress().equals(user.getProperty(Mailer.UserProperty.class).getAddress()))
            ) {
                return user;
            }
        }
        return null;
    }

    public static GameUser mapUser(PersonIdent ident, ArrayList<GameUser> users) {
        String[] split = ident.getName().split(" ");
        for (GameUser user : users) {
            if (user.getGitNames().contains(ident.getName())
                    || (user.getFullName().contains(split[0]) && user.getFullName().contains(split[split.length - 1]))
                    || ident.getEmailAddress().equals(user.getMail())) {
                return user;
            }
        }
        return null;
    }

    //TODO: Not performant - maybe JGit starts to search from the HEAD every time?
    //TODO: Looking at the time it seems random...
    //TODO: Maybe some commits have many parents?
    public static ArrayList<JacocoUtil.ClassDetails> getLastChangedClasses(int count, HashMap<String,
            String> constants, TaskListener listener, ArrayList<GameUser> users, FilePath workspace)
            throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.setGitDir(new File(workspace.getRemote() + "/.git"))
                .setMustExist(true).build();
        RevWalk walk = new RevWalk(repo);

        RevCommit headCommit = getHead(repo);
        Git git = new Git(repo);

        int totalCount = 0;
        HashSet<RevCommit> currentCommits = new HashSet<>();
        HashSet<RevCommit> searchedCommits = new HashSet<>();
        currentCommits.add(headCommit);

        ArrayList<JacocoUtil.ClassDetails> classes = new ArrayList<>();
        HashMap<PersonIdent, GameUser> authorMapping = new HashMap<>();

        while (totalCount < count) {
            listener.getLogger().println("[Gamekins] Searched through " + totalCount + " Commits");
            if (currentCommits.isEmpty()) break;
            HashSet<RevCommit> newCommits = new HashSet<>();
            for (RevCommit commit : currentCommits) {
                searchedCommits.add(commit);
                String diff = getDiffOfCommit(git, repo, commit);

                String[] lines = diff.split("\n");
                for (String line : lines) {
                    if (commit.getShortMessage().toLowerCase().contains("merge")) break;
                    //TODO: Shows diff of some merge requests, but not all
                    if (line.contains("diff --git")) {
                        String path = line.split(" ")[2].substring(1);
                        String[] pathSplit = path.split("/");
                        if (Arrays.asList(path.split("/")).contains("test")
                                || !(path.contains(".java") || path.contains(".kt"))) {
                            continue;
                        }
                        String classname = pathSplit[pathSplit.length - 1].split("\\.")[0];

                        boolean found = false;
                        for (JacocoUtil.ClassDetails details : classes) {
                            if (details.getClassName().equals(classname)) {
                                GameUser user = authorMapping.get(commit.getAuthorIdent());
                                if (user == null) {
                                    user = mapUser(commit.getAuthorIdent(), users);
                                    if (user == null) {
                                        found = true;
                                        break;
                                    }
                                    authorMapping.put(commit.getAuthorIdent(), user);
                                }
                                details.addUser(user);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            JacocoUtil.ClassDetails details = new JacocoUtil.ClassDetails(workspace, path,
                                    constants.get("jacocoResultsPath"), constants.get("jacocoCSVPath"), listener);
                            GameUser user = authorMapping.get(commit.getAuthorIdent());
                            if (user == null) {
                                user = mapUser(commit.getAuthorIdent(), users);
                                if (user == null) continue;
                                authorMapping.put(commit.getAuthorIdent(), user);
                            }
                            details.addUser(user);
                            classes.add(details);
                        }
                    }
                }

                for (RevCommit parent : commit.getParents()) {
                    if (!searchedCommits.contains(parent) && !newCommits.contains(parent)
                            && !currentCommits.contains(parent)) {
                        newCommits.add(walk.parseCommit(repo.resolve(parent.getName())));
                    }
                    walk.dispose();
                }
                totalCount++;
            }

            currentCommits = new HashSet<>(newCommits);
        }

        return classes;
    }

    public static ArrayList<GameUser> mapUsersToGameUsers(Collection<User> users) {
        ArrayList<GameUser> gameUsers = new ArrayList<>();
        for (User user : users) {
            gameUsers.add(new GameUser(user));
        }
        return gameUsers;
    }

    private static class BranchCallable extends MasterToSlaveCallable<String, IOException> {

        private final String workspace;

        private BranchCallable(String workspace) {
            this.workspace = workspace;
        }

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        @Override
        public String call() {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try {
                Repository repo = builder.setGitDir(new File(workspace + "/.git")).setMustExist(true).build();
                return repo.getBranch();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    private static class LastChangedFilesCallable extends MasterToSlaveCallable<Set<String>, IOException> {


        private final String workspace;
        private final GameUser user;
        private final int commitCount;
        private final String commitHash;
        private final ArrayList<GameUser> users;

        private LastChangedFilesCallable(String workspace, GameUser user, int commitCount,
                                         String commitHash, ArrayList<GameUser> users) {
            this.workspace = workspace;
            this.user = user;
            this.commitCount = commitCount;
            this.commitHash = commitHash;
            this.users = users;
        }

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        @Override
        public Set<String> call() throws IOException {
            return getLastChangedFilesOfUser(this.workspace, this.user, this.commitCount, this.commitHash, this.users);
        }
    }

    public static class LastChangedClassesCallable
            extends MasterToSlaveCallable<ArrayList<JacocoUtil.ClassDetails>, IOException> {

        private final int count;
        private  final HashMap<String, String> constants;
        private final TaskListener listener;
        private final ArrayList<GitUtil.GameUser> users;
        private final FilePath workspace;

        public LastChangedClassesCallable(int count, HashMap<String, String> constants, TaskListener listener,
                                          ArrayList<GitUtil.GameUser> users, FilePath workspace) {
            this.count = count;
            this.constants = constants;
            this.listener = listener;
            this.users = users;
            this.workspace = workspace;
        }

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        @Override
        public ArrayList<JacocoUtil.ClassDetails> call() throws IOException {
            return GitUtil.getLastChangedClasses(this.count, this.constants, this.listener, this.users,
                    this.workspace);
        }
    }

    public static class HeadCommitCallable extends MasterToSlaveCallable<RevCommit, IOException> {

        private final String workspace;

        public HeadCommitCallable(String workspace) {
            this.workspace = workspace;
        }

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        @Override
        public RevCommit call() throws IOException {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repo = builder.setGitDir(new File(workspace + "/.git")).setMustExist(true).build();
            return GitUtil.getHead(repo);
        }
    }

    public static class GameUser implements Serializable {

        private final String id;
        private final String fullName;
        private final String mail;
        private final HashSet<String> gitNames;

        public GameUser(User user) {
            this.id = user.getId();
            this.fullName = user.getFullName();
            this.mail = user.getProperty(Mailer.UserProperty.class) == null ? ""
                    : user.getProperty(Mailer.UserProperty.class).getAddress();
            this.gitNames = user.getProperty(GameUserProperty.class) == null ? new HashSet<>()
                    : new HashSet<>(user.getProperty(GameUserProperty.class).getGitNames());
        }

        public String getId() {
            return id;
        }

        public String getFullName() {
            return fullName;
        }

        public String getMail() {
            return mail;
        }

        public HashSet<String> getGitNames() {
            return gitNames;
        }

        //TODO: Not callable on remote machines
        public User getUser() {
            for (User user : User.getAll()) {
                if (user.getId().equals(this.id)) return user;
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GameUser gameUser = (GameUser) o;
            return id.equals(gameUser.id) &&
                    fullName.equals(gameUser.fullName) &&
                    mail.equals(gameUser.mail) &&
                    gitNames.equals(gameUser.gitNames);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, fullName, mail, gitNames);
        }
    }
}
