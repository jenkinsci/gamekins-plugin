package io.jenkins.plugins.gamekins.challenge;

import hudson.model.User;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class ChallengeFactory {

    private ChallengeFactory() {

    }

    public static Challenge generateChallenge(String workspace, User user) throws IOException {
        Set<String> lastChangedFilesOfUser = getLastChangedFilesOfUser(workspace, user);

        return null;
    }

    private static Set<String> getLastChangedFilesOfUser(String workspace, User user) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.setGitDir(new File(workspace + "/.git")).setMustExist(true).build();
        RevWalk walk = new RevWalk(repo);

        ObjectId head = repo.resolve(Constants.HEAD);
        RevCommit headCommit = walk.parseCommit(head);
        walk.dispose();
        Git git = new Git(repo);

        int countUserCommit = 0;
        int totalCount = 0;
        RevCommit currentCommit = headCommit;
        LinkedHashSet<String> pathsToFiles = new LinkedHashSet<>();

        while (countUserCommit < 10 && totalCount < 100) {
            if (currentCommit == null) break;
            if (currentCommit.getAuthorIdent().getName().equals(user.getFullName())) {
                String diff = getDiffOfCommit(git, repo, currentCommit);

                String[] lines = diff.split("\n");
                for (String line : lines) {
                    if (line.contains("diff --git")) {
                        pathsToFiles.add(line.split(" ")[2].substring(1));
                    }
                }

                countUserCommit++;
            }

            //TODO: Case with more than one parent?
            currentCommit = currentCommit.getParentCount() == 0
                    ? null
                    : walk.parseCommit(repo.resolve(currentCommit.getParent(0).getName()));
            walk.dispose();
            totalCount++;
        }

        if (!pathsToFiles.isEmpty()) {
            pathsToFiles.removeIf(path -> Arrays.asList(path.split("/")).contains("test"));
        }

        return pathsToFiles;
    }

    //Helper gets the diff as a string.
    private static String getDiffOfCommit(Git git, Repository repo, RevCommit newCommit) throws IOException {

        //Get commit that is previous to the current one.
        RevCommit oldCommit = getPrevHash(repo, newCommit);
        if(oldCommit == null){
            return "Start of repo";
        }
        //Use treeIterator to diff.
        AbstractTreeIterator oldTreeIterator = getCanonicalTreeParser(git, oldCommit);
        AbstractTreeIterator newTreeIterator = getCanonicalTreeParser(git, newCommit);
        OutputStream outputStream = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(outputStream)) {
            formatter.setRepository(git.getRepository());
            formatter.format(oldTreeIterator, newTreeIterator);
        }
        return outputStream.toString();
    }
    //Helper function to get the previous commit.
    public static RevCommit getPrevHash(Repository repo, RevCommit commit)  throws  IOException {

        try (RevWalk walk = new RevWalk(repo)) {
            // Starting point
            walk.markStart(commit);
            int count = 0;
            for (RevCommit rev : walk) {
                // got the previous commit.
                if (count == 1) {
                    return rev;
                }
                count++;
            }
            walk.dispose();
        }
        //Reached end and no previous commits.
        return null;
    }
    //Helper function to get the tree of the changes in a commit. Written by Rüdiger Herrmann
    private static AbstractTreeIterator getCanonicalTreeParser(Git git, ObjectId commitId) throws IOException {
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            RevCommit commit = walk.parseCommit(commitId);
            ObjectId treeId = commit.getTree().getId();
            try (ObjectReader reader = git.getRepository().newObjectReader()) {
                return new CanonicalTreeParser(null, reader, treeId);
            }
        }
    }
}
