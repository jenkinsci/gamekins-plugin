package io.jenkins.plugins.gamekins.challenge;

import hudson.FilePath;
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
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class ChallengeFactory {

    private ChallengeFactory() {

    }

    public static Challenge generateChallenge(User user, HashMap<String, String> constants) throws IOException {
        if (Math.random() > 0.9) {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repo = builder.setGitDir(
                    new File(constants.get("workspace") + "/.git")).setMustExist(true).build();
            return new TestChallenge(getHead(repo).getName(), getTestCount(constants), user, constants.get("branch"));
        }

        ArrayList<String> lastChangedFilesOfUser = new ArrayList<>(getLastChangedSourceFilesOfUser(
                constants.get("workspace"), user, 10, ""));
        if (lastChangedFilesOfUser.size() == 0) {
            lastChangedFilesOfUser = new ArrayList<>(getLastChangedSourceFilesOfUser(
                    constants.get("workspace"), user, 100, ""));
            if (lastChangedFilesOfUser.size() == 0) {
                return new DummyChallenge();
            }
        }

        ArrayList<ClassDetails> files = new ArrayList<>();
        for (String file : lastChangedFilesOfUser) {
            files.add(new ClassDetails(constants.get("workspace"), file, constants.get("jacocoResultsPath"), constants.get("jacocoCSVPath")));
        }
        files.removeIf(classDetails -> classDetails.coverage == 1.0);
        if (files.size() == 0) return new DummyChallenge();
        files.sort(Comparator.comparingDouble(covFile -> covFile.coverage));
        Collections.reverse(files);
        ArrayList<ClassDetails> worklist = new ArrayList<>(files);

        final double c = 1.5;
        double[] rankValues = new double[worklist.size()];
        for (int i = 0; i < worklist.size(); i++) {
            rankValues[i] = (2 - c + 2 * (c - 1) * (i / (double) (worklist.size() - 1))) / (double) worklist.size();
            if (i != 0) rankValues[i] += rankValues[i - 1];
        }

        Challenge challenge;
        Random random = new Random();
        do {
            double probability = Math.random();
            ClassDetails selectedClass = worklist.get(worklist.size() - 1);
            for (int i = 0; i < worklist.size(); i++) {
                if (rankValues[i] > probability) {
                    selectedClass = worklist.get(i);
                    break;
                }
            }
            //TODO: Make more beautiful
            int challengeType = random.nextInt(4);
            Class challengeClass;
            if (challengeType == 0) {
                challengeClass = ClassCoverageChallenge.class;
            } else if (challengeType == 1) {
                challengeClass = MethodCoverageChallenge.class;
            } else {
                challengeClass = LineCoverageChallenge.class;
            }
            challenge = generateCoverageChallenge(selectedClass, challengeClass, constants.get("branch"));
            worklist.remove(selectedClass);
        } while (challenge == null);

        return challenge;
    }

    //TODO: Create Enum
    private static CoverageChallenge generateCoverageChallenge(ClassDetails classDetails, Class challengeClass,
                                                               String branch) throws IOException {
        Document document = CoverageChallenge.generateDocument(classDetails.jacocoSourceFile, "UTF-8");
        if (CoverageChallenge.calculateCoveredLines(document, "pc") > 0
                || CoverageChallenge.calculateCoveredLines(document, "nc") > 0) {
            if (challengeClass == ClassCoverageChallenge.class) {
                return new ClassCoverageChallenge(classDetails, branch);
            } else if (challengeClass == MethodCoverageChallenge.class) {
                return new MethodCoverageChallenge(classDetails, branch);
            } else {
                return new LineCoverageChallenge(classDetails, branch);
            }
        }
        return null;
    }

    public static String getFullPath(String workspace, String partPath, boolean file) {
        if (partPath.startsWith("**/")) {
            String path = workspace + partPath.substring(2);
            if (!file && !path.endsWith("/")) path += "/";
            return path;
        }
        return partPath;
    }

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

    static Set<String> getLastChangedSourceFilesOfUser(String workspace, User user, int commitCount,
                                                       String commitHash) throws IOException {
        Set<String> pathsToFiles = getLastChangedFilesOfUser(workspace, user, commitCount, commitHash);
        if (!pathsToFiles.isEmpty()) {
            pathsToFiles.removeIf(path -> Arrays.asList(path.split("/")).contains("test"));
            pathsToFiles.removeIf(path -> !(path.contains(".java") || path.contains(".kt")));
        }
        return pathsToFiles;
    }

    static Set<String> getLastChangedTestFilesOfUser(String workspace, User user, int commitCount,
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

    public static RevCommit getPrevHash(Repository repo, RevCommit commit)  throws  IOException {
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

    private static Double getCoverageInPercentageFromJacoco(String className, File csv) {
        List<List<String>> records = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(csv));
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
            for (List<String> coverageLine : records ) {
                //TODO: Improve
                if (className.contains(coverageLine.get(2))) {
                    return Double.parseDouble(coverageLine.get(4))
                            / (Double.parseDouble(coverageLine.get(3))
                            + Double.parseDouble(coverageLine.get(4)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    //TODO: Check sub directories
    //TODO: Use JUnit-Plugin if possible
    static int getTestCount(HashMap<String, String> constants) {
        try {
            List<FilePath> files = getFilesInAllSubDirectories(constants.get("workspace"), "TEST-.+\\.xml");
            int testCount = 0;
            for (FilePath file : files) {
                StringBuilder xml = new StringBuilder();
                try (Stream<String> stream = Files.lines( Paths.get(file.getRemote()), StandardCharsets.UTF_8)) {
                    stream.forEach(s -> xml.append(s).append("\n"));
                }

                Document document = Jsoup.parse(xml.toString(), "", Parser.xmlParser());
                Elements elements = document.select("testsuite");
                testCount += Integer.parseInt(elements.first().attr("tests"));
            }
            return testCount;
        } catch (IOException ignored) { }

        return 0;
    }

    public static ArrayList<FilePath> getFilesInAllSubDirectories(String directory, String regex) {
        FilePath rootPath = new FilePath(new File(directory));
        ArrayList<FilePath> files = new ArrayList<>();
        try {
            for (FilePath path : rootPath.list()) {
                if (path.isDirectory()) {
                    files.addAll(getFilesInAllSubDirectories(path.getRemote(), regex));
                } else {
                    if (path.getName().matches(regex)) files.add(path);
                }
            }
        } catch (IOException | InterruptedException ignored) {
            return new ArrayList<>();
        }
        return files;
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

    static class ClassDetails {

        final String className;
        final String extension;
        final String packageName;
        final File jacocoMethodFile;
        final File jacocoSourceFile;
        final File jacocoCSVFile;
        final File file;
        final double coverage;

        /**
         *
         * @param workspace Workspace of the project
         * @param shortFilePath Path of the file, starting in the workspace root directory
         * @param shortJacocoPath Path of the JaCoCo root directory, beginning with ** / (without space)
         * @param shortJacocoCSVPath Path of the JaCoCo csv file, beginning with ** / (without space)
         */
        ClassDetails(String workspace, String shortFilePath, String shortJacocoPath, String shortJacocoCSVPath) {
            ArrayList<String> pathSplit = new ArrayList<>(Arrays.asList(shortFilePath.split("/")));
            this.className = pathSplit.get(pathSplit.size() - 1).split("\\.")[0];
            this.extension = pathSplit.get(pathSplit.size() - 1).split("\\.")[1];
            this.packageName = computePackageName(shortFilePath);
            String jacocoPath = workspace;
            int i = 0;
            while (!pathSplit.get(i).equals("src")) {
                if (!pathSplit.get(i).isEmpty()) jacocoPath = jacocoPath + "/" + pathSplit.get(i);
                i++;
            }
            this.jacocoCSVFile = new File(jacocoPath + shortJacocoCSVPath.substring(2));
            jacocoPath = jacocoPath + shortJacocoPath.substring(2) + this.packageName + "/";
            this.jacocoMethodFile = new File(jacocoPath + this.className + ".html");
            this.jacocoSourceFile = new File(jacocoPath + this.className + "." + this.extension + ".html");
            this.file = new File(workspace + shortFilePath);
            this.coverage = getCoverageInPercentageFromJacoco(this.className, this.jacocoCSVFile);
        }

        static String computePackageName(String shortFilePath) {
            ArrayList<String> pathSplit = new ArrayList<>(Arrays.asList(shortFilePath.split("/")));
            String packageName = "";
            for (int i = pathSplit.size() - 2; i >= 0; i--) {
                if (pathSplit.get(i).equals("src") || pathSplit.get(i).equals("main") || pathSplit.get(i).equals("java")) {
                    packageName = packageName.substring(1);
                    break;
                }
                packageName = "." + pathSplit.get(i) + packageName;
            }
            return  packageName;
        }
    }
}
