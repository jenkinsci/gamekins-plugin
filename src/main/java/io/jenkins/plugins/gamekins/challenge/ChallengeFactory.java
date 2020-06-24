package io.jenkins.plugins.gamekins.challenge;

import hudson.model.TaskListener;
import hudson.model.User;
import io.jenkins.plugins.gamekins.util.GitUtil;
import io.jenkins.plugins.gamekins.util.JacocoUtil;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jsoup.nodes.Document;

import java.io.*;
import java.util.*;

public class ChallengeFactory {

    private ChallengeFactory() {

    }

    public static Challenge generateChallenge(User user, HashMap<String, String> constants, TaskListener listener) throws IOException {
        if (Math.random() > 0.9) {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repo = builder.setGitDir(
                    new File(constants.get("workspace") + "/.git")).setMustExist(true).build();
            return new TestChallenge(GitUtil.getHead(repo).getName(), JacocoUtil.getTestCount(constants),
                    user, constants.get("branch"));
        }

        ArrayList<String> lastChangedFilesOfUser = new ArrayList<>(GitUtil.getLastChangedSourceFilesOfUser(
                constants.get("workspace"), user, 10, ""));
        listener.getLogger().println("[Gamekins] Found " + lastChangedFilesOfUser.size() + " last changed files of user " + user.getFullName());
        if (lastChangedFilesOfUser.size() == 0) {
            lastChangedFilesOfUser = new ArrayList<>(GitUtil.getLastChangedSourceFilesOfUser(
                    constants.get("workspace"), user, 40, ""));
            listener.getLogger().println("[Gamekins] Found " + lastChangedFilesOfUser.size() + " last changed files of user " + user.getFullName());
            if (lastChangedFilesOfUser.size() == 0) {
                return new DummyChallenge();
            }
        }

        ArrayList<JacocoUtil.ClassDetails> files = new ArrayList<>();
        for (String file : lastChangedFilesOfUser) {
            files.add(new JacocoUtil.ClassDetails(constants.get("workspace"), file,
                    constants.get("jacocoResultsPath"), constants.get("jacocoCSVPath")));
        }
        files.removeIf(classDetails -> classDetails.getCoverage() == 1.0);
        listener.getLogger().println("[Gamekins] Found " + files.size() + " files without 100% coverage of user " + user.getFullName());
        if (files.size() == 0) return new DummyChallenge();
        files.sort(Comparator.comparingDouble(JacocoUtil.ClassDetails::getCoverage));
        Collections.reverse(files);
        ArrayList<JacocoUtil.ClassDetails> worklist = new ArrayList<>(files);

        final double c = 1.5;
        double[] rankValues = new double[worklist.size()];
        for (int i = 0; i < worklist.size(); i++) {
            rankValues[i] = (2 - c + 2 * (c - 1) * (i / (double) (worklist.size() - 1))) / (double) worklist.size();
            if (i != 0) rankValues[i] += rankValues[i - 1];
        }

        Challenge challenge;
        Random random = new Random();
        int count = 0;
        do {
            if (count == 5 || worklist.isEmpty()) {
                listener.getLogger().println("[Gamekins] No CoverageChallenge could be built");
                return new DummyChallenge();
            }
            double probability = Math.random();
            JacocoUtil.ClassDetails selectedClass = worklist.get(worklist.size() - 1);
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
            listener.getLogger().println("[Gamekins] Try class " + selectedClass + " and type " + challengeClass.getName());
            challenge = generateCoverageChallenge(selectedClass, challengeClass, constants.get("branch"));
            worklist.remove(selectedClass);
            count ++;
        } while (challenge == null);

        return challenge;
    }

    //TODO: Create Enum
    private static CoverageChallenge generateCoverageChallenge(JacocoUtil.ClassDetails classDetails,
                                                               Class challengeClass, String branch)
            throws IOException {
        Document document = JacocoUtil.generateDocument(classDetails.getJacocoSourceFile(), "UTF-8");
        if (JacocoUtil.calculateCoveredLines(document, "pc") > 0
                || JacocoUtil.calculateCoveredLines(document, "nc") > 0) {
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
}
