package io.jenkins.plugins.gamekins.challenge;

import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.HashMap;

public interface Challenge {

    boolean isSolved(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener);

    boolean isSolvable(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener);

    int getScore();

    long getCreated();

    long getSolved();

    String printToXML(String reason, String indentation);

    @Override
    String toString();
}
