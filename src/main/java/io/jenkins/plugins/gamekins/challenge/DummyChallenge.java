package io.jenkins.plugins.gamekins.challenge;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.util.HashMap;

//TODO: Add text why the DummyChallenge has been created
public class DummyChallenge implements Challenge {
    @Override
    public boolean isSolved(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener,
                            FilePath workspace) {
        return true;
    }

    @Override
    public boolean isSolvable(HashMap<String, String> constants, Run<?, ?> run, TaskListener listener,
                              FilePath workspace) {
        return true;
    }

    @Override
    public int getScore() {
        return 0;
    }

    @Override
    public long getCreated() {
        return 0;
    }

    @Override
    public long getSolved() {
        return 0;
    }

    @Override
    public String printToXML(String reason, String indentation) {
        return indentation + "<DummyChallenge>";
    }

    @Override
    public String toString() {
        return "You have nothing developed recently";
    }
}
