package io.jenkins.plugins.gamekins.challenge;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.User;
import hudson.tasks.Mailer;
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
import org.jsoup.nodes.Document;

import java.io.*;
import java.util.*;

public class ChallengeFactory {

    private ChallengeFactory() {

    }

    public static Challenge generateChallenge(AbstractBuild<?, ?> build, User user) throws IOException {
        if (build.getResult() != Result.SUCCESS && Math.random() > 0.5) {
            return new BuildChallenge();
        }

        String workspace = build.getWorkspace().getRemote();
        ArrayList<String> lastChangedFilesOfUser = new ArrayList<>(getLastChangedFilesOfUser(workspace, user, 10));
        if (lastChangedFilesOfUser.size() == 0) {
            lastChangedFilesOfUser = new ArrayList<>(getLastChangedFilesOfUser(workspace, user, 100));
            if (lastChangedFilesOfUser.size() == 0) {
                return new DummyChallenge();
            }
        }

        ArrayList<Double> coverageValues = getCoverageInPercentageFromJacoco(lastChangedFilesOfUser, workspace);
        ArrayList<CoverageFiles> files = new ArrayList<>();
        for (int i = 0; i < lastChangedFilesOfUser.size(); i++) {
            files.add(new CoverageFiles(lastChangedFilesOfUser.get(i), coverageValues.get(i)));
        }
        files.removeIf(coverageFiles -> coverageFiles.coverage == 1.0);
        files.sort(Comparator.comparingDouble(covFile -> covFile.coverage));
        Collections.reverse(files);
        ArrayList<CoverageFiles> worklist = new ArrayList<>(files);

        final double c = 1.5;
        double[] rankValues = new double[worklist.size()];
        for (int i = 0; i < worklist.size(); i++) {
            rankValues[i] = (2 - c + 2 * (c - 1) * (i / (double) (worklist.size() - 1))) / (double) worklist.size();
            if (i != 0) rankValues[i] += rankValues[i - 1];
        }

        //TODO: Generate other Challenges
        Challenge challenge;
        Random random = new Random();
        do {
            double probability = Math.random();
            CoverageFiles selectedClass = worklist.get(worklist.size() - 1);
            for (int i = 0; i < worklist.size(); i++) {
                if (rankValues[i] > probability) {
                    selectedClass = worklist.get(i);
                    break;
                }
            }
            //TODO: Make more beautiful
            int challengeType = random.nextInt(3);
            Class challengeClass;
            if (challengeType == 0) {
                challengeClass = ClassCoverageChallenge.class;
            } else {
                challengeClass = LineCoverageChallenge.class;
            }
            challenge = generateCoverageChallenge(workspace, selectedClass.file, challengeClass);
            worklist.remove(selectedClass);
        } while (challenge == null);

        return challenge;
    }

    //TODO: Create Enum
    private static CoverageChallenge generateCoverageChallenge(String workspace, String path, Class challengeClass)
            throws IOException {
        //TODO: Change path for different build tools
        String filePath = workspace + "/target/site/jacoco/";
        StringBuilder packageName = new StringBuilder();
        String className = "";
        for (String part : path.split("/")) {
            if (part.contains(".java")) {
                packageName.deleteCharAt(packageName.length() - 1);
                className = part.split("\\.")[0];
                break;
            }
            if (!part.equals("src") && !part.equals("java") && !part.equals("main") && !part.isEmpty()) {
                packageName.append(part).append(".");
            }
        }
        Document document = CoverageChallenge.generateDocument(
                filePath + packageName + "/" + className + ".java.html", "UTF-8");
        if (CoverageChallenge.calculateCoveredLines(document, "pc") > 0
                || CoverageChallenge.calculateCoveredLines(document, "nc") > 0) {
            if (challengeClass == ClassCoverageChallenge.class) {
                return new ClassCoverageChallenge(filePath + packageName, className);
            } else {
                return new LineCoverageChallenge(filePath + packageName, className);
            }
        }
        return null;
    }

    private static RevCommit getHead(Repository repo) throws IOException {
        RevWalk walk = new RevWalk(repo);

        ObjectId head = repo.resolve(Constants.HEAD);
        RevCommit headCommit = walk.parseCommit(head);
        walk.dispose();

        return headCommit;
    }

    private static Set<String> getLastChangedFilesOfUser(String workspace, User user, int commitCount) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.setGitDir(new File(workspace + "/.git")).setMustExist(true).build();
        RevWalk walk = new RevWalk(repo);

        RevCommit headCommit = getHead(repo);
        Git git = new Git(repo);

        int countUserCommit = 0;
        int totalCount = 0;
        ArrayList<RevCommit> currentCommits = new ArrayList<>();
        currentCommits.add(headCommit);
        LinkedHashSet<String> pathsToFiles = new LinkedHashSet<>();

        while (countUserCommit < commitCount && totalCount < commitCount * 5) {
            if (currentCommits.isEmpty()) break;
            ArrayList<RevCommit> newCommits = new ArrayList<>();
            for (RevCommit commit : currentCommits) {
                if (commit.getAuthorIdent().getName().equals(user.getFullName())
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
            currentCommits = new ArrayList<>(newCommits);
            totalCount++;
        }

        if (!pathsToFiles.isEmpty()) {
            pathsToFiles.removeIf(path -> Arrays.asList(path.split("/")).contains("test"));
            pathsToFiles.removeIf(path -> !path.contains(".java"));
        }

        return pathsToFiles;
    }

    //Helper gets the diff as a string.
    private static String getDiffOfCommit(Git git, Repository repo, RevCommit newCommit) throws IOException {

        //TODO: Get diff from first commit
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

    private static ArrayList<Double> getCoverageInPercentageFromJacoco(List<String> paths, String workspace) {
        String filePath = workspace + "/target/site/jacoco/jacoco.csv";
        List<List<String>> records = new ArrayList<>();
        ArrayList<Double> coverageValues = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }

            for (String path : paths) {
                String[] split = path.split("/");
                for (List<String> coverageLine : records ) {
                    //TODO: Improve
                    if (split[split.length - 1].contains(coverageLine.get(2))) {
                        double value = Double.parseDouble(coverageLine.get(4))
                                / (Double.parseDouble(coverageLine.get(3))
                                + Double.parseDouble(coverageLine.get(4)));
                        coverageValues.add(value);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return coverageValues;
    }

    static class CoverageFiles {

        final String file;
        final double coverage;

        CoverageFiles(String file, double coverage) {
            this.file = file;
            this.coverage = coverage;
        }
    }
}
