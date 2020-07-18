package io.jenkins.plugins.gamekins.challenge;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.model.User;
import io.jenkins.plugins.gamekins.util.GitUtil;
import io.jenkins.plugins.gamekins.util.JacocoUtil;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

public class ChallengeFactory {

    private ChallengeFactory() {

    }

    public static Challenge generateChallenge(User user, HashMap<String, String> constants, TaskListener listener,
                                              ArrayList<JacocoUtil.ClassDetails> classes, FilePath workspace)
            throws IOException, InterruptedException {
        if (Math.random() > 0.9) {
            listener.getLogger().println("[Gamekins] Generated new TestChallenge");
            return new TestChallenge(workspace.act(new GitUtil.HeadCommitCallable(workspace.getRemote())).getName(),
                    JacocoUtil.getTestCount(workspace), user, constants.get("branch"));
        }


        ArrayList<JacocoUtil.ClassDetails> worklist = new ArrayList<>(classes);

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
            listener.getLogger().println("[Gamekins] Try class " + selectedClass.getClassName() + " and type "
                    + challengeClass.getSimpleName());
            challenge = generateCoverageChallenge(selectedClass, challengeClass, constants.get("branch"),
                    listener, workspace);
            if (challenge instanceof MethodCoverageChallenge
                    && ((MethodCoverageChallenge) challenge).getMethodName() == null) {
                challenge = null;
            }
            worklist.remove(selectedClass);
            count ++;
        } while (challenge == null);

        return challenge;
    }

    //TODO: Create Enum
    private static CoverageChallenge generateCoverageChallenge(JacocoUtil.ClassDetails classDetails,
                                                               Class challengeClass, String branch,
                                                               TaskListener listener, FilePath workspace)
            throws IOException, InterruptedException {
        Document document;
        try {
            document = JacocoUtil.generateDocument(JacocoUtil.calculateCurrentFilePath(workspace,
                    classDetails.getJacocoSourceFile(), classDetails.getWorkspace()));
        } catch (IOException | InterruptedException e) {
            listener.getLogger().println("[Gamekins] IOException with JaCoCoSourceFile "
                    + classDetails.getJacocoSourceFile().getAbsolutePath());
            e.printStackTrace(listener.getLogger());
            throw e;
        }
        if (JacocoUtil.calculateCoveredLines(document, "pc") > 0
                || JacocoUtil.calculateCoveredLines(document, "nc") > 0) {
            if (challengeClass == ClassCoverageChallenge.class) {
                return new ClassCoverageChallenge(classDetails, branch, workspace);
            } else if (challengeClass == MethodCoverageChallenge.class) {
                return new MethodCoverageChallenge(classDetails, branch, workspace);
            } else {
                return new LineCoverageChallenge(classDetails, branch, workspace);
            }
        }
        return null;
    }
}
