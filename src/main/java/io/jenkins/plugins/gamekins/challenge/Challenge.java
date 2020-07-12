package io.jenkins.plugins.gamekins.challenge;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.HashMap;

public interface Challenge {

    boolean isSolved(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener, FilePath workspace);

    boolean isSolvable(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener, FilePath workspace);

    int getScore();

    long getCreated();

    long getSolved();

    String printToXML(String reason, String indentation);

    @Override
    String toString();
}
