package io.jenkins.plugins.gamekins.util;

import hudson.model.User;
import hudson.tasks.Mailer;
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

    public static Set<String> getLastChangedSourceFilesOfUser(String workspace, User user, int commitCount,
                                                              String commitHash) throws IOException {
        Set<String> pathsToFiles = getLastChangedFilesOfUser(workspace, user, commitCount, commitHash);
        if (!pathsToFiles.isEmpty()) {
            pathsToFiles.removeIf(path -> Arrays.asList(path.split("/")).contains("test"));
            pathsToFiles.removeIf(path -> !(path.contains(".java") || path.contains(".kt")));
        }
        return pathsToFiles;
    }

    public static Set<String> getLastChangedTestFilesOfUser(String workspace, User user, int commitCount,
                                                            String commitHash) throws IOException {
        Set<String> pathsToFiles = getLastChangedFilesOfUser(workspace, user, commitCount, commitHash);
        if (!pathsToFiles.isEmpty()) {
            pathsToFiles.removeIf(path -> !Arrays.asList(path.split("/")).contains("test"));
            pathsToFiles.removeIf(path -> !(path.contains(".java") || path.contains(".kt")));
        }
        return pathsToFiles;
    }

    private static Set<String> getLastChangedFilesOfUser(String workspace, User user, int commitCount,
                                                         String commitHash) throws IOException {
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
                if (GitUtil.userEquals(commit.getAuthorIdent().getName(), user.getFullName())
                        || commit.getAuthorIdent().getEmailAddress()
                        .equals(user.getProperty(Mailer.UserProperty.class).getAddress())) {
                    String diff = getDiffOfCommit(git, repo, commit);

                    String[] lines = diff.split("\n");
                    for (String line : lines) {
                        if (line.contains("diff --git")) {
                            pathsToFiles.add(line.split(" ")[2].substring(1));
                        }
                    }

                    countUserCommit++;
                }

                //TODO: Adjustment to git branches necessary?
                for (RevCommit parent : commit.getParents()) {
                    newCommits.add(walk.parseCommit(repo.resolve(parent.getName())));
                    walk.dispose();
                }
            }

            if (targetCommit != null && newCommits.contains(targetCommit)) break;

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

    public static String getBranch(String workspace) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            Repository repo = builder.setGitDir(new File(workspace + "/.git")).setMustExist(true).build();
            return repo.getBranch();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean userEquals(String commitUsername, String jenkinsUserName) {
        if (commitUsername.equals(jenkinsUserName)) return true;
        String[] split = commitUsername.split(" ");
        return jenkinsUserName.contains(split[0]) && jenkinsUserName.contains(split[split.length - 1]);
    }

    public static User mapUser(PersonIdent ident) {
        String[] split = ident.getName().split(" ");
        for (User user : User.getAll()) {
            if ((user.getFullName().contains(split[0]) && user.getFullName().contains(split[split.length - 1]))
                    || ident.getEmailAddress().equals(user.getProperty(Mailer.UserProperty.class).getAddress())) {
                return user;
            }
        }
        return null;
    }

    public static ArrayList<JacocoUtil.ClassDetails> getLastChangedFiles(int count, HashMap<String, String> constants)
            throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.setGitDir(new File(constants.get("workspace") + "/.git"))
                .setMustExist(true).build();
        RevWalk walk = new RevWalk(repo);

        RevCommit headCommit = getHead(repo);
        Git git = new Git(repo);

        int totalCount = 0;
        ArrayList<RevCommit> currentCommits = new ArrayList<>();
        currentCommits.add(headCommit);

        ArrayList<JacocoUtil.ClassDetails> classes = new ArrayList<>();
        HashMap<PersonIdent, User> authorMapping = new HashMap<>();

        while (totalCount < count) {
            if (currentCommits.isEmpty()) break;
            ArrayList<RevCommit> newCommits = new ArrayList<>();
            for (RevCommit commit : currentCommits) {
                String diff = getDiffOfCommit(git, repo, commit);

                String[] lines = diff.split("\n");
                for (String line : lines) {
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
                                User user = authorMapping.get(commit.getAuthorIdent());
                                if (user == null) {
                                    user = mapUser(commit.getAuthorIdent());
                                    authorMapping.put(commit.getAuthorIdent(), user);
                                }
                                details.addUser(user);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            JacocoUtil.ClassDetails details = new JacocoUtil.ClassDetails(constants.get("workspace"),
                                    path, constants.get("jacocoResultsPath"), constants.get("jacocoCSVPath"));
                            User user = authorMapping.get(commit.getAuthorIdent());
                            if (user == null) {
                                user = mapUser(commit.getAuthorIdent());
                                authorMapping.put(commit.getAuthorIdent(), user);
                            }
                            details.addUser(user);
                            classes.add(details);
                        }
                    }
                }

                for (RevCommit parent : commit.getParents()) {
                    newCommits.add(walk.parseCommit(repo.resolve(parent.getName())));
                    walk.dispose();
                }
            }

            currentCommits = new ArrayList<>(newCommits);
            totalCount++;
        }

        return classes;
    }
}
