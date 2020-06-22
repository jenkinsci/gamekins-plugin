package io.jenkins.plugins.gamekins.statistics;

import hudson.model.*;
import io.jenkins.plugins.gamekins.GameUserProperty;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;

import java.util.ArrayList;

public class Statistics {

    private final String projectName;
    private final ArrayList<RunEntry> runEntries;

    public Statistics(AbstractItem job) {
        this.projectName = job.getName();
        this.runEntries = generateRunEntries(job);
    }

    public String printToXML() {
        StringBuilder print = new StringBuilder();
        print.append("<Statistics project=\"").append(this.projectName).append("\">\n");
        ArrayList<User> users = new ArrayList<>(User.getAll());
        users.removeIf(user -> !user.getProperty(GameUserProperty.class).isParticipating(this.projectName));
        print.append("    <Users count=\"").append(users.size()).append("\">\n");
        for (User user : users) {
            GameUserProperty property = user.getProperty(GameUserProperty.class);
            if (property != null && property.isParticipating(this.projectName)) {
                print.append(property.printToXML(this.projectName, "        ")).append("\n");
            }
        }
        print.append("    </Users>\n");
        print.append("    <Runs count=\"").append(this.runEntries.size()).append("\">\n");
        for (RunEntry entry : this.runEntries) {
            print.append(entry.printToXML("        ")).append("\n");
        }
        print.append("    </Runs>\n");
        print.append("</Statistics>");
        return print.toString();
    }

    private ArrayList<RunEntry> generateRunEntries(AbstractItem job) {
        ArrayList<RunEntry> entries = new ArrayList<>();
        if (job instanceof WorkflowMultiBranchProject) {
            for (WorkflowJob workflowJob : ((WorkflowMultiBranchProject) job).getItems()) {
                for (WorkflowRun workflowRun : workflowJob.getBuilds()) {
                    entries.add(new RunEntry(
                            workflowRun.getNumber(),
                            workflowJob.getName(),
                            workflowRun.getResult(),
                            workflowRun.getStartTimeInMillis(),
                            0,
                            0));
                }
            }
        } else if (job instanceof WorkflowJob) {
            for (WorkflowRun workflowRun : ((WorkflowJob) job).getBuilds()) {
                entries.add(new RunEntry(
                        workflowRun.getNumber(),
                        "",
                        workflowRun.getResult(),
                        workflowRun.getStartTimeInMillis(),
                        0,
                        0));
            }
        } else if (job instanceof AbstractProject<?, ?>) {
            for (AbstractBuild<?, ?> abstractBuild : ((AbstractProject<?, ?>) job).getBuilds()) {
                entries.add(new RunEntry(
                        abstractBuild.getNumber(),
                        "",
                        abstractBuild.getResult(),
                        abstractBuild.getStartTimeInMillis(),
                        0,
                        0));
            }
        }
        return entries;
    }

    public void addRunEntry(RunEntry entry) {
        this.runEntries.add(entry);
    }

    public static class RunEntry {

        private final int runNumber;
        private final String branch;
        private final Result result;
        private final long startTime;
        private final int generatedChallenges;
        private final int solvedChallenges;

        public RunEntry(int runNumber, String branch, Result result, long startTime,
                        int generatedChallenges, int solvedChallenges) {
            this.runNumber = runNumber;
            this.branch = branch;
            this.result = result;
            this.startTime = startTime;
            this.generatedChallenges = generatedChallenges;
            this.solvedChallenges = solvedChallenges;
        }

        public int getRunNumber() {
            return runNumber;
        }

        public String getBranch() {
            return branch;
        }

        public Result getResult() {
            return result;
        }

        public long getStartTime() {
            return startTime;
        }

        public int getGeneratedChallenges() {
            return generatedChallenges;
        }

        public int getSolvedChallenges() {
            return solvedChallenges;
        }

        public String printToXML(String indentation) {
            return indentation + "<Run number=\"" + this.runNumber + "\" branch=\"" + this.branch +
                    "\" result=\"" + this.result.toString() + "\" startTime=\"" +
                    this.startTime + "\" generatedChallenges=\"" + this.generatedChallenges +
                    "\" solvedChallenges=\"" + this.solvedChallenges + "\"/>";
        }
    }
}
